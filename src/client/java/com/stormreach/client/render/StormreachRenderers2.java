package com.stormreach.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.model.gen.*;
import com.stormreach.entity.BallLightning;
import com.stormreach.entity.HarborSeal;
import com.stormreach.entity.Stormwarden;
import com.stormreach.entity.ThunderCrab;
import com.stormreach.entity.Waterspout;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Renderers for the mobs added in Stormreach 2.0. */
public final class StormreachRenderers2 {
    private StormreachRenderers2() {}

    public static class ThunderCrabRenderer extends MobRenderer<ThunderCrab, LivingEntityRenderState, ThunderCrabModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/thunder_crab.png");

        public ThunderCrabRenderer(EntityRendererProvider.Context context) {
            super(context, new ThunderCrabModel(ThunderCrabModel.createBodyLayer().bakeRoot()), 0.6f);
        }

        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState state, PoseStack poseStack) { poseStack.scale(1.0f, 1.0f, 1.0f); }
        @Override protected int getBlockLightLevel(ThunderCrab entity, BlockPos pos) { return 10; }
    }

    public static class HarborSealRenderer extends MobRenderer<HarborSeal, LivingEntityRenderState, HarborSealModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/harbor_seal.png");

        public HarborSealRenderer(EntityRendererProvider.Context context) {
            super(context, new HarborSealModel(HarborSealModel.createBodyLayer().bakeRoot()), 0.5f);
        }

        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState state, PoseStack poseStack) { if (state.isBaby) poseStack.scale(0.5f, 0.5f, 0.5f); }
    }

    public static class BrineWraithRenderer extends ZombieRenderer {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/brine_wraith.png");

        public BrineWraithRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override public Identifier getTextureLocation(ZombieRenderState state) { return TEXTURE; }
    }

    public static class BallLightningRenderer extends MobRenderer<BallLightning, LivingEntityRenderState, BallLightningModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/ball_lightning.png");

        public BallLightningRenderer(EntityRendererProvider.Context context) {
            super(context, new BallLightningModel(BallLightningModel.createBodyLayer().bakeRoot()), 0.0f);
        }

        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
        @Override protected int getBlockLightLevel(BallLightning entity, BlockPos pos) { return 15; }
    }

    public static class WaterspoutRenderer extends MobRenderer<Waterspout, LivingEntityRenderState, WaterspoutModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/waterspout.png");

        public WaterspoutRenderer(EntityRendererProvider.Context context) {
            super(context, new WaterspoutModel(WaterspoutModel.createBodyLayer().bakeRoot()), 0.0f);
        }

        @Override protected void scale(LivingEntityRenderState state, PoseStack poseStack) { poseStack.scale(1.4f, 5.8f, 1.4f); }

        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
    }

    public static class StormwardenRenderer extends MobRenderer<Stormwarden, LivingEntityRenderState, StormwardenModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/stormwarden.png");

        public StormwardenRenderer(EntityRendererProvider.Context context) {
            super(context, new StormwardenModel(StormwardenModel.createBodyLayer().bakeRoot()), 1.2f);
        }

        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState state, PoseStack poseStack) { poseStack.scale(1.55f, 1.55f, 1.55f); }
        @Override protected int getBlockLightLevel(Stormwarden entity, BlockPos pos) { return 12; }
    }
}
