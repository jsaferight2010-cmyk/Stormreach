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

/** Puffling: a small four-legged body inside a big puff of cloud (hidden once sheared). */
public class PufflingModel extends EntityModel<PufflingModel.State> {
    public static class State extends LivingEntityRenderState {
        public boolean sheared;
    }

    private final ModelPart fluff, head;
    private final ModelPart[] legs = new ModelPart[4];

    public PufflingModel(ModelPart root) {
        super(root);
        fluff = root.getChild("fluff");
        head = root.getChild("head");
        for (int i = 0; i < 4; i++) legs[i] = root.getChild("leg_" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -5, 8, 6, 10), PartPose.offset(0, 16, 0));
        root.addOrReplaceChild("fluff", CubeListBuilder.create().texOffs(0, 16).addBox(-6, -5, -7, 12, 9, 14), PartPose.offset(0, 16, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-3, -4, -6, 6, 6, 6), PartPose.offset(0, 13, -5));
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? -2.5f : 2.5f, z = i < 2 ? -3.5f : 3.5f;
            root.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(0, 40).addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(x, 19, z));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        fluff.visible = !state.sheared;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        float swing = state.walkAnimationPos * 0.6662f, amount = state.walkAnimationSpeed;
        legs[0].xRot = Mth.cos(swing) * 1.4f * amount;
        legs[3].xRot = Mth.cos(swing) * 1.4f * amount;
        legs[1].xRot = Mth.cos(swing + Mth.PI) * 1.4f * amount;
        legs[2].xRot = Mth.cos(swing + Mth.PI) * 1.4f * amount;
    }
}
