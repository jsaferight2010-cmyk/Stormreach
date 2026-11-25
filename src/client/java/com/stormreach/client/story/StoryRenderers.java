package com.stormreach.client.story;

import com.stormreach.client.model.gen.HollowKeeperModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.Stormreach;
import com.stormreach.labyrinth.HollowKeeper;
import com.stormreach.story.CyrrusBoss;
import com.stormreach.story.Stranger;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Humanoid renderers: the Stranger, Cyrrus (its true form; scaled by its Scale attribute) and the Hollow Keeper. */
public final class StoryRenderers {
    private StoryRenderers() {}

    public static class StrangerRenderer extends HumanoidMobRenderer<Stranger, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/stranger.png");

        public StrangerRenderer(EntityRendererProvider.Context c) {
            super(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE)), 0.0f);
        }

        @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }
        @Override public Identifier getTextureLocation(HumanoidRenderState s) { return TEXTURE; }
    }

    public static class CyrrusState extends HumanoidRenderState {
        public int form;
    }

    public static class CyrrusRenderer extends HumanoidMobRenderer<CyrrusBoss, CyrrusState, HumanoidModel<CyrrusState>> {
        private static final Identifier[] TEXTURES = {Stormreach.id("textures/entity/stranger.png"),
                Stormreach.id("textures/entity/cyrrus.png"), Stormreach.id("textures/entity/cyrrus_final.png")};

        public CyrrusRenderer(EntityRendererProvider.Context c) {
            super(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE)), 0.6f);
        }

        @Override public CyrrusState createRenderState() { return new CyrrusState(); }

        @Override
        public void extractRenderState(CyrrusBoss e, CyrrusState s, float partial) {
            super.extractRenderState(e, s, partial);
            s.form = e.form();
        }

        @Override public Identifier getTextureLocation(CyrrusState s) { return TEXTURES[Math.max(0, Math.min(2, s.form))]; }
        @Override protected int getBlockLightLevel(CyrrusBoss e, BlockPos pos) { return e.form() > 0 ? 12 : super.getBlockLightLevel(e, pos); }
    }

    public static class KeeperRenderer extends MobRenderer<HollowKeeper, LivingEntityRenderState, HollowKeeperModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/hollow_keeper.png");

        public KeeperRenderer(EntityRendererProvider.Context c) {
            super(c, new HollowKeeperModel(HollowKeeperModel.createBodyLayer().bakeRoot()), 0.7f);
        }

        @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
        @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEXTURE; }
        @Override protected void scale(LivingEntityRenderState s, PoseStack pose) { pose.scale(1.27f, 1.27f, 1.27f); }
    }
}
