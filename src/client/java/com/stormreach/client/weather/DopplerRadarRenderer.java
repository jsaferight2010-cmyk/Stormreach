package com.stormreach.client.weather;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.tornado.radar.DopplerRadarBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0: draws the whole Doppler radar installation from its controller (the pedestal block): concrete pad, equipment
 * shelter, galvanised lattice tower with ladder and cable tray, railed platform and the white panelled radome, the way
 * a NEXRAD site looks. The parts below and around are invisible collision cells.
 */
public class DopplerRadarRenderer implements BlockEntityRenderer<DopplerRadarBlockEntity, DopplerRadarRenderer.State> {
    private static final Identifier TEXTURE = Stormreach.id("textures/block/radar_dish.png");

    public static class State extends BlockEntityRenderState {
        public float yaw;
    }

    public DopplerRadarRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(DopplerRadarBlockEntity be, State s, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, s, crumbling);
        long time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        s.yaw = (float) ((time + partial) * (Math.PI * 2 / 160.0));
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(com.stormreach.client.render.Mesh.white()),
                (p, vc) -> com.stormreach.client.render.Machines.dopplerRadar(com.stormreach.client.render.Mesh.to(p, vc, light)));
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() { return true; }

    @Override
    public int getViewDistance() { return 128; }
}
