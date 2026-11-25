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

/** A funnel of spinning water, eight blocks tall, widening toward the cloud. */
public class WaterspoutModel extends EntityModel<LivingEntityRenderState> {
    private static final int SEGMENTS = 8;
    private final ModelPart[] rings = new ModelPart[SEGMENTS];

    public WaterspoutModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        for (int i = 0; i < SEGMENTS; i++) rings[i] = root.getChild("ring_" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < SEGMENTS; i++) {
            int size = 6 + i * 3;
            float half = size / 2.0f;
            // Each segment is 16 pixels (one block) tall, stacked upward from the water.
            root.addOrReplaceChild("ring_" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-half, -16.0f, -half, size, 16, size),
                    PartPose.offset(0.0f, 24.0f - i * 16.0f, 0.0f));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        for (int i = 0; i < SEGMENTS; i++) {
            rings[i].yRot = state.ageInTicks * (0.35f - i * 0.02f) + i;
            rings[i].x = Mth.sin(state.ageInTicks * 0.15f + i * 0.6f) * i * 0.6f;
        }
    }
}
