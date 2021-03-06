package org.ovirt.engine.core.bll;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.DiskImageDAO;
import org.ovirt.engine.core.dao.SnapshotDao;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.VdsGroupDAO;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.VmTemplateDAO;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("serial")
public class AddVmCommandTest {

    private static final int REQUIRED_DISK_SIZE_GB = 10;
    private static final int AVAILABLE_SPACE_GB = 11;
    private static final int USED_SPACE_GB = 4;
    private static int MAX_PCI_SLOTS = 26;
    private static final Guid STORAGE_POOL_ID = Guid.newGuid();
    private static final Guid STORAGE_DOMAIN_ID = Guid.newGuid();
    private VmTemplate vmTemplate = null;
    private VDSGroup vdsGroup = null;

    @Rule
    public MockConfigRule mcr = new MockConfigRule();

    @Mock
    StorageDomainDAO sdDAO;

    @Mock
    VmTemplateDAO vmTemplateDAO;

    @Mock
    VmDAO vmDAO;

    @Mock
    DiskImageDAO diskImageDAO;

    @Mock
    VdsGroupDAO vdsGroupDao;

    @Mock
    BackendInternal backend;

    @Mock
    VDSBrokerFrontend vdsBrokerFrontend;

    @Mock
    SnapshotDao snapshotDao;

    @Mock
    OsRepository osRepository;

    @Test
    public void create10GBVmWith11GbAvailableAndA5GbBuffer() throws Exception {
        VM vm = createVm();
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd = createVmFromTemplateCommand(vm);

        mockOsRepository();
        mockStorageDomainDAOGetForStoragePool();
        mockVdsGroupDAOReturnVdsGroup();
        mockVmTemplateDAOReturnVmTemplate();
        mockDiskImageDAOGetSnapshotById();
        mockVerifyAddVM(cmd);
        mockConfig();
        mockConfigSizeDefaults();
        mockMaxPciSlots();

        mockStorageDomainDaoGetAllStoragesForPool(AVAILABLE_SPACE_GB);
        mockUninterestingMethods(cmd);
        assertFalse("If the disk is too big, canDoAction should fail", cmd.canDoAction());
        assertTrue("canDoAction failed for the wrong reason",
                cmd.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_TARGET_STORAGE_DOMAIN.toString()));
    }

    private void mockOsRepository() {
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
    }

    @Test
    public void canAddVm() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 20;
        final int sizeRequired = 5;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        assertTrue("vm could not be added", cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
    }

    @Test
    public void canAddVmFailSpaceThreshold() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int sizeRequired = 10;
        final int domainSizeGB = 4;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        assertFalse("vm could not be added", cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for the wrong reason",
                cmd.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_TARGET_STORAGE_DOMAIN.toString()));
    }

    @Test
    public void canAddThinVmFromTemplateWithManyDisks() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 20;
        final int sizeRequired = 10;
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired);
        doReturn(Collections.emptyList()).when(cmd).validateCustomProperties(any(VmStatic.class));
        // Adding 10 disks, which each one should consume the default sparse size (which is 1GB).
        setNewDisksForTemplate(10, cmd.getVmTemplate().getDiskTemplateMap());
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        assertFalse("Thin vm could not be added due to storage sufficient",
                cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for insufficient disk size", cmd.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_TARGET_STORAGE_DOMAIN.toString()));
    }

    @Test
    public void canAddCloneVmFromTemplate() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd =
                setupCanAddVmFromTemplateTests(domainSizeGB, sizeRequired);

        // Set new Disk Image map with 3GB.
        setNewImageDiskMapForTemplate(cmd, 3000000000L, cmd.getVmTemplate().getDiskImageMap());
        assertFalse("Clone vm could not be added due to storage sufficient",
                cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
        assertTrue("canDoAction failed for insufficient disk size",
                cmd.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_TARGET_STORAGE_DOMAIN.toString()));
    }

    @Test
    public void canAddCloneVmFromSnapshotSnapshotDoesNotExist() {
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final Guid sourceSnapshotId = Guid.newGuid();
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                setupCanAddVmFromSnapshotTests(domainSizeGB, sizeRequired, sourceSnapshotId);
        cmd.getVm().setName("vm1");
        mockNonInterestingMethodsForCloneVmFromSnapshot(cmd);
        assertFalse("Clone vm should have failed due to non existing snapshot id", cmd.canDoAction());
        ArrayList<String> reasons = cmd.getReturnValue().getCanDoActionMessages();
        assertTrue("Clone vm should have failed due to non existing snapshot id",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_VM_SNAPSHOT_DOES_NOT_EXIST.toString()));
    }

    @Test
    public void canAddCloneVmFromSnapshotNoConfiguration() {
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final Guid sourceSnapshotId = Guid.newGuid();
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                setupCanAddVmFromSnapshotTests(domainSizeGB, sizeRequired, sourceSnapshotId);
        cmd.getVm().setName("vm1");
        mockNonInterestingMethodsForCloneVmFromSnapshot(cmd);
        SnapshotsValidator sv = spy(new SnapshotsValidator());
        doReturn(ValidationResult.VALID).when(sv).vmNotDuringSnapshot(any(Guid.class));
        doReturn(sv).when(cmd).createSnapshotsValidator();
        when(snapshotDao.get(sourceSnapshotId)).thenReturn(new Snapshot());
        assertFalse("Clone vm should have failed due to non existing vm configuration", cmd.canDoAction());
        ArrayList<String>  reasons = cmd.getReturnValue().getCanDoActionMessages();
        assertTrue("Clone vm should have failed due to no configuration id",
                reasons.contains(VdcBllMessages.ACTION_TYPE_FAILED_VM_SNAPSHOT_HAS_NO_CONFIGURATION.toString()));

    }

    @Test
    public void canAddVmWithVirtioScsiControllerNotSupportedOs() {
        VM vm = createVm();
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd = createVmFromTemplateCommand(vm);

        VDSGroup vdsGroup = createVdsGroup();

        mockOsRepository();
        mockStorageDomainDAOGetForStoragePool();
        mockVmTemplateDAOReturnVmTemplate();
        mockDiskImageDAOGetSnapshotById();
        mockVerifyAddVM(cmd);
        mockConfig();
        mockConfigSizeDefaults();
        mockMaxPciSlots();
        mockStorageDomainDaoGetAllStoragesForPool(20);
        mockUninterestingMethods(cmd);
        mockDisplayTypes(vm.getOs(), vdsGroup.getcompatibility_version());
        doReturn(true).when(cmd).checkCpuSockets();

        doReturn(vdsGroup).when(cmd).getVdsGroup();
        cmd.getParameters().setVirtioScsiEnabled(true);
        when(osRepository.getArchitectureFromOS(any(Integer.class))).thenReturn(ArchitectureType.x86_64);
        when(osRepository.getDiskInterfaces(any(Integer.class), any(Version.class))).thenReturn(
                new ArrayList<>(Arrays.asList("VirtIO")));

        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                VdcBllMessages.ACTION_TYPE_FAILED_ILLEGAL_OS_TYPE_DOES_NOT_SUPPORT_VIRTIO_SCSI);
    }

    @Test
    public void isVirtioScsiEnabledDefaultedToTrue() {
        mockOsRepository();
        AddVmCommand<VmManagementParametersBase> cmd = setupCanAddVmTests(0, 0);
        doReturn(createVdsGroup()).when(cmd).getVdsGroup();
        when(osRepository.getDiskInterfaces(any(Integer.class), any(Version.class))).thenReturn(
                new ArrayList<>(Arrays.asList("VirtIO_SCSI")));
        assertTrue("isVirtioScsiEnabled hasn't been defaulted to true on cluster >= 3.3.", cmd.isVirtioScsiEnabled());
    }

    private void mockDisplayTypes(int osId, Version clusterVersion) {
        Map<Integer, Map<Version, List<DisplayType>>> displayTypeMap = new HashMap<>();
        displayTypeMap.put(osId, new HashMap<Version, List<DisplayType>>());
        displayTypeMap.get(osId).put(clusterVersion, Arrays.asList(DisplayType.qxl));
        when(osRepository.getDisplayTypes()).thenReturn(displayTypeMap);
    }

    protected void mockNonInterestingMethodsForCloneVmFromSnapshot(AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd) {
        mockUninterestingMethods(cmd);
        doReturn(true).when(cmd).checkCpuSockets();
        doReturn(null).when(cmd).getVmFromConfiguration();
    }

    private void mockMaxPciSlots() {
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
        doReturn(MAX_PCI_SLOTS).when(osRepository).getMaxPciDevices(anyInt(), any(Version.class));
    }

    private AddVmFromTemplateCommand<AddVmFromTemplateParameters> createVmFromTemplateCommand(VM vm) {
        AddVmFromTemplateParameters param = new AddVmFromTemplateParameters();
        param.setVm(vm);
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> concrete =
                new AddVmFromTemplateCommand<AddVmFromTemplateParameters>(param) {
                    @Override
                    protected void initTemplateDisks() {
                        // Stub for testing
                    }

                    @Override
                    protected void initStoragePoolId() {
                        // Stub for testing
                    }

                    @Override
                    public VmTemplate getVmTemplate() {
                        return createVmTemplate();
                    }
                };
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> result = spy(concrete);
        doReturn(true).when(result).checkNumberOfMonitors();
        doReturn(createVmTemplate()).when(result).getVmTemplate();
        doReturn(Collections.emptyList()).when(result).validateCustomProperties(any(VmStatic.class));
        mockDAOs(result);
        mockBackend(result);
        return result;
    }

    private AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> createVmFromSnapshotCommand(VM vm,
            Guid sourceSnapshotId) {
        AddVmFromSnapshotParameters param = new AddVmFromSnapshotParameters();
        param.setVm(vm);
        param.setSourceSnapshotId(sourceSnapshotId);
        param.setStorageDomainId(Guid.newGuid());
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                new AddVmFromSnapshotCommand<AddVmFromSnapshotParameters>(param) {
                    @Override
                    protected void initTemplateDisks() {
                        // Stub for testing
                    }

                    @Override
                    protected void initStoragePoolId() {
                        // Stub for testing
                    }

                    @Override
                    public VmTemplate getVmTemplate() {
                        return createVmTemplate();
                    }
                };
        cmd = spy(cmd);
        doReturn(vm).when(cmd).getVm();
        mockDAOs(cmd);
        doReturn(snapshotDao).when(cmd).getSnapshotDao();
        mockBackend(cmd);
        return cmd;
    }

    private AddVmFromTemplateCommand<AddVmFromTemplateParameters> setupCanAddVmFromTemplateTests(final int domainSizeGB,
            final int sizeRequired) {
        VM vm = initializeMock(domainSizeGB, sizeRequired);
        AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd = createVmFromTemplateCommand(vm);
        initCommandMethods(cmd);
        return cmd;
    }

    private AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> setupCanAddVmFromSnapshotTests(final int domainSizeGB,
            final int sizeRequired,
            Guid sourceSnapshotId) {
        VM vm = initializeMock(domainSizeGB, sizeRequired);
        initializeVmDAOMock(vm);
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd = createVmFromSnapshotCommand(vm, sourceSnapshotId);
        initCommandMethods(cmd);
        return cmd;
    }

    private void initializeVmDAOMock(VM vm) {
        when(vmDAO.get(Matchers.<Guid> any(Guid.class))).thenReturn(vm);
    }

    private AddVmCommand<VmManagementParametersBase> setupCanAddVmTests(final int domainSizeGB,
            final int sizeRequired) {
        VM vm = initializeMock(domainSizeGB, sizeRequired);
        AddVmCommand<VmManagementParametersBase> cmd = createCommand(vm);
        initCommandMethods(cmd);
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        return cmd;
    }

    private static <T extends VmManagementParametersBase> void initCommandMethods(AddVmCommand<T> cmd) {
        doReturn(Guid.newGuid()).when(cmd).getStoragePoolId();
        doReturn(true).when(cmd).canAddVm(anyListOf(String.class), anyString(), any(Guid.class), anyInt());
        doReturn(STORAGE_POOL_ID).when(cmd).getStoragePoolId();
    }

    private VM initializeMock(final int domainSizeGB, final int sizeRequired) {
        mockVmTemplateDAOReturnVmTemplate();
        mockDiskImageDAOGetSnapshotById();
        mockStorageDomainDAOGetForStoragePool(domainSizeGB);
        mockStorageDomainDAOGet(domainSizeGB);
        mockConfig();
        mockConfigSizeRequirements(sizeRequired);
        VM vm = createVm();
        return vm;
    }

    private static void setNewDisksForTemplate(int numberOfNewDisks, Map<Guid, DiskImage> disksMap) {
        for (int newDiskInd = 0; newDiskInd < numberOfNewDisks; newDiskInd++) {
            DiskImage diskImageTempalte = new DiskImage();
            diskImageTempalte.setImageId(Guid.newGuid());
            disksMap.put(Guid.newGuid(), diskImageTempalte);
        }
    }

    private static void setNewImageDiskMapForTemplate(AddVmFromTemplateCommand<AddVmFromTemplateParameters> cmd,
            long diskSize,
            Map<Guid, DiskImage> diskImageMap) {
        DiskImage diskImage = new DiskImage();
        diskImage.setActualSizeInBytes(diskSize);
        diskImage.setStorageIds(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID)));
        diskImageMap.put(Guid.newGuid(), diskImage);
        cmd.storageToDisksMap = new HashMap<Guid, List<DiskImage>>();
        cmd.storageToDisksMap.put(STORAGE_DOMAIN_ID,
                new ArrayList<DiskImage>(diskImageMap.values()));
    }

    private void mockBackend(AddVmCommand<?> cmd) {
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);
        doReturn(backend).when(cmd).getBackend();
    }

    private void mockDAOs(AddVmCommand<?> cmd) {
        doReturn(vmDAO).when(cmd).getVmDAO();
        doReturn(sdDAO).when(cmd).getStorageDomainDAO();
        doReturn(vmTemplateDAO).when(cmd).getVmTemplateDAO();
        doReturn(vdsGroupDao).when(cmd).getVdsGroupDAO();
    }

    private void mockStorageDomainDAOGetForStoragePool(int domainSpaceGB) {
        when(sdDAO.getForStoragePool(Matchers.<Guid> any(Guid.class), Matchers.<Guid> any(Guid.class))).thenReturn(createStorageDomain(domainSpaceGB));
    }

    private void mockStorageDomainDAOGet(final int domainSpaceGB) {
        doAnswer(new Answer<StorageDomain>() {

            @Override
            public StorageDomain answer(InvocationOnMock invocation) throws Throwable {
                StorageDomain result = createStorageDomain(domainSpaceGB);
                result.setId((Guid) invocation.getArguments()[0]);
                return result;
            }

        }).when(sdDAO).get(any(Guid.class));
    }

    private void mockStorageDomainDaoGetAllStoragesForPool(int domainSpaceGB) {
        when(sdDAO.getAllForStoragePool(any(Guid.class))).thenReturn(Arrays.asList(createStorageDomain(domainSpaceGB)));
    }

    private void mockStorageDomainDAOGetForStoragePool() {
        mockStorageDomainDAOGetForStoragePool(AVAILABLE_SPACE_GB);
    }

    private void mockVmTemplateDAOReturnVmTemplate() {
        when(vmTemplateDAO.get(Matchers.<Guid> any(Guid.class))).thenReturn(createVmTemplate());
    }

    private void mockVdsGroupDAOReturnVdsGroup() {
        when(vdsGroupDao.get(Matchers.<Guid> any(Guid.class))).thenReturn(createVdsGroup());
    }

    private VmTemplate createVmTemplate() {
        if (vmTemplate == null) {
            vmTemplate = new VmTemplate();
            vmTemplate.setStoragePoolId(STORAGE_POOL_ID);
            DiskImage image = createDiskImageTemplate();
            vmTemplate.getDiskTemplateMap().put(image.getImageId(), image);
            HashMap<Guid, DiskImage> diskImageMap = new HashMap<Guid, DiskImage>();
            DiskImage diskImage = createDiskImage(REQUIRED_DISK_SIZE_GB);
            diskImageMap.put(diskImage.getId(), diskImage);
            vmTemplate.setDiskImageMap(diskImageMap);
        }
        return vmTemplate;
    }

    private VDSGroup createVdsGroup() {
        if (vdsGroup == null) {
            vdsGroup = new VDSGroup();
            vdsGroup.setvds_group_id(Guid.newGuid());
            vdsGroup.setcompatibility_version(Version.v3_3);
            vdsGroup.setcpu_name("Intel Conroe Family");
            vdsGroup.setArchitecture(ArchitectureType.x86_64);
        }

        return vdsGroup;
    }

    private static DiskImage createDiskImageTemplate() {
        DiskImage i = new DiskImage();
        i.setSizeInGigabytes(USED_SPACE_GB + AVAILABLE_SPACE_GB);
        i.setActualSizeInBytes(REQUIRED_DISK_SIZE_GB * 1024L * 1024L * 1024L);
        i.setImageId(Guid.newGuid());
        i.setStorageIds(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID)));
        return i;
    }

    private void mockDiskImageDAOGetSnapshotById() {
        when(diskImageDAO.getSnapshotById(Matchers.<Guid> any(Guid.class))).thenReturn(createDiskImage(REQUIRED_DISK_SIZE_GB));
    }

    private static DiskImage createDiskImage(int size) {
        DiskImage img = new DiskImage();
        img.setSizeInGigabytes(size);
        img.setActualSize(size);
        img.setId(Guid.newGuid());
        img.setStorageIds(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID)));
        return img;
    }

    protected StorageDomain createStorageDomain(int availableSpace) {
        StorageDomain sd = new StorageDomain();
        sd.setStorageDomainType(StorageDomainType.Master);
        sd.setStatus(StorageDomainStatus.Active);
        sd.setAvailableDiskSize(availableSpace);
        sd.setUsedDiskSize(USED_SPACE_GB);
        sd.setId(STORAGE_DOMAIN_ID);
        return sd;
    }

    private static void mockVerifyAddVM(AddVmCommand<?> cmd) {
        doReturn(true).when(cmd).verifyAddVM(anyListOf(String.class), anyInt());
    }

    private void mockConfig() {
        mcr.mockConfigValue(ConfigValues.PredefinedVMProperties, Version.v3_0, "");
        mcr.mockConfigValue(ConfigValues.UserDefinedVMProperties, Version.v3_0, "");
        mcr.mockConfigValue(ConfigValues.InitStorageSparseSizeInGB, 1);
        mcr.mockConfigValue(ConfigValues.VirtIoScsiEnabled, Version.v3_3, true);
    }

    private void mockConfigSizeRequirements(int requiredSpaceBufferInGB) {
        mcr.mockConfigValue(ConfigValues.FreeSpaceCriticalLowInGB, requiredSpaceBufferInGB);
    }

    private void mockConfigSizeDefaults() {
        int requiredSpaceBufferInGB = 5;
        mockConfigSizeRequirements(requiredSpaceBufferInGB);
    }

    private static VM createVm() {
        VM vm = new VM();
        VmDynamic dynamic = new VmDynamic();
        VmStatic stat = new VmStatic();
        stat.setVmtGuid(Guid.newGuid());
        stat.setName("testVm");
        stat.setPriority(1);
        vm.setStaticData(stat);
        vm.setDynamicData(dynamic);
        vm.setSingleQxlPci(false);
        return vm;
    }

    private AddVmCommand<VmManagementParametersBase> createCommand(VM vm) {
        VmManagementParametersBase param = new VmManagementParametersBase(vm);
        AddVmCommand<VmManagementParametersBase> cmd = new AddVmCommand<VmManagementParametersBase>(param) {
            @Override
            protected void initTemplateDisks() {
                // Stub for testing
            }

            @Override
            protected void initStoragePoolId() {
                // stub for testing
            }

            @Override
            protected int getNeededDiskSize(Guid domainId) {
                return getBlockSparseInitSizeInGb() * getVmTemplate().getDiskTemplateMap().size();
            }

            @Override
            public VmTemplate getVmTemplate() {
                return createVmTemplate();
            }
        };
        cmd = spy(cmd);
        mockDAOs(cmd);
        mockBackend(cmd);
        return cmd;
    }

    private <T extends VmManagementParametersBase> void mockUninterestingMethods(AddVmCommand<T> spy) {
        doReturn(true).when(spy).isVmNameValidLength(Matchers.<VM> any(VM.class));
        doReturn(false).when(spy).isVmWithSameNameExists(anyString());
        doReturn(STORAGE_POOL_ID).when(spy).getStoragePoolId();
        doReturn(createVmTemplate()).when(spy).getVmTemplate();
        doReturn(createVdsGroup()).when(spy).getVdsGroup();
        doReturn(true).when(spy).areParametersLegal(anyListOf(String.class));
        doReturn(Collections.<VmNetworkInterface> emptyList()).when(spy).getVmInterfaces();
        doReturn(Collections.<DiskImageBase> emptyList()).when(spy).getVmDisks();
        doReturn(false).when(spy).isVirtioScsiControllerAttached(any(Guid.class));
        spy.setVmTemplateId(Guid.newGuid());
    }
}
