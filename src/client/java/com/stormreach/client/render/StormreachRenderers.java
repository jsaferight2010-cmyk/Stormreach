package com.stormreach.client.render;

import com.stormreach.client.anim.RigRenderer;
import com.stormreach.client.anim.RigState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.client.model.gen.*;
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

    public static class CliffCrabRenderer extends RigRenderer<CliffCrab, CliffCrabModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/cliff_crab.png");

        public CliffCrabRenderer(EntityRendererProvider.Context context) {
            super(context, new CliffCrabModel(CliffCrabModel.createBodyLayer().bakeRoot()), 0.4f);
        }

        @Override
        public RigState createRenderState() {
            return new RigState();
        }

        @Override
        public Identifier getTextureLocation(RigState state) {
            return TEXTURE;
        }

        @Override
        protected void scale(RigState state, PoseStack poseStack) {
            if (state.isBaby) poseStack.scale(0.5f, 0.5f, 0.5f);
        }
    }

    public static class StormGullRenderer extends RigRenderer<StormGull, StormGullModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/storm_gull.png");

        public StormGullRenderer(EntityRendererProvider.Context context) {
            super(context, new StormGullModel(StormGullModel.createBodyLayer().bakeRoot()), 0.3f);
        }

        @Override
        public RigState createRenderState() {
            return new RigState();
        }

        @Override
        public Identifier getTextureLocation(RigState state) {
            return TEXTURE;
        }

        @Override
        protected void scale(RigState state, PoseStack poseStack) {
            if (state.isBaby) poseStack.scale(0.5f, 0.5f, 0.5f);
        }
    }

    public static class SquallRenderer extends RigRenderer<Squall, SquallModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/squall.png");

        public SquallRenderer(EntityRendererProvider.Context context) {
            super(context, new SquallModel(SquallModel.createBodyLayer().bakeRoot()), 0.0f);
        }

        @Override
        protected void scale(RigState state, PoseStack poseStack) {
            poseStack.scale(1.5f, 1.5f, 1.5f);
        }

        @Override
        public RigState createRenderState() {
            return new RigState();
        }

        @Override
        public Identifier getTextureLocation(RigState state) {
            return TEXTURE;
        }
    }
}
