package com.stormreach.client.tornado;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.client.render.Mesh;
import com.stormreach.tornado.FireWhirlEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/**
 * 6.0: the fire whirl: three nested, twisting columns of flame (white-yellow core, orange body, dark red outer flames
 * thinning into smoke), each a ring stack that sways and spins, drawn full-bright so it lights up the Nether gloom.
 */
public class FireWhirlRenderer extends EntityRenderer<FireWhirlEntity, FireWhirlRenderer.State> {
    public static class State extends EntityRenderState {
        public float width, height, strength, spin;
    }

    public FireWhirlRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0; }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(FireWhirlEntity e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.width = e.whirlWidth(); s.height = e.whirlHeight(); s.strength = e.strength();
        s.spin = e.spinO + (e.spin - e.spinO) * partial;
    }

    @Override
    protected AABB getBoundingBoxForCulling(FireWhirlEntity e, float partial) {
        double w = e.whirlWidth() + 4;
        return new AABB(e.getX() - w, e.getY() - 1, e.getZ() - w, e.getX() + w, e.getY() + e.whirlHeight() + 6, e.getZ() + w);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(s, pose, collector, camera);
        if (s.strength <= 0.02f) return;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(Mesh.white()), (p, vc) -> {
            Mesh.Out o = Mesh.to(p, vc, 0xF000F0);
            column(o, s, 1.35f, 0x30200C08, 0x10302020, 0.6f, 2.1f);   // outer flames thinning into smoke
            column(o, s, 1.0f, 0xC8E0501A, 0x40A02010, 1.0f, 0f);      // orange body
            column(o, s, 0.5f, 0xF0FFE6A0, 0x90FFB040, 1.6f, 4.4f);    // hot core
        });
    }

    private static void column(Mesh.Out o, State s, float scale, int bottom, int top, float spinMul, float phase) {
        int rings = 22, seg = 16;
        float h = s.height * (0.4f + 0.6f * s.strength), r0 = s.width * 0.5f * scale * (0.5f + 0.5f * s.strength);
        float[][][] ring = new float[rings + 1][seg + 1][];
        int[] col = new int[rings + 1];
        for (int i = 0; i <= rings; i++) {
            float k = (float) i / rings, y = k * h;
            // narrow at the ground, widest a third of the way up, curling in and out toward the top
            float r = r0 * (0.55f + 0.7f * (float) Math.sin(Math.PI * Math.min(1, k * 1.5f)) * (1 - 0.35f * k));
            float cx = (float) Math.sin(s.spin * 0.07f + k * 4 + phase) * r0 * 0.35f * k, cz = (float) Math.cos(s.spin * 0.05f + k * 3 + phase) * r0 * 0.35f * k;
            for (int j = 0; j <= seg; j++) {
                double a = j * Math.PI * 2 / seg + s.spin * spinMul + k * 2.4;
                float lick = 1 + 0.25f * (float) Math.sin(a * 3 + s.spin * 2.3f + k * 9 + phase);
                ring[i][j] = new float[]{cx + (float) Math.cos(a) * r * lick, y, cz + (float) Math.sin(a) * r * lick};
            }
            col[i] = Mesh.mix(bottom, top, k);
        }
        for (int i = 0; i < rings; i++)
            for (int j = 0; j < seg; j++)
                o.quad(ring[i][j], ring[i][j + 1], ring[i + 1][j + 1], ring[i + 1][j], Mesh.mix(col[i], col[i + 1], 0.5f), 0, 1, 0);
    }
}
