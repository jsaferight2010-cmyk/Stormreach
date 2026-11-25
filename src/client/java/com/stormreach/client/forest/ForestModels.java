package com.stormreach.client.forest;

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

    public static class StagModel extends EntityModel<LivingEntityRenderState> {
        private final ModelPart head;
        private final ModelPart[] legs = new ModelPart[4];

        public StagModel(ModelPart root) {
            super(root);
            head = root.getChild("neck");
            for (int i = 0; i < 4; i++) legs[i] = root.getChild("leg" + i);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -8, 8, 8, 16), PartPose.offset(0, 11, 0));
            PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 24).addBox(-2, -9, -2, 4, 10, 4)
                    .texOffs(16, 24).addBox(-2.5f, -12, -6, 5, 4, 7), PartPose.offset(0, 8, -7));
            neck.addOrReplaceChild("antlers", CubeListBuilder.create().texOffs(40, 0).addBox(-6, -18, -2, 1, 7, 1).addBox(5, -18, -2, 1, 7, 1)
                    .addBox(-6, -18, -2, 4, 1, 1).addBox(2, -18, -2, 4, 1, 1).addBox(-4, -16, -2, 1, 3, 1).addBox(3, -16, -2, 1, 3, 1), PartPose.ZERO);
            for (int i = 0; i < 4; i++) {
                float x = i % 2 == 0 ? -2.5f : 2.5f, z = i < 2 ? -6 : 6;
                root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(48, 16).addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(x, 14, z));
            }
            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(LivingEntityRenderState s) {
            super.setupAnim(s);
            head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.5f;
            head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.5f;
            float swing = s.walkAnimationPos * 0.6662f, amount = s.walkAnimationSpeed;
            for (int i = 0; i < 4; i++) legs[i].xRot = Mth.cos(swing + (i == 0 || i == 3 ? 0 : Mth.PI)) * 1.2f * amount;
        }
    }

    public static class HopModel extends EntityModel<LivingEntityRenderState> {
        private final ModelPart body;

        public HopModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -7, -3.5f, 7, 7, 7), PartPose.offset(0, 22, 0));
            body.addOrReplaceChild("ears", CubeListBuilder.create().texOffs(0, 14).addBox(-3, -11, 0, 2, 4, 1).addBox(1, -11, 0, 2, 4, 1), PartPose.ZERO);
            root.addOrReplaceChild("feet", CubeListBuilder.create().texOffs(14, 14).addBox(-3, 0, -2, 2, 2, 3).addBox(1, 0, -2, 2, 2, 3), PartPose.offset(0, 22, 0));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void setupAnim(LivingEntityRenderState s) {
            super.setupAnim(s);
            body.y = 22 - Math.abs(Mth.sin(s.walkAnimationPos * 0.8f)) * 2 * s.walkAnimationSpeed;
            body.yRot = s.yRot * Mth.DEG_TO_RAD * 0.3f;
        }
    }

    public static class StagRenderer extends MobRenderer<Glimmerstag, LivingEntityRenderState, StagModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/glimmerstag.png");
        public StagRenderer(EntityRendererProvider.Context c) { super(c, new StagModel(StagModel.createLayer().bakeRoot()), 0.6f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack p) { if (s.isBaby) p.scale(0.5f, 0.5f, 0.5f); }
        @Override protected int getBlockLightLevel(Glimmerstag e, BlockPos pos) { return Math.max(8, super.getBlockLightLevel(e, pos)); }
    }

    public static class HopRenderer extends MobRenderer<Thistlehop, LivingEntityRenderState, HopModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/thistlehop.png");
        public HopRenderer(EntityRendererProvider.Context c) { super(c, new HopModel(HopModel.createLayer().bakeRoot()), 0.3f); }
        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack p) { if (s.isBaby) p.scale(0.6f, 0.6f, 0.6f); }
    }
}
