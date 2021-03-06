package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.common.action.MoveOrCopyParameters;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.vdscommands.GetImagesListVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;

public class MoveOrCopyTemplateCommandTest {

    private Guid[] diskIds;

    @Before
    public void setup() {
        diskIds = new Guid[] {Guid.newGuid(), Guid.newGuid(), Guid.newGuid()};
    }

    @Test
    public void checkIfDisksExistSucceededAllDisksOnSameSD() {
        // Prepare test case
        MoveOrCopyParameters params = createParameters(true);
        VDSReturnValue returnValue = createGetImageListReturnValue(true, Collections.<Guid>emptyList());
        MoveOrCopyTemplateCommand cmd = createCommand(params, returnValue);
        // since target storage domain doesn't contain any of an input disks
        // validation should pass successfully
        assertTrue(cmd.checkIfDisksExist(createImageList()));
        // verify that call to VDSM is executed only once because all disks are copied to the same storage domain.
        verify(cmd, times(1)).runVdsCommand(any(VDSCommandType.class), any(GetImagesListVDSCommandParameters.class));
    }

    @Test
    public void checkIfDisksExistFailedAllDisksAlreadyOnTargetSD() {
        // Prepare test case
        MoveOrCopyParameters params = createParameters(true);
        VDSReturnValue returnValue = createGetImageListReturnValue(true, Arrays.asList(diskIds));
        MoveOrCopyTemplateCommand cmd = createCommand(params, returnValue);
        // since target storage domain contains the first disk validation is failed.
        assertFalse(cmd.checkIfDisksExist(createImageList()));
        // verify that call to VDSM is executed only once because the first disk is found on the target storage domain.
        verify(cmd, times(1)).runVdsCommand(any(VDSCommandType.class), any(GetImagesListVDSCommandParameters.class));
        verify(cmd, times(1)).addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_ALREADY_CONTAINS_DISK);
    }

    @Test
    public void checkIfDisksExistFailedVDSMReturnError() {
        // Prepare test case
        MoveOrCopyParameters params = createParameters(true);
        VDSReturnValue returnValue = createGetImageListReturnValue(false, Collections.<Guid>emptyList());
        MoveOrCopyTemplateCommand cmd = createCommand(params, returnValue);

        assertFalse(cmd.checkIfDisksExist(createImageList()));
        verify(cmd, times(1)).addCanDoActionMessage(VdcBllMessages.ERROR_GET_IMAGE_LIST);
        verify(cmd, times(1)).addCanDoActionMessage("$sdName SD");
        verify(cmd, times(1)).runVdsCommand(any(VDSCommandType.class), any(GetImagesListVDSCommandParameters.class));
    }

    @Test
    public void checkIfDisksExistSucceededCallVDSMTwice() {
        MoveOrCopyParameters params = createParameters(false);
        // First and Third disks should be copied to the same storage domain.
        Guid targetSDForFirstDisk = params.getImageToDestinationDomainMap().get(diskIds[0]);
        params.getImageToDestinationDomainMap().put(diskIds[2], targetSDForFirstDisk);

        VDSReturnValue returnValue = createGetImageListReturnValue(true, null);
        MoveOrCopyTemplateCommand cmd = createCommand(params, returnValue);

        assertTrue(cmd.checkIfDisksExist(createImageList()));
        verify(cmd, times(2)).runVdsCommand(any(VDSCommandType.class), any(GetImagesListVDSCommandParameters.class));
    }

    protected MoveOrCopyParameters createParameters(boolean moveToSameStorageDomain) {
        Guid targetStorageDomainId = Guid.newGuid();
        Map<Guid, Guid> imageToDestinationDomainMap = new HashMap<>();
        for (Guid id : diskIds) {
            if (moveToSameStorageDomain) {
                imageToDestinationDomainMap.put(id, targetStorageDomainId);
            } else {
                imageToDestinationDomainMap.put(id, Guid.newGuid());
            }
        }

        MoveOrCopyParameters params = new MoveOrCopyParameters(Guid.newGuid(), Guid.newGuid());
        params.setImageToDestinationDomainMap(imageToDestinationDomainMap);
        return params;
    }

    private VDSReturnValue createGetImageListReturnValue(boolean isSuccess, final List<Guid> disks) {
        VDSReturnValue returnValue = spy(new VDSReturnValue());
        doReturn(isSuccess).when(returnValue).getSucceeded();
        // If caller doesn't provide list of images to return, return images will be generated per each request.
        if (disks != null) {
            doReturn(disks).when(returnValue).getReturnValue();
        } else {
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return Arrays.asList(new Guid[] {Guid.newGuid(), Guid.newGuid(), Guid.newGuid()});
                }
            }).when(returnValue).getReturnValue();
        }

        return  returnValue;
    }

    private MoveOrCopyTemplateCommand createCommand(MoveOrCopyParameters params, VDSReturnValue returnValue) {
        MoveOrCopyTemplateCommand cmd = spy(new MoveOrCopyTemplateCommand<>(params));
        doReturn(returnValue).when(cmd).runVdsCommand(any(VDSCommandType.class), any(GetImagesListVDSCommandParameters.class));
        doReturn(Guid.newGuid()).when(cmd).getStoragePoolId();

        StorageDomain storageDomain = spy(new StorageDomain());
        doReturn("SD").when(storageDomain).getName();

        doReturn(storageDomain).when(cmd).getStorageDomain(any(Guid.class));
        return  cmd;
    }

    private List<DiskImage> createImageList() {
        List<DiskImage> disks = new ArrayList<>();
        for (Guid id : diskIds) {
            DiskImage diskImage = new DiskImage();
            diskImage.setId(id);
            disks.add(diskImage);
        }
        return disks;
    }
}
