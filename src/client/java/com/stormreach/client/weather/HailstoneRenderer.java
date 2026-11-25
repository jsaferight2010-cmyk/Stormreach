package com.stormreach.client.weather;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stormreach.Stormreach;
import com.stormreach.client.tornado.TornadoMesh;
import com.stormreach.sim.HailSize;
import com.stormreach.weather.precip.HailstoneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Draws a hailstone as a lumpy, translucent ball of ice sized to its hail class. */
public class HailstoneRenderer extends EntityRenderer<HailstoneEntity, HailstoneRenderer.State> {
    private static final Identifier TEXTURE = Stormreach.id("textures/entity/hailstone.png");

    public static class State extends EntityRenderState {
        public int size;
        public int seed;
        public float spin;
    }

    public HailstoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(HailstoneEntity e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.size = e.size().ordinal();
        s.seed = e.getId();
        s.spin = Mth.lerp(partial, e.spinO, e.spin);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(s, pose, collector, camera);
        HailSize size = HailSize.values()[Mth.clamp(s.size, 0, HailSize.values().length - 1)];
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TEXTURE), (p, vc) -> WeatherMesh.hailstone(sink(p, vc, light), size.visualSize(), s.seed, s.spin));
    }

    static TornadoMesh.Sink sink(PoseStack.Pose p, VertexConsumer vc, int light) {
        return (x, y, z, u, v, argb) -> {
            float len = Math.max(0.001f, Mth.sqrt(x * x + y * y + z * z));
            vc.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, x / len, y / len, z / len);
        };
    }
}
