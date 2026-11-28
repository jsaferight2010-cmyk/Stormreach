package com.stormreach.client.backpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Draws the worn backpack (see BackpackEquipment) on a player's back, over any chestplate. */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Stormreach.id("backpack"), "main");
    private static final String[] TIERS = {"diamond", "obsidian", "netherite", "storm_crystal", "voltite", "aurorite", "maelstrom", "storm_chasers"};
    private static final Identifier[] TEXTURES = new Identifier[TIERS.length];
    static {
        for (int i = 0; i < TIERS.length; i++) TEXTURES[i] = Stormreach.id("textures/entity/backpack/" + TIERS[i] + ".png");
    }

    private final BackpackModel model;

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
        this.model = new BackpackModel(BackpackModel.createLayer().bakeRoot());
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        int tier = ((BackpackRenderData) state).stormreach$backpackTier();
        if (tier < 0 || state.isInvisible) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        boolean armored = !state.chestEquipment.isEmpty();
        if (armored) pose.translate(0, 0, 1.2f / 16f); // sit on top of the chestplate instead of clipping into it
        model.showStraps(!armored);
        collector.submitModel(model, tier, pose, RenderTypes.entityCutout(TEXTURES[Math.min(tier, TEXTURES.length - 1)]), light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    /** Implemented on AvatarRenderState by a mixin: the tier of the worn backpack, or -1. */
    public interface BackpackRenderData {
        int stormreach$backpackTier();

        void stormreach$setBackpackTier(int tier);
    }
}
