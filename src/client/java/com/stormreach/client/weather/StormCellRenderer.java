package com.stormreach.client.weather;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.weather.cloud.StormCellEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** Draws a developing storm cloud from its synced growth (see WeatherMesh.stormCloud and sim.CloudGrowth). */
public class StormCellRenderer extends EntityRenderer<StormCellEntity, StormCellRenderer.State> {
    private static final Identifier TEXTURE = Stormreach.id("textures/entity/storm_cloud.png");

    public static class State extends EntityRenderState {
        public float growth, width, depth;
        public boolean severe;
        public int seed;
    }

    public StormCellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(StormCellEntity e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.growth = e.growth();
        s.width = e.cloudWidth();
        s.depth = e.cloudDepth();
        s.severe = e.severe();
        s.seed = e.getUUID().hashCode();
    }

    @Override
    protected AABB getBoundingBoxForCulling(StormCellEntity e, float partial) {
        double w = e.cloudWidth() * 2.5 + 20;
        return new AABB(e.getX() - w, e.getY() - 10, e.getZ() - w, e.getX() + w, e.getY() + e.cloudDepth() + 30, e.getZ() + w);
    }

    @Override
    protected boolean affectedByCulling(StormCellEntity e) { return true; }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(s, pose, collector, camera);
        // lit like the sky at the cloud base, so storm clouds darken at night
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TEXTURE),
                (p, vc) -> WeatherMesh.stormCloud(HailstoneRenderer.sink(p, vc, light), s.growth, s.width, s.depth, s.severe, s.seed, s.ageInTicks));
    }
}
