package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.DbUser;
import org.ovirt.engine.core.common.utils.ExternalId;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.CustomMapSqlParameterSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

/**
 * <code>DBUserDAODbFacadeImpl</code> provides an implementation of {@link DbUserDAO} with the previously developed
 * {@link org.ovirt.engine.core.dal.dbbroker.DbFacade.DbFacade} code.
 *
 */
public class DbUserDAODbFacadeImpl extends BaseDAODbFacade implements DbUserDAO {
    private static class DbUserRowMapper implements RowMapper<DbUser> {
        public static final DbUserRowMapper instance = new DbUserRowMapper();

        @Override
        public DbUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            DbUser entity = new DbUser();
            entity.setDepartment(rs.getString("department"));
            entity.setDomain(rs.getString("domain"));
            entity.setEmail(rs.getString("email"));
            entity.setGroupNames(rs.getString("groups"));
            entity.setFirstName(rs.getString("name"));
            entity.setNote(rs.getString("note"));
            entity.setNote(rs.getString("note"));
            entity.setRole(rs.getString("role"));
            entity.setActive(rs.getBoolean("active"));
            entity.setLastName(rs.getString("surname"));
            entity.setId(getGuidDefaultEmpty(rs, "user_id"));
            entity.setLoginName(rs.getString("username"));
            entity.setAdmin(rs.getBoolean("last_admin_check_status"));
            entity.setGroupIds(rs.getString("group_ids"));
            entity.setExternalId(new ExternalId(rs.getBytes("external_id")));

            if (entity.isGroup())
                entity.setDomain("");

            return entity;
        }
    }

    private class DbUserMapSqlParameterSource extends
            CustomMapSqlParameterSource {
        public DbUserMapSqlParameterSource(DbUser user) {
            super(dialect);
            addValue("department", user.getDepartment());
            addValue("domain", user.getDomain());
            addValue("email", user.getEmail());
            addValue("groups", user.getGroupNames());
            addValue("name", user.getFirstName());
            addValue("note", user.getNote());
            addValue("role", user.getRole());
            addValue("active", user.isActive());
            addValue("surname", user.getLastName());
            addValue("user_id", user.getId());
            addValue("username", user.getLoginName());
            addValue("last_admin_check_status", user.isAdmin());
            addValue("group_ids", user.getGroupIds());
            addValue("external_id", user.getExternalId());
        }
    }

    @Override
    public DbUser get(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("user_id", id);

        return getCallsHandler().executeRead("GetUserByUserId", DbUserRowMapper.instance, parameterSource);
    }

    @Override
    public DbUser getByUsername(String username) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("username", username);

        return getCallsHandler().executeRead("GetUserByUserName", DbUserRowMapper.instance, parameterSource);
    }

    @Override
    public DbUser getByExternalId(String domain, ExternalId externalId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("domain", domain)
                .addValue("external_id", externalId.getBytes());

        return getCallsHandler().executeRead("GetUserByExternalId", DbUserRowMapper.instance, parameterSource);
    }

    @Override
    public DbUser getByIdOrExternalId(Guid id, String domain, ExternalId externalId) {
        // Check if there is a user with the given internal identifier:
        if (id != null) {
            DbUser existing = get(id);
            if (existing != null) {
                return existing;
            }
        }

        // Check if there is an existing user for the given external identifier:
        if (domain != null && externalId != null) {
            DbUser existing = getByExternalId(domain, externalId);
            if (existing != null) {
                return existing;
            }
        }

        // In older versions of the engine the internal and external identifiers were the same, so we also need to check
        // if the internal id is really an external id:
        if (domain != null && id != null) {
            externalId = ExternalId.fromHex(id.toString());
            DbUser existing = getByExternalId(domain, externalId);
            if (existing != null) {
                return existing;
            }
        }

        // There is no such existing user:
        return null;
    }

    @Override
    public List<DbUser> getAllForVm(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_guid", id);

        return getCallsHandler().executeReadList("GetUsersByVmGuid", DbUserRowMapper.instance, parameterSource);
    }

    @Override
    public List<DbUser> getAllWithQuery(String query) {
        return jdbcTemplate.query(query, DbUserRowMapper.instance);
    }

    @Override
    public List<DbUser> getAll() {
        return getAll(null, false);
    }

    @Override
    public List<DbUser> getAll(Guid userID, boolean isFiltered) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("user_id", userID).addValue("is_filtered", isFiltered);
        return getCallsHandler().executeReadList("GetAllFromUsers", DbUserRowMapper.instance, parameterSource);
    }

    @Override
    public void save(DbUser user) {
        new SimpleJdbcCall(jdbcTemplate).withProcedureName("InsertUser").execute(new DbUserMapSqlParameterSource(user));
    }

    @Override
    public void update(DbUser user) {
        getCallsHandler().executeModification("UpdateUser", new DbUserMapSqlParameterSource(user));
    }

    @Override
    public void remove(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("user_id", id);

        getCallsHandler().executeModification("DeleteUser", parameterSource);
    }
}
