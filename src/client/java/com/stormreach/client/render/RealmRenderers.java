package com.stormreach.client.render;

import com.stormreach.client.anim.RigRenderer;
import com.stormreach.client.anim.RigState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.model.gen.*;
import com.stormreach.entity.Cyclone;
import com.stormreach.entity.Puffling;
import com.stormreach.entity.Skyray;
import com.stormreach.entity.StaticWisp;
import com.stormreach.entity.Stormhawk;
import com.stormreach.entity.TempestSovereign;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Renderers for the 4.0 mobs. */
public final class RealmRenderers {
    private RealmRenderers() {}

    private static Identifier tex(String name) {
        return Stormreach.id("textures/entity/" + name + ".png");
    }

    public static class SovereignRenderer extends RigRenderer<TempestSovereign, SovereignModel> {
        private static final Identifier TEXTURE = tex("tempest_sovereign");
        public SovereignRenderer(EntityRendererProvider.Context c) { super(c, new SovereignModel(SovereignModel.createBodyLayer().bakeRoot()), 1.2f); }
        @Override public RigState createRenderState() { return new RigState(); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { pose.scale(1.6f, 1.6f, 1.6f); }
        @Override protected int getBlockLightLevel(TempestSovereign e, BlockPos pos) { return 15; }
    }

    public static class SkyrayRenderer extends RigRenderer<Skyray, SkyrayModel> {
        private static final Identifier TEXTURE = tex("skyray");
        public SkyrayRenderer(EntityRendererProvider.Context c) { super(c, new SkyrayModel(SkyrayModel.createBodyLayer().bakeRoot()), 0.9f); }
        @Override public RigState createRenderState() { return new RigState(); }
        @Override public void extractRenderState(Skyray e, RigState s, float partial) {
            super.extractRenderState(e, s, partial);
            s.saddled = e.isSaddled();
        }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { float k = s.isBaby ? 0.5f : 1.15f; pose.scale(k, k, k); }
    }

    public static class PufflingRenderer extends RigRenderer<Puffling, PufflingModel> {
        private static final Identifier TEXTURE = tex("puffling");
        public PufflingRenderer(EntityRendererProvider.Context c) { super(c, new PufflingModel(PufflingModel.createBodyLayer().bakeRoot()), 0.5f); }
        @Override public RigState createRenderState() { return new RigState(); }
        @Override public void extractRenderState(Puffling e, RigState s, float partial) {
            super.extractRenderState(e, s, partial);
            s.sheared = e.isSheared();
        }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { if (s.isBaby) pose.scale(0.5f, 0.5f, 0.5f); }
    }

    public static class StaticWispRenderer extends RigRenderer<StaticWisp, StaticWispModel> {
        private static final Identifier TEXTURE = tex("static_wisp");
        public StaticWispRenderer(EntityRendererProvider.Context c) { super(c, new StaticWispModel(StaticWispModel.createBodyLayer().bakeRoot()), 0.0f); }
        @Override public RigState createRenderState() { return new RigState(); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { pose.scale(0.8f, 0.8f, 0.8f); }
        @Override protected int getBlockLightLevel(StaticWisp e, BlockPos pos) { return 15; }
    }

    public static class CycloneRenderer extends RigRenderer<Cyclone, CycloneModel> {
        private static final Identifier TEXTURE = tex("cyclone");
        public CycloneRenderer(EntityRendererProvider.Context c) { super(c, new CycloneModel(CycloneModel.createBodyLayer().bakeRoot()), 0.4f); }
        @Override public RigState createRenderState() { return new RigState(); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { pose.scale(0.8f, 1.6f, 0.8f); }
    }

    public static class StormhawkRenderer extends RigRenderer<Stormhawk, StormhawkModel> {
        private static final Identifier TEXTURE = tex("stormhawk");
        public StormhawkRenderer(EntityRendererProvider.Context c) { super(c, new StormhawkModel(StormhawkModel.createBodyLayer().bakeRoot()), 0.5f); }
        @Override public RigState createRenderState() { return new RigState(); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { pose.scale(1.15f, 1.15f, 1.15f); }
    }
}
