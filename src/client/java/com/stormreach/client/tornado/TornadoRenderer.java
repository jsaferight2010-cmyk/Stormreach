package com.stormreach.client.tornado;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.tornado.TornadoEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** Draws the funnel (and, for multiple-vortex tornadoes, the orbiting subvortex funnels). */
public class TornadoRenderer extends EntityRenderer<TornadoEntity, TornadoRenderState> {
    private static final Identifier TEXTURE = Stormreach.id("textures/entity/tornado.png");
    private final FunnelModel model;

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FunnelModel(FunnelModel.createLayer().bakeRoot());
        this.shadowRadius = 0;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(TornadoEntity e, TornadoRenderState s, float partial) {
        super.extractRenderState(e, s, partial);
        s.width = Math.max(1.5f, e.visualWidth());
        s.cloudBase = e.cloudBase();
        s.reach = e.funnelReach();
        s.intensity = e.intensity();
        s.morph = e.morphology().ordinal();
        s.subvortices = e.subvortices();
        s.rotation = e.rotationSign();
    }

    @Override
    protected AABB getBoundingBoxForCulling(TornadoEntity e, float partial) {
        double w = Math.max(8, e.visualWidth());
        return new AABB(e.getX() - w, e.getY() - 2, e.getZ() - w, e.getX() + w, e.getY() + e.cloudBase() + 10, e.getZ() + w);
    }

    @Override
    public void submit(TornadoRenderState s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(s, pose, collector, camera);
        int alpha = (int) (110 + 120 * Math.min(1, s.intensity));
        int color = (alpha << 24) | 0x7A7F86;
        drawFunnel(s, pose, collector, 0, 0, 1f, 0f, color);
        if (s.subvortices > 0 && s.intensity > 0.6f) {
            // suction vortices orbit just inside the edge of the parent circulation
            float orbit = s.width * 0.5f * 0.75f;
            for (int i = 0; i < s.subvortices; i++) {
                double a = s.rotation * s.ageInTicks * 0.06 + i * Math.PI * 2 / s.subvortices;
                drawFunnel(s, pose, collector, (float) Math.cos(a) * orbit, (float) Math.sin(a) * orbit, 0.18f, i * 17f, (alpha << 24) | 0x5E6168);
            }
        }
    }

    private void drawFunnel(TornadoRenderState s, PoseStack pose, SubmitNodeCollector collector, float ox, float oz, float widthScale, float phase, int color) {
        pose.pushPose();
        pose.translate(ox, 0, oz);
        pose.scale(-1, -1, 1); // model space is y-down
        model.widthScale = widthScale;
        model.phase = phase;
        collector.submitModel(model, s, pose, RenderTypes.entityTranslucent(TEXTURE), s.lightCoords, OverlayTexture.NO_OVERLAY, color, null, 0);
        pose.popPose();
    }
}
