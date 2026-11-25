package com.stormreach.client.tornado;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class TornadoRenderState extends EntityRenderState {
    public float width = 10f;
    public float cloudBase = 80f;
    public float reach = 1f;
    public float intensity = 1f;
    public int morph;
    public int formation;
    public int subvortices;
    public int rotation = 1;
    /** A stable per-tornado number, so two tornadoes never wobble in step. */
    public float seed;
    /** Accumulated spin angle, radians (see TornadoEntity.clientSpin). */
    public float spin;
    /** Creative see-inside view (see TornadoSeeThrough). */
    public boolean seeThrough;
}
