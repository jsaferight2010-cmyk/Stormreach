package com.stormreach.client.forest;

import com.stormreach.client.model.gen.GlimmerstagModel;
import com.stormreach.client.model.gen.ThistlehopModel;

import com.stormreach.Stormreach;
import com.stormreach.forest.Glimmerstag;
import com.stormreach.forest.Thistlehop;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Models and renderers for the Elderstorm Grove's creatures. */
public final class ForestModels {
    private ForestModels() {}

    public static class StagRenderer extends MobRenderer<Glimmerstag, LivingEntityRenderState, GlimmerstagModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/glimmerstag.png");
        public StagRenderer(EntityRendererProvider.Context c) { super(c, new GlimmerstagModel(GlimmerstagModel.createBodyLayer().bakeRoot()), 0.6f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack p) { if (s.isBaby) p.scale(0.5f, 0.5f, 0.5f); }
        @Override protected int getBlockLightLevel(Glimmerstag e, BlockPos pos) { return Math.max(8, super.getBlockLightLevel(e, pos)); }
    }

    public static class HopRenderer extends MobRenderer<Thistlehop, LivingEntityRenderState, ThistlehopModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/thistlehop.png");
        public HopRenderer(EntityRendererProvider.Context c) { super(c, new ThistlehopModel(ThistlehopModel.createBodyLayer().bakeRoot()), 0.3f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack p) { if (s.isBaby) p.scale(0.6f, 0.6f, 0.6f); }
    }
}
