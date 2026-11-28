package com.stormreach.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.tornado.TornadoMesh;
import com.stormreach.client.weather.HailstoneRenderer;
import com.stormreach.power.WaterWheelBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * The Water Wheel's wheel: two rims joined by eight paddles, 1.5 blocks across, turning about the block's axle at a
 * speed set by how much moving water is pushing it (the FLOW state). Spin accumulates per wheel so it never jumps.
 */
public class WaterWheelRenderer implements BlockEntityRenderer<WaterWheelBlock.Entity, WaterWheelRenderer.State> {
    private static final Identifier TEXTURE = Stormreach.id("textures/block/radar_dish.png");

    public static class State extends BlockEntityRenderState {
        public float spin;
        public boolean alongX;
    }

    public WaterWheelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(WaterWheelBlock.Entity be, State s, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, s, crumbling);
        var st = be.getBlockState();
        int flow = st.hasProperty(WaterWheelBlock.FLOW) ? st.getValue(WaterWheelBlock.FLOW) : 0;
        float rate = flow * 0.03f;
        long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        if (be.spunAt < 0) be.spunAt = now;
        if (now > be.spunAt) { be.spin += rate * Math.min(40, now - be.spunAt); be.spunAt = now; }
        s.spin = be.spin + rate * partial;
        s.alongX = !st.hasProperty(WaterWheelBlock.AXIS) || st.getValue(WaterWheelBlock.AXIS) == Direction.Axis.X;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, vc) -> build(HailstoneRenderer.sink(p, vc, light), s.spin, s.alongX));
        pose.popPose();
    }

    /** Wheel in the plane across the axle; axle along x (alongX) or z. Coordinates relative to the block centre. */
    static void build(TornadoMesh.Sink out, float spin, boolean alongX) {
        int wood = 0xFF9A6A3E, dark = 0xFF6A4628, iron = 0xFF8A929A;
        float r = 0.75f, rin = 0.62f;
        int seg = 16;
        for (float side : new float[]{-0.28f, 0.28f}) {
            for (int i = 0; i < seg; i++) {
                float a0 = spin + (float) (i * Math.PI * 2 / seg), a1 = spin + (float) ((i + 1) * Math.PI * 2 / seg);
                quad(out, alongX, side, rin, r, a0, a1, 0.05f, dark);
            }
        }
        for (int k = 0; k < 8; k++) {
            float a = spin + (float) (k * Math.PI / 4);
            paddle(out, alongX, a, 0.12f, r + 0.05f, 0.3f, wood);
            paddle(out, alongX, a + (float) (Math.PI / 8), 0.08f, rin, 0.27f, dark);
        }
        paddle(out, alongX, spin, -0.1f, 0.1f, 0.45f, iron);
        paddle(out, alongX, spin + (float) (Math.PI / 2), -0.1f, 0.1f, 0.45f, iron);
    }

    private static float[] at(boolean alongX, float axial, float radius, float angle) {
        float u = radius * (float) Math.cos(angle), v = radius * (float) Math.sin(angle);
        return alongX ? new float[]{axial, v, u} : new float[]{u, v, axial};
    }

    /** A flat ring segment at one side of the wheel, both faces. */
    private static void quad(TornadoMesh.Sink out, boolean alongX, float side, float r0, float r1, float a0, float a1, float t, int col) {
        for (float s : new float[]{side - t, side + t}) {
            float[][] v = {at(alongX, s, r0, a0), at(alongX, s, r1, a0), at(alongX, s, r1, a1), at(alongX, s, r0, a1)};
            for (int i = 0; i < 4; i++) out.vertex(v[i][0], v[i][1], v[i][2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col);
            for (int i = 3; i >= 0; i--) out.vertex(v[i][0], v[i][1], v[i][2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col);
        }
    }

    /** A radial board from r0 to r1 spanning the wheel's width (+-half), both faces. */
    private static void paddle(TornadoMesh.Sink out, boolean alongX, float a, float r0, float r1, float half, int col) {
        float[][] v = {at(alongX, -half, r0, a), at(alongX, -half, r1, a), at(alongX, half, r1, a), at(alongX, half, r0, a)};
        for (int i = 0; i < 4; i++) out.vertex(v[i][0], v[i][1], v[i][2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col);
        for (int i = 3; i >= 0; i--) out.vertex(v[i][0], v[i][1], v[i][2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col);
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 96; }
}
