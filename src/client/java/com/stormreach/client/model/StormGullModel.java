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

/** A grey-and-white sea gull with broad wings. */
public class StormGullModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public StormGullModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.0f, -4.0f, 4, 4, 7),
                PartPose.offset(0.0f, 18.0f, 0.0f));
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.5f, -3.0f, -2.5f, 3, 3, 3),
                PartPose.offset(0.0f, 16.5f, -3.5f));
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5f, -1.8f, -4.5f, 1, 1, 2),
                PartPose.ZERO);
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 8).addBox(-1.5f, 0.0f, 0.0f, 3, 1, 3),
                PartPose.offsetAndRotation(0.0f, 17.0f, 3.0f, -0.2f, 0.0f, 0.0f));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 12).addBox(0.0f, 0.0f, -2.0f, 6, 1, 5),
                PartPose.offset(2.0f, 16.5f, -1.0f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 18).addBox(-6.0f, 0.0f, -2.0f, 6, 1, 5),
                PartPose.offset(-2.0f, 16.5f, -1.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 8).addBox(-0.5f, 0.0f, -0.5f, 1, 4, 1),
                PartPose.offset(1.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(44, 8).addBox(-0.5f, 0.0f, -0.5f, 1, 4, 1),
                PartPose.offset(-1.0f, 20.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float flap = Mth.sin(state.ageInTicks * 0.7f) * 0.5f;
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        float walk = state.walkAnimationPos * 0.9f;
        leftLeg.xRot = Mth.cos(walk) * 1.2f * state.walkAnimationSpeed;
        rightLeg.xRot = Mth.cos(walk + Mth.PI) * 1.2f * state.walkAnimationSpeed;
    }
}
