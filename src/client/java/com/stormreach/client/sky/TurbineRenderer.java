package com.stormreach.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.tornado.TornadoMesh;
import com.stormreach.client.weather.HailstoneRenderer;
import com.stormreach.sky.WindTurbineBlock;
import com.stormreach.sky.WindTurbineBlockEntity;
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
 * The Wind Turbine's rotor: three tapered blades on a nose cone, in front of the nacelle, turning faster the harder the
 * wind blows (from the block's POWER). Spin is accumulated per turbine so a change in speed never makes it jump.
 */
public class TurbineRenderer implements BlockEntityRenderer<WindTurbineBlockEntity, TurbineRenderer.State> {
    private static final Identifier TEXTURE = Stormreach.id("textures/block/radar_dish.png");

    public static class State extends BlockEntityRenderState {
        public float spin, facingYaw;
    }

    public TurbineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(WindTurbineBlockEntity be, State s, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, s, crumbling);
        var st = be.getBlockState();
        int power = st.hasProperty(WindTurbineBlock.POWER) ? st.getValue(WindTurbineBlock.POWER) : 0;
        float rate = 0.01f + power * 0.012f;   // a big rotor turns slowly
        long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        if (be.spunAt < 0) be.spunAt = now;
        if (now > be.spunAt) { be.spin += rate * Math.min(40, now - be.spunAt); be.spunAt = now; }
        s.spin = be.spin + rate * partial;
        Direction f = st.hasProperty(WindTurbineBlock.FACING) ? st.getValue(WindTurbineBlock.FACING) : Direction.NORTH;
        // the nacelle yaws to keep the rotor (local -z) facing into the wind, easing round slowly like the real thing
        float[] w = com.stormreach.client.ClientWeather.windDir();
        float target = (float) Math.atan2(w[0], w[1]);
        if (Float.isNaN(be.yawNow)) be.yawNow = (180f - f.toYRot()) * (float) Math.PI / 180f;
        float diff = (float) Math.atan2(Math.sin(target - be.yawNow), Math.cos(target - be.yawNow));
        be.yawNow += diff * Math.min(1f, 0.004f * Math.max(1, now - be.yawAt)); be.yawAt = now;
        s.facingYaw = be.yawNow;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(com.stormreach.client.render.Mesh.white()),
                (p, vc) -> com.stormreach.client.render.Machines.windTurbine(com.stormreach.client.render.Mesh.to(p, vc, light), s.facingYaw, s.spin, com.stormreach.sky.WindTurbineBlock.TOWER));
        pose.popPose();
    }

    /** Hub at (0, 1.05, 0.42) facing +z before the yaw turn; blades 1.1 blocks long. */
    static void build(TornadoMesh.Sink out, float spin, float yaw) {
        int white = 0xFFF2F4F6, shade = 0xFFC4CAD0, tip = 0xFFE06A3A, grey = 0xFF8A9098;
        float hy = 1.05f, hz = -0.42f;
        // nose cone: a short square spinner
        beam(out, new float[]{0, hy, hz}, new float[]{0, hy, hz - 0.18f}, 0.09f, spin, yaw, hy, grey);
        for (int k = 0; k < 3; k++) {
            float a = spin + (float) (k * Math.PI * 2 / 3);
            float c = (float) Math.cos(a), sn = (float) Math.sin(a);
            // root, mid and tip segments, narrowing; the last span painted with an orange warning band
            float[] r0 = {0.08f * c, hy + 0.08f * sn, hz - 0.08f}, r1 = {0.55f * c, hy + 0.55f * sn, hz - 0.08f},
                    r2 = {0.95f * c, hy + 0.95f * sn, hz - 0.08f}, r3 = {1.12f * c, hy + 1.12f * sn, hz - 0.08f};
            flat(out, r0, r1, 0.075f, c, sn, yaw, white);
            flat(out, r1, r2, 0.055f, c, sn, yaw, shade);
            flat(out, r2, r3, 0.04f, c, sn, yaw, tip);
        }
    }

    private static float[] turn(float[] p, float yaw) {
        float x = p[0] * (float) Math.cos(yaw) + p[2] * (float) Math.sin(yaw);
        float z = -p[0] * (float) Math.sin(yaw) + p[2] * (float) Math.cos(yaw);
        return new float[]{x, p[1], z};
    }

    /** A flat blade quad (both faces) from a to b, widened across the blade (perpendicular in the rotor plane). */
    private static void flat(TornadoMesh.Sink out, float[] a, float[] b, float w, float c, float sn, float yaw, int col) {
        float px = -sn * w, py = c * w;
        float[][] q = {{a[0] + px, a[1] + py, a[2] - 0.01f}, {b[0] + px, b[1] + py, b[2] + 0.01f}, {b[0] - px, b[1] - py, b[2] - 0.01f}, {a[0] - px, a[1] - py, a[2] + 0.01f}};
        for (int i = 0; i < 4; i++) q[i] = turn(q[i], yaw);
        for (int i : new int[]{0, 1, 2, 3}) out.vertex(q[i][0], q[i][1], q[i][2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col);
        for (int i : new int[]{3, 2, 1, 0}) out.vertex(q[i][0], q[i][1], q[i][2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col);
    }

    /** A square beam along z from a to b (the spinner), turned by spin about its own axis. */
    private static void beam(TornadoMesh.Sink out, float[] a, float[] b, float w, float spin, float yaw, float hy, int col) {
        float[][] ring = new float[4][];
        for (int k = 0; k < 4; k++) {
            float ang = spin + (float) (k * Math.PI / 2 + Math.PI / 4);
            ring[k] = new float[]{(float) Math.cos(ang) * w * 1.41f, (float) Math.sin(ang) * w * 1.41f};
        }
        for (int k = 0; k < 4; k++) {
            float[] p = ring[k], q = ring[(k + 1) % 4];
            float[][] v = {{a[0] + p[0], a[1] + p[1], a[2]}, {a[0] + q[0], a[1] + q[1], a[2]}, {b[0] + q[0] * 0.5f, b[1] + q[1] * 0.5f, b[2]}, {b[0] + p[0] * 0.5f, b[1] + p[1] * 0.5f, b[2]}};
            for (int i = 0; i < 4; i++) { float[] t = turn(v[i], yaw); out.vertex(t[0], t[1], t[2], i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, col); }
        }
        float[][] cap = new float[4][];
        for (int k = 0; k < 4; k++) cap[k] = turn(new float[]{b[0] + ring[k][0] * 0.5f, b[1] + ring[k][1] * 0.5f, b[2]}, yaw);
        for (int k = 3; k >= 0; k--) out.vertex(cap[k][0], cap[k][1], cap[k][2], 0.5f, 0.5f, col);
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 256; }
}
