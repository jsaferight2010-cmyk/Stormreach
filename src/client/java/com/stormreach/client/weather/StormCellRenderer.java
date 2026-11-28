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
    private static final Identifier RAIN = Stormreach.id("textures/entity/rain_shaft.png");

    public static class State extends EntityRenderState {
        public float growth, width, depth;
        public boolean severe;
        public int seed;
        public float drop;
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
        // how far the ground is below the cloud base, for the rain shaft (client heightmap, loaded chunks only)
        var lvl = e.level();
        int gx = net.minecraft.util.Mth.floor(e.getX()), gz = net.minecraft.util.Mth.floor(e.getZ());
        int ground = lvl.hasChunkAt(new net.minecraft.core.BlockPos(gx, 0, gz))
                ? lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz) : lvl.getSeaLevel();
        s.drop = (float) Math.max(0, e.getY() - ground);
    }

    @Override
    protected AABB getBoundingBoxForCulling(StormCellEntity e, float partial) {
        double w = e.cloudWidth() * 2.5 + 20;
        return new AABB(e.getX() - w, e.getY() - 160, e.getZ() - w, e.getX() + w, e.getY() + e.cloudDepth() + 30, e.getZ() + w);
    }

    @Override
    protected boolean affectedByCulling(StormCellEntity e) { return true; }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(s, pose, collector, camera);
        // lit like the sky at the cloud base, so storm clouds darken at night
        int light = s.lightCoords;
        // 6.0: opaque billows (cutout) so the cloud reads as one solid mass, then the translucent rain shaft below it
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE),
                (p, vc) -> WeatherMesh.stormCloud(HailstoneRenderer.sink(p, vc, light), s.growth, s.width, s.depth, s.severe, s.seed, s.ageInTicks));
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(RAIN),
                (p, vc) -> WeatherMesh.rainShaft(HailstoneRenderer.sink(p, vc, light), s.growth, s.width, s.drop, s.severe, s.ageInTicks));
    }
}
