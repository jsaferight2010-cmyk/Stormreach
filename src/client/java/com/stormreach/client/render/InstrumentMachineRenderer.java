package com.stormreach.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.client.ClientWeather;
import com.stormreach.tornado.InstrumentBlock;
import com.stormreach.tornado.InstrumentBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** Draws the tornado siren (pole, cabinet, rotating head) and the weather station mast (spinning cups, live vane). */
public class InstrumentMachineRenderer implements BlockEntityRenderer<InstrumentBlockEntity, InstrumentMachineRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public boolean siren;
        public float spin, sirenYaw, vaneYaw;
    }

    public InstrumentMachineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(InstrumentBlockEntity be, State s, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, s, crumbling);
        var st = be.getBlockState();
        s.siren = st.getBlock() instanceof InstrumentBlock ib && ib.kind() == InstrumentBlock.Kind.TORNADO_SIREN;
        long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        if (be.animAt < 0) be.animAt = now;
        long dt = Math.min(40, Math.max(0, now - be.animAt));
        be.animAt = now;
        if (s.siren) {
            boolean sounding = st.hasProperty(InstrumentBlock.POWER) && st.getValue(InstrumentBlock.POWER) > 0;
            float rate = sounding ? (float) (Math.PI * 2 / 120.0) : 0;     // one sweep every six seconds while sounding
            be.sirenYaw += rate * dt;
            s.sirenYaw = be.sirenYaw + rate * partial;
        } else {
            float rate = 0.05f + ClientWeather.windMph() * 0.012f;          // cups turn faster in stronger wind
            be.spin += rate * dt;
            s.spin = be.spin + rate * partial;
            float[] w = ClientWeather.windDir();
            // the vane's arrowhead (local -z) points into the wind, i.e. along -wind: yaw = atan2(wx, wz)
            s.vaneYaw = (float) Math.atan2(w[0], w[1]);
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(Mesh.white()), (p, vc) -> {
            Mesh.Out o = Mesh.to(p, vc, light);
            if (s.siren) Machines.siren(o, s.sirenYaw); else Machines.weatherStation(o, s.spin, s.vaneYaw);
        });
        pose.popPose();
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 128; }
}
