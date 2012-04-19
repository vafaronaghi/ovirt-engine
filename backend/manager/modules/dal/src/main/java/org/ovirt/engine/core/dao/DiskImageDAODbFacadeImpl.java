package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.PropagateErrors;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.ovirt.engine.core.utils.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * <code>DiskImageDAODbFacadeImpl</code> provides an implementation of {@link DiskImageDAO} that uses previously
 * developed code from {@link DbFacade}.
 */
public class DiskImageDAODbFacadeImpl extends BaseDAODbFacade implements DiskImageDAO {

    @Override
    public DiskImage get(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_guid", id);

        return getCallsHandler().executeRead("GetImageByImageGuid", DiskImageRowMapper.instance, parameterSource);
    }

    @Override
    public DiskImage getSnapshotById(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_guid", id);

        return getCallsHandler().executeRead("GetSnapshotByGuid", DiskImageRowMapper.instance, parameterSource);
    }

    @Override
    public List<DiskImage> getAllForVm(Guid id) {
        return getAllForVm(id, null, false);
    }

    @Override
    public List<DiskImage> getAllForQuotaId(Guid quotaId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("quota_id", quotaId);

        return getCallsHandler().executeReadList("GetImagesByQuotaId", DiskImageRowMapper.instance, parameterSource);
    }

    @Override
    public List<DiskImage> getAllForVm(Guid id, Guid userID, boolean isFiltered) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_guid", id).addValue("user_id", userID).addValue("is_filtered", isFiltered);
        return getCallsHandler().executeReadList("GetImagesByVmGuid", DiskImageRowMapper.instance, parameterSource);
    }

    @Override
    public List<DiskImage> getAllSnapshotsForParent(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("parent_guid", id);
        return getCallsHandler().executeReadList("GetSnapshotByParentGuid",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<DiskImage> getAllSnapshotsForStorageDomain(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("storage_domain_id", id);

        return getCallsHandler().executeReadList("GetSnapshotsByStorageDomainId",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<DiskImage> getAllSnapshotsForVmSnapshot(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_snapshot_id", id);

        return getCallsHandler().executeReadList("GetSnapshotsByVmSnapshotId",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<DiskImage> getAllSnapshotsForImageGroup(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_group_id", id);

        return getCallsHandler().executeReadList("GetSnapshotsByImageGroupId",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<DiskImage> getAll() {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource();
        return getCallsHandler().executeReadList("GetAllFromImages", DiskImageRowMapper.instance, parameterSource);
    }

    @Override
    public void save(DiskImage image) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("creation_date", image.getcreation_date())
                .addValue("image_guid", image.getImageId())
                .addValue("it_guid", image.getit_guid())
                .addValue("size", image.getsize())
                .addValue("ParentId", image.getParentId())
                .addValue("imageStatus", image.getimageStatus())
                .addValue("lastModified", image.getlastModified())
                .addValue("vm_snapshot_id", image.getvm_snapshot_id())
                .addValue("volume_type", image.getvolume_type())
                .addValue("volume_format", image.getvolume_format())
                .addValue("image_group_id", image.getimage_group_id())
                .addValue("active", image.getactive())
                .addValue("boot", image.getboot())
                .addValue("quota_id", image.getQuotaId());
        getCallsHandler().executeModification("InsertImage", parameterSource);
    }

    @Override
    public void update(DiskImage image) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("creation_date", image.getcreation_date())
                .addValue("image_guid", image.getImageId())
                .addValue("it_guid", image.getit_guid())
                .addValue("size", image.getsize())
                .addValue("ParentId", image.getParentId())
                .addValue("imageStatus", image.getimageStatus())
                .addValue("lastModified", image.getlastModified())
                .addValue("vm_snapshot_id", image.getvm_snapshot_id())
                .addValue("volume_type", image.getvolume_type())
                .addValue("volume_format", image.getvolume_format())
                .addValue("image_group_id", image.getimage_group_id())
                .addValue("active", image.getactive())
                .addValue("boot", image.getboot())
                .addValue("quota_id", image.getQuotaId());

        getCallsHandler().executeModification("UpdateImage", parameterSource);
    }

    @Override
    public void remove(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_guid", id);

        getCallsHandler().executeModification("DeleteImage", parameterSource);
    }

    @Override
    public void removeAllForVmId(Guid id) {
        List<Guid> imagesList = new ArrayList<Guid>();
        for (DiskImage image : getAllForVm(id)) {
            imagesList.add(image.getImageId());

            List<DiskImage> imagesForDisk =
                    DbFacade.getInstance().getDiskImageDAO().getAllSnapshotsForImageGroup(image.getimage_group_id());
            if (imagesForDisk == null || imagesForDisk.isEmpty()) {
                DbFacade.getInstance().getBaseDiskDao().remove(image.getimage_group_id());
            }
        }
    }

    @Override
    public DiskImage getAncestor(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_guid", id);

        return getCallsHandler().executeRead("GetAncestralImageByImageGuid",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<DiskImage> getImagesByStorageIdAndTemplateId(Guid storageId, Guid templateId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("storage_id", storageId)
                .addValue("template_id", templateId);

        return getCallsHandler().executeReadList("GetImageByStorageIdAndTemplateId",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<DiskImage> getAllAttachableDisksByPoolId(Guid poolId, Guid userId, boolean isFiltered) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("storage_pool_id", poolId)
                .addValue("user_id", userId)
                .addValue("is_filtered", isFiltered);

        return getCallsHandler().executeReadList("GetAllAttachableDisksByPoolId",
                DiskImageRowMapper.instance,
                parameterSource);

    }

    @Override
    public List<DiskImage> getImagesWithNoDisk(Guid vmId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);

        return getCallsHandler().executeReadList("GetImagesWhichHaveNoDisk",
                DiskImageRowMapper.instance,
                parameterSource);
    }

    private static class DiskImageRowMapper implements
            ParameterizedRowMapper<DiskImage> {

        public static DiskImageRowMapper instance = new DiskImageRowMapper();

        private DiskImageRowMapper() {
        }

        @Override
        public DiskImage mapRow(ResultSet rs, int rowNum) throws SQLException {
            DiskImage entity = new DiskImage();
            mapEntity(rs, entity);
            return entity;
        }

        protected void mapEntity(ResultSet rs, DiskImage entity) throws SQLException {
            entity.setvm_guid(Guid.createGuidFromString(rs
                    .getString("vm_guid")));
            entity.setcreation_date(DbFacadeUtils.fromDate(rs
                    .getTimestamp("creation_date")));
            entity.setactual_size(rs.getLong("actual_size"));
            entity.setdescription(rs.getString("description"));
            entity.setImageId(Guid.createGuidFromString(rs
                    .getString("image_guid")));
            entity.setinternal_drive_mapping(rs
                    .getString("internal_drive_mapping"));
            entity.setit_guid(Guid.createGuidFromString(rs
                    .getString("it_guid")));
            entity.setsize(rs.getLong("size"));
            entity.setParentId(Guid.createGuidFromString(rs
                    .getString("ParentId")));
            entity.setimageStatus(ImageStatus.forValue(rs
                    .getInt("imageStatus")));
            entity.setlastModified(DbFacadeUtils.fromDate(rs
                    .getTimestamp("lastModified")));
            entity.setappList(rs.getString("app_list"));
            entity.setstorage_ids(StringUtils.getStorageIdList(rs.getString("storage_id")));
            entity.setStoragesNames(StringUtils.splitStringList(rs.getString("storage_name")));
            entity.setvm_snapshot_id(NGuid.createGuidFromString(rs
                    .getString("vm_snapshot_id")));
            entity.setvolume_type(VolumeType.forValue(rs
                    .getInt("volume_type")));
            entity.setvolume_format(VolumeFormat.forValue(rs
                    .getInt("volume_format")));
            entity.setimage_group_id(Guid.createGuidFromString(rs
                    .getString("image_group_id")));
            entity.setstorage_path(StringUtils.splitStringList(rs.getString("storage_path")));
            entity.setstorage_pool_id(NGuid.createGuidFromString(rs
                    .getString("storage_pool_id")));
            entity.setDiskInterface(DiskInterface.forValue(rs
                    .getInt("disk_interface")));
            entity.setboot(rs.getBoolean("boot"));
            entity.setWipeAfterDelete(rs.getBoolean("wipe_after_delete"));
            entity.setPropagateErrors(PropagateErrors.forValue(rs
                    .getInt("propagate_errors")));
            entity.setread_rate(rs.getInt("read_rate"));
            entity.setwrite_rate(rs.getInt("write_rate"));
            entity.setQuotaId(Guid.createGuidFromString(rs.getString("quota_id")));
            entity.setactive((Boolean) rs.getObject("active"));
            entity.setQuotaName(rs.getString("quota_name"));
            entity.setDiskAlias(rs.getString("disk_alias"));
            entity.setDiskDescription(rs.getString("disk_description"));
            entity.setShareable(rs.getBoolean("shareable"));
            String entityType = rs.getString("entity_type");
            handleEntityType(entityType, entity);
        }
    }

    private static void handleEntityType(String entityType, DiskImage entity) {
        if (entityType != null && !entityType.isEmpty()) {
            VmEntityType vmEntityType = VmEntityType.valueOf(entityType);
            entity.setVmEntityType(vmEntityType);
        }
    }

    @Override
    public List<DiskImage> getAllWithQuery(String query) {
        return new SimpleJdbcTemplate(jdbcTemplate).query(query, DiskImageRowMapper.instance);
    }

    @Override
    public void updateStatus(Guid id, ImageStatus status) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_id", id)
                .addValue("status", status);
        getCallsHandler().executeModification("UpdateImageStatus", parameterSource);
    }
}
