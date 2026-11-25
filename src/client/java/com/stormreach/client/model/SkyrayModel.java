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

/** Skyray: a broad, flat ray with flapping wing-fins and a long whip tail. */
public class SkyrayModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart leftWing, rightWing, tail;

    public SkyrayModel(ModelPart root) {
        super(root);
        leftWing = root.getChild("left_wing");
        rightWing = root.getChild("right_wing");
        tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -1, -5, 14, 2, 10), PartPose.offset(0, 20, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 30).addBox(-3, -1, -3, 6, 2, 3), PartPose.offset(0, 20, -5));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 12).addBox(0, -0.5f, -4, 8, 1, 8), PartPose.offset(7, 20, 0));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 21).addBox(-8, -0.5f, -4, 8, 1, 8), PartPose.offset(-7, 20, 0));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 30).addBox(-0.5f, -0.5f, 0, 1, 1, 12), PartPose.offset(0, 20, 5));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float flap = Mth.sin(state.ageInTicks * 0.15f) * 0.35f;
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        tail.yRot = Mth.sin(state.ageInTicks * 0.1f) * 0.3f;
        tail.xRot = Mth.cos(state.ageInTicks * 0.12f) * 0.1f;
    }
}
