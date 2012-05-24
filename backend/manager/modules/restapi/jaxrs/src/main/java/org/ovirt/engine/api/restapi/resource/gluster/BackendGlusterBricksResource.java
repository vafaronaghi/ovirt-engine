package org.ovirt.engine.api.restapi.resource.gluster;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.GlusterBrick;
import org.ovirt.engine.api.model.GlusterBricks;
import org.ovirt.engine.api.resource.gluster.GlusterBrickResource;
import org.ovirt.engine.api.resource.gluster.GlusterBricksResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendCollectionResource;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.compat.NotImplementedException;

public class BackendGlusterBricksResource
        extends AbstractBackendCollectionResource<GlusterBrick, GlusterBrickEntity>
        implements GlusterBricksResource {

    private BackendGlusterVolumeResource parent;

    public BackendGlusterBricksResource(BackendGlusterVolumeResource parent) {
        super(GlusterBrick.class, GlusterBrickEntity.class);
        setParent(parent);
    }

    @Override
    public GlusterBricks list() {
        // TODO: To be implemented
        throw new NotImplementedException();
    }

    @Override
    public Response add(GlusterBricks bricks) {
        // TODO: To be implemented (pending backend functionality)
        throw new NotImplementedException();
    }

    @Override
    protected Response performRemove(String id) {
        // TODO: To be implemented (pending backend functionality)
        throw new NotImplementedException();
    }

    @Override
    public GlusterBrickResource getGlusterBrickSubResource(String brickId) {
        return inject(new BackendGlusterBrickResource(brickId, this));
    }

    public BackendGlusterVolumeResource getParent() {
        return parent;
    }

    public void setParent(BackendGlusterVolumeResource parent) {
        this.parent = parent;
    }

}
