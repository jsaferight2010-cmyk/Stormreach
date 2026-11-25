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

/** A plump grey seal with flippers and a tail fluke. */
public class HarborSealModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;

    public HarborSealModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.leftFlipper = root.getChild("left_flipper");
        this.rightFlipper = root.getChild("right_flipper");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -3.5f, -6.0f, 8, 7, 12),
                PartPose.offset(0.0f, 20.5f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 19).addBox(-3.0f, -3.0f, -5.0f, 6, 5, 5)
                        .texOffs(22, 19).addBox(-1.5f, -1.0f, -6.5f, 3, 2, 2),
                PartPose.offset(0.0f, 19.0f, -6.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(40, 0).addBox(-3.0f, -1.0f, 0.0f, 6, 2, 5),
                PartPose.offset(0.0f, 22.0f, 6.0f));
        root.addOrReplaceChild("left_flipper", CubeListBuilder.create().texOffs(32, 19).addBox(0.0f, -0.5f, -1.5f, 5, 1, 3),
                PartPose.offsetAndRotation(4.0f, 23.0f, -3.0f, 0.0f, 0.0f, 0.3f));
        root.addOrReplaceChild("right_flipper", CubeListBuilder.create().texOffs(32, 23).addBox(-5.0f, -0.5f, -1.5f, 5, 1, 3),
                PartPose.offsetAndRotation(-4.0f, 23.0f, -3.0f, 0.0f, 0.0f, -0.3f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float swim = state.isInWater ? state.ageInTicks * 0.4f : state.walkAnimationPos * 0.8f;
        float amount = state.isInWater ? 0.5f : Math.min(state.walkAnimationSpeed, 1.0f);
        tail.xRot = Mth.sin(swim) * 0.4f * amount;
        leftFlipper.yRot = Mth.cos(swim) * 0.6f * amount;
        rightFlipper.yRot = -Mth.cos(swim) * 0.6f * amount;
    }
}
