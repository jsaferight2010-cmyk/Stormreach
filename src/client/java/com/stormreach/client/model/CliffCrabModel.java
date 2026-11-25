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

/** A flat shore crab: wide shell, two big claws, eye stalks and three legs per side. */
public class CliffCrabModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;
    private final ModelPart[] leftLegs = new ModelPart[3];
    private final ModelPart[] rightLegs = new ModelPart[3];

    public CliffCrabModel(ModelPart root) {
        super(root);
        this.leftClaw = root.getChild("left_claw");
        this.rightClaw = root.getChild("right_claw");
        for (int i = 0; i < 3; i++) {
            leftLegs[i] = root.getChild("left_leg_" + i);
            rightLegs[i] = root.getChild("right_leg_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -1.5f, -3.0f, 8, 3, 6),
                PartPose.offset(0.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(30, 8).addBox(-0.5f, -2.0f, -0.5f, 1, 2, 1),
                PartPose.offset(1.5f, 18.5f, -2.5f));
        root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(34, 8).addBox(-0.5f, -2.0f, -0.5f, 1, 2, 1),
                PartPose.offset(-1.5f, 18.5f, -2.5f));
        root.addOrReplaceChild("left_claw", CubeListBuilder.create().texOffs(0, 10).addBox(-1.0f, -1.0f, -4.0f, 2, 2, 4),
                PartPose.offsetAndRotation(3.0f, 20.5f, -3.0f, 0.0f, -0.35f, 0.0f));
        root.addOrReplaceChild("right_claw", CubeListBuilder.create().texOffs(12, 10).addBox(-1.0f, -1.0f, -4.0f, 2, 2, 4),
                PartPose.offsetAndRotation(-3.0f, 20.5f, -3.0f, 0.0f, 0.35f, 0.0f));
        for (int i = 0; i < 3; i++) {
            float z = -1.5f + i * 1.8f;
            root.addOrReplaceChild("left_leg_" + i, CubeListBuilder.create().texOffs(30, i * 2).addBox(0.0f, -0.5f, -0.5f, 4, 1, 1),
                    PartPose.offsetAndRotation(4.0f, 21.0f, z, 0.0f, 0.0f, 0.6f));
            root.addOrReplaceChild("right_leg_" + i, CubeListBuilder.create().texOffs(40, i * 2).addBox(-4.0f, -0.5f, -0.5f, 4, 1, 1),
                    PartPose.offsetAndRotation(-4.0f, 21.0f, z, 0.0f, 0.0f, -0.6f));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 1.6f;
        float amount = Math.min(state.walkAnimationSpeed, 1.0f);
        for (int i = 0; i < 3; i++) {
            float phase = i * 2.1f;
            leftLegs[i].yRot = Mth.cos(walk + phase) * 0.5f * amount;
            rightLegs[i].yRot = -Mth.cos(walk + phase) * 0.5f * amount;
            leftLegs[i].zRot = 0.6f + Mth.sin(walk + phase) * 0.25f * amount;
            rightLegs[i].zRot = -0.6f - Mth.sin(walk + phase) * 0.25f * amount;
        }
        float snap = Mth.sin(state.ageInTicks * 0.15f) * 0.12f;
        leftClaw.xRot = snap;
        rightClaw.xRot = -snap;
    }
}
