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

/** Ball Lightning: a bright core inside a spinning shell. */
public class OrbModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart core;
    private final ModelPart shell;

    public OrbModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.0f, -2.0f, 4, 4, 4),
                PartPose.offset(0.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 8).addBox(-3.5f, -3.5f, -3.5f, 7, 7, 7),
                PartPose.offset(0.0f, 20.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        shell.yRot = state.ageInTicks * 0.3f;
        shell.xRot = state.ageInTicks * 0.2f;
        core.yRot = -state.ageInTicks * 0.5f;
    }
}
