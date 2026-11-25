package com.stormreach.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The Tempest Sovereign: a crowned, robed storm spirit with a spinning ring of lightning and two orbiting orbs. */
public class SovereignModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head, leftArm, rightArm, skirt, ring, orbA, orbB;

    public SovereignModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        skirt = root.getChild("skirt");
        ring = root.getChild("ring");
        orbA = root.getChild("orb_a");
        orbB = root.getChild("orb_b");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 0).addBox(-4.5f, -10, -4.5f, 9, 2, 9), PartPose.offset(0, -2, 0));
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? -3.5f : 2.5f, z = i < 2 ? -3.5f : 2.5f;
            head.addOrReplaceChild("spike_" + i, CubeListBuilder.create().texOffs(70, 0).addBox(x, -13, z, 1, 3, 1), PartPose.ZERO);
        }
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-5, -2, -3, 10, 12, 6), PartPose.ZERO);
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 34).addBox(-6, 0, -4, 12, 10, 8), PartPose.offset(0, 10, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.5f, 0, -1.5f, 3, 12, 3), PartPose.offset(6.5f, -1, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 16).addBox(-1.5f, 0, -1.5f, 3, 12, 3), PartPose.offset(-6.5f, -1, 0));
        root.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(40, 34).addBox(-10, 0, -10, 20, 1, 20), PartPose.offset(0, 6, 0));
        root.addOrReplaceChild("orb_a", CubeListBuilder.create().texOffs(88, 0).addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(12, 2, 0));
        root.addOrReplaceChild("orb_b", CubeListBuilder.create().texOffs(88, 0).addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(-12, 2, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        leftArm.zRot = -0.35f - Mth.sin(t * 0.08f) * 0.25f;
        rightArm.zRot = 0.35f + Mth.sin(t * 0.08f) * 0.25f;
        leftArm.xRot = Mth.cos(t * 0.06f) * 0.3f - 0.4f;
        rightArm.xRot = -Mth.cos(t * 0.06f) * 0.3f - 0.4f;
        skirt.zRot = Mth.sin(t * 0.1f) * 0.06f;
        skirt.xRot = Mth.cos(t * 0.09f) * 0.06f;
        ring.yRot = t * 0.12f;
        orbA.x = Mth.cos(t * 0.1f) * 12; orbA.z = Mth.sin(t * 0.1f) * 12; orbA.y = 2 + Mth.sin(t * 0.2f) * 2;
        orbB.x = -orbA.x; orbB.z = -orbA.z; orbB.y = 2 - Mth.sin(t * 0.2f) * 2;
    }
}
