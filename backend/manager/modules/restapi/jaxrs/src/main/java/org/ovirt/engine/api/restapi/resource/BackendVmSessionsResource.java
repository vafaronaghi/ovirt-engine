package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Session;
import org.ovirt.engine.api.model.Sessions;
import org.ovirt.engine.api.model.User;
import org.ovirt.engine.api.resource.UserResource;
import org.ovirt.engine.api.resource.VmSessionResource;
import org.ovirt.engine.api.resource.VmSessionsResource;
import org.ovirt.engine.api.restapi.types.VmMapper;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmSessionsResource extends AbstractBackendCollectionResource<Session, Object> implements VmSessionsResource {

    private Guid vmId;
    private BackendUserResource userResource;

    public BackendVmSessionsResource(Guid vmId) {
        super(Session.class, Object.class);
        this.vmId = vmId;
    }

    @Override
    public Sessions list() {
        Object obj = getEntity(entityType, VdcQueryType.GetVmByVmId, new IdQueryParameters(vmId), vmId.toString(), true);
        VM vm = (VM)obj;
        Sessions sessions = VmMapper.map(vm, new Sessions());
        org.ovirt.engine.api.model.VM vmModel = new org.ovirt.engine.api.model.VM();
        vmModel.setId(vm.getId().toString());
        if (sessions.isSetSessions()) {
            for (Session session : sessions.getSessions()) {
                setSessionId(session);
                setSessionVmId(vmModel, session);
                setSessionUser(session);
                addLinks(session, org.ovirt.engine.api.model.VM.class);
            }
        }
        return sessions;
    }

    private void setSessionVmId(org.ovirt.engine.api.model.VM vmModel, Session session) {
        session.setVm(vmModel);
    }

    /**
     * A session is not a business-entity in the engine and does not have an ID. This method generates an ID for the
     * session object, based on its attributes.
     */
    private void setSessionId(Session session) {
        String idString = session.getUser().getName();
        if (session.isSetIp() && session.getIp().isSetAddress()) {
            idString += session.getIp().getAddress();
        }
        if (session.isSetProtocol()) {
            idString += session.getProtocol();
        }
        session.setId(GuidUtils.generateGuidUsingMd5(idString).toString());
    }

    /**
     * The console user, if exists, is a real ovirt-user. Use its name to get ID and herf information, and set them
     * inside the user object, inside the session.
     */
    private void setSessionUser(Session session) {
        if (session.isSetConsoleUser() && session.isConsoleUser()) { // (only console user assumed to be an ovirt user).
            User user = getUserResource().getUserByName(session.getUser().getName());
            session.getUser().setId(user.getId());
            session.getUser().setHref(user.getHref());
        }
    }

    private BackendUserResource getUserResource() {
        if (this.userResource == null) {
            BackendUsersResource usersResource = new BackendUsersResource();
            inject(usersResource);
            UserResource userResource = usersResource.getUserSubResource("");
            this.userResource = (BackendUserResource) userResource;
        }
        return this.userResource;
    }

    @Override
    @Path("{iden}")
    public VmSessionResource getSessionSubResource(@PathParam("iden") String id) {
        return inject(new BackendVmSessionResource(this, id));
    }

    @Override
    protected Response performRemove(String id) {
        throw new UnsupportedOperationException("Remove of sessions not currently possible");
    }

    @Override
    protected Session doPopulate(Session model, Object entity) {
        return model;
    }

    public void setUserResource(BackendUserResource userResource) {
        this.userResource = userResource;
    }
}
