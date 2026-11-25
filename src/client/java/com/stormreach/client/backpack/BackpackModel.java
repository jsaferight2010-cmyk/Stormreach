package com.stormreach.client.backpack;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * A backpack worn on the player's back, posed with the body. Each tier has its own texture, and bigger packs grow
 * extra parts: side pockets from Storm Crystal up, a rolled bedroll on top from Aurorite up, and the Maelstrom pack
 * carries a glowing storm crystal. The argument to setupAnim is the tier (0..6, 7 = Storm Chaser's Pack).
 */
public class BackpackModel extends Model<Integer> {
    private final ModelPart pocketL, pocketR, bedroll, crystal, straps;

    public BackpackModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        pocketL = root.getChild("pocket_left");
        pocketR = root.getChild("pocket_right");
        bedroll = root.getChild("bedroll");
        crystal = root.getChild("crystal");
        straps = root.getChild("straps");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // body space: the pivot is at the neck, +y is down, +z is behind the player; the torso's back is at z = 2
        root.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(0, 0).addBox(-4, 0, 0, 8, 10, 4), PartPose.offset(0, 1.5f, 2.6f));
        root.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(0, 14).addBox(-4.5f, -0.5f, -0.5f, 9, 4, 5), PartPose.offset(0, 1.5f, 2.6f));
        root.addOrReplaceChild("pocket_left", CubeListBuilder.create().texOffs(28, 0).addBox(-2, 0, 0, 2, 5, 3), PartPose.offset(-4, 5.5f, 3.1f));
        root.addOrReplaceChild("pocket_right", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0, 0, 0, 2, 5, 3), PartPose.offset(4, 5.5f, 3.1f));
        root.addOrReplaceChild("bedroll", CubeListBuilder.create().texOffs(0, 23).addBox(-5, -1.5f, -1.5f, 10, 3, 3), PartPose.offset(0, 0.5f, 5.2f));
        root.addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(38, 8).addBox(-1, -4, -1, 2, 4, 2), PartPose.offsetAndRotation(2.5f, 1.5f, 4.6f, 0, 0.6f, 0.25f));
        root.addOrReplaceChild("straps", CubeListBuilder.create().texOffs(48, 0).addBox(-3.5f, 0, -2.6f, 1, 9, 1, new CubeDeformation(-0.2f))
                .texOffs(48, 0).mirror().addBox(2.5f, 0, -2.6f, 1, 9, 1, new CubeDeformation(-0.2f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(Integer tier) {
        super.setupAnim(tier);
        int t = tier == null ? 0 : tier;
        pocketL.visible = pocketR.visible = t >= 3;
        bedroll.visible = t >= 5;
        crystal.visible = t == 6;
    }

    /** The armour hides the straps; without a chestplate they show across the chest. */
    public void showStraps(boolean show) {
        straps.visible = show;
    }
}
