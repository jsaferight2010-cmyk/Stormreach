package com.stormreach.client.tornado;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class TornadoRenderState extends EntityRenderState {
    public float width = 10f;
    public float cloudBase = 80f;
    public float reach = 1f;
    public float intensity = 1f;
    public int morph;
    public int subvortices;
    public int rotation = 1;
}
