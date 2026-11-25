package com.stormreach.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/** A spinning funnel of storm cloud: each layer turns at its own speed. Drawn see-through. */
public class SquallModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart ring;
    private final ModelPart body;
    private final ModelPart tail;

    public SquallModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.head = root.getChild("head");
        this.ring = root.getChild("ring");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.0f, -3.0f, 6, 6, 6),
                PartPose.offset(0.0f, 9.0f, 0.0f));
        root.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 12).addBox(-5.0f, -1.0f, -5.0f, 10, 2, 10),
                PartPose.offset(0.0f, 13.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0f, -2.5f, -2.0f, 4, 5, 4),
                PartPose.offset(0.0f, 16.5f, 0.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0f, -2.0f, -1.0f, 2, 4, 2),
                PartPose.offset(0.0f, 21.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        head.yRot = t * 0.12f;
        ring.yRot = -t * 0.25f;
        body.yRot = t * 0.4f;
        tail.yRot = -t * 0.6f;
        float sway = Mth.sin(t * 0.2f);
        tail.x = sway * 1.2f;
        body.x = sway * 0.6f;
        head.y = 9.0f + Mth.sin(t * 0.1f) * 0.5f;
    }
}
