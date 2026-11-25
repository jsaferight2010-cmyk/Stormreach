package com.stormreach.client.story;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import com.stormreach.client.anim.RigRenderer;
import com.stormreach.client.anim.RigState;
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

    /** Render state for the Stranger: which weathered skin, and whether it has slim arms. */
    public static class StrangerState extends AvatarRenderState {
        public Identifier texture;
        public boolean slim;
    }

    /**
     * The Stranger is you, weathered: the player model wearing a storm-worn copy of the viewing player's own skin
     * (see {@link WeatheredSkins}), posed by {@link WeatheredModel}. Each player who sees it sees themselves.
     */
    public static class StrangerRenderer extends LivingEntityRenderer<Stranger, StrangerState, PlayerModel> {
        private final PlayerModel wide, slim;

        public StrangerRenderer(EntityRendererProvider.Context c) {
            super(c, new WeatheredModel(c.bakeLayer(ModelLayers.PLAYER), false), 0.0f);
            this.wide = this.model;
            this.slim = new WeatheredModel(c.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }

        @Override public StrangerState createRenderState() { return new StrangerState(); }

        @Override
        public void extractRenderState(Stranger e, StrangerState s, float partial) {
            super.extractRenderState(e, s, partial);
            HumanoidMobRenderer.extractHumanoidRenderState(e, s, partial, itemModelResolver);
            Minecraft mc = Minecraft.getInstance();
            PlayerSkin skin = mc.player != null ? mc.player.getSkin() : DefaultPlayerSkin.getDefaultSkin();
            s.skin = skin;
            s.slim = skin.model() == PlayerModelType.SLIM;
            s.texture = WeatheredSkins.get(skin);
            s.id = e.getId();
            s.showHat = s.showJacket = s.showLeftSleeve = s.showRightSleeve = s.showLeftPants = s.showRightPants = true;
            s.showCape = false;
        }

        @Override
        public void submit(StrangerState s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
            this.model = s.slim ? slim : wide;
            super.submit(s, pose, out, camera);
        }

        @Override public Identifier getTextureLocation(StrangerState s) { return s.texture; }
        @Override protected boolean shouldShowName(Stranger e, double d) { return false; }

        @Override
        protected void scale(StrangerState s, PoseStack pose) {
            pose.scale(0.9375f, 0.9375f, 0.9375f);
        }
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

    public static class KeeperRenderer extends RigRenderer<HollowKeeper, HollowKeeperModel> {
        private static final Identifier TEXTURE = Stormreach.id("textures/entity/hollow_keeper.png");

        public KeeperRenderer(EntityRendererProvider.Context c) {
            super(c, new HollowKeeperModel(HollowKeeperModel.createBodyLayer().bakeRoot()), 0.7f);
        }

        @Override public RigState createRenderState() { return new RigState(); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, PoseStack pose) { pose.scale(1.27f, 1.27f, 1.27f); }
    }
}
