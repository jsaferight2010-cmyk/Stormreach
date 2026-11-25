package com.stormreach.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.model.OrbModel;
import com.stormreach.client.model.PufflingModel;
import com.stormreach.client.model.SkyrayModel;
import com.stormreach.client.model.SovereignModel;
import com.stormreach.client.model.StormGullModel;
import com.stormreach.client.model.WaterspoutModel;
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

    public static class SovereignRenderer extends MobRenderer<TempestSovereign, LivingEntityRenderState, SovereignModel> {
        private static final Identifier TEXTURE = tex("tempest_sovereign");
        public SovereignRenderer(EntityRendererProvider.Context c) { super(c, new SovereignModel(SovereignModel.createBodyLayer().bakeRoot()), 1.2f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack pose) { pose.scale(1.9f, 1.9f, 1.9f); }
        @Override protected int getBlockLightLevel(TempestSovereign e, BlockPos pos) { return 15; }
    }

    public static class SkyrayRenderer extends MobRenderer<Skyray, LivingEntityRenderState, SkyrayModel> {
        private static final Identifier TEXTURE = tex("skyray");
        public SkyrayRenderer(EntityRendererProvider.Context c) { super(c, new SkyrayModel(SkyrayModel.createBodyLayer().bakeRoot()), 0.9f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack pose) { float k = s.isBaby ? 0.5f : 1.15f; pose.scale(k, k, k); }
    }

    public static class PufflingRenderer extends MobRenderer<Puffling, PufflingModel.State, PufflingModel> {
        private static final Identifier TEXTURE = tex("puffling");
        public PufflingRenderer(EntityRendererProvider.Context c) { super(c, new PufflingModel(PufflingModel.createBodyLayer().bakeRoot()), 0.5f); }
        @Override public PufflingModel.State createRenderState() { return new PufflingModel.State(); }
        @Override public void extractRenderState(Puffling e, PufflingModel.State s, float partial) {
            super.extractRenderState(e, s, partial);
            s.sheared = e.isSheared();
        }
        @Override public Identifier getTextureLocation(PufflingModel.State s) { return TEXTURE; }
        @Override protected void scale(PufflingModel.State s, PoseStack pose) { if (s.isBaby) pose.scale(0.5f, 0.5f, 0.5f); }
    }

    public static class StaticWispRenderer extends MobRenderer<StaticWisp, LivingEntityRenderState, OrbModel> {
        private static final Identifier TEXTURE = tex("static_wisp");
        public StaticWispRenderer(EntityRendererProvider.Context c) { super(c, new OrbModel(OrbModel.createBodyLayer().bakeRoot()), 0.0f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack pose) { pose.scale(0.8f, 0.8f, 0.8f); }
        @Override protected int getBlockLightLevel(StaticWisp e, BlockPos pos) { return 15; }
    }

    public static class CycloneRenderer extends MobRenderer<Cyclone, LivingEntityRenderState, WaterspoutModel> {
        private static final Identifier TEXTURE = tex("cyclone");
        public CycloneRenderer(EntityRendererProvider.Context c) { super(c, new WaterspoutModel(WaterspoutModel.createBodyLayer().bakeRoot()), 0.4f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack pose) { pose.scale(0.5f, 0.28f, 0.5f); }
    }

    public static class StormhawkRenderer extends MobRenderer<Stormhawk, LivingEntityRenderState, StormGullModel> {
        private static final Identifier TEXTURE = tex("stormhawk");
        public StormhawkRenderer(EntityRendererProvider.Context c) { super(c, new StormGullModel(StormGullModel.createBodyLayer().bakeRoot()), 0.5f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack pose) { pose.scale(1.7f, 1.7f, 1.7f); }
    }
}
