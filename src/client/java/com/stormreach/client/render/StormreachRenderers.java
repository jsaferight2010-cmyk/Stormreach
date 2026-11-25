package com.stormreach.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.model.CliffCrabModel;
import com.stormreach.client.model.SquallModel;
import com.stormreach.client.model.StormGullModel;
import com.stormreach.entity.CliffCrab;
import com.stormreach.entity.Squall;
import com.stormreach.entity.StormGull;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Renderers for the three Stormreach mobs. Models are baked directly from their layer definitions. */
public final class StormreachRenderers {
    private StormreachRenderers() {}

    public static class CliffCrabRenderer extends MobRenderer<CliffCrab, LivingEntityRenderState, CliffCrabModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/cliff_crab.png");

        public CliffCrabRenderer(EntityRendererProvider.Context context) {
            super(context, new CliffCrabModel(CliffCrabModel.createBodyLayer().bakeRoot()), 0.4f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }

        @Override
        protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
            if (state.isBaby) poseStack.scale(0.5f, 0.5f, 0.5f);
        }
    }

    public static class StormGullRenderer extends MobRenderer<StormGull, LivingEntityRenderState, StormGullModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/storm_gull.png");

        public StormGullRenderer(EntityRendererProvider.Context context) {
            super(context, new StormGullModel(StormGullModel.createBodyLayer().bakeRoot()), 0.3f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }

        @Override
        protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
            if (state.isBaby) poseStack.scale(0.5f, 0.5f, 0.5f);
        }
    }

    public static class SquallRenderer extends MobRenderer<Squall, LivingEntityRenderState, SquallModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/squall.png");

        public SquallRenderer(EntityRendererProvider.Context context) {
            super(context, new SquallModel(SquallModel.createBodyLayer().bakeRoot()), 0.0f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }
    }
}
