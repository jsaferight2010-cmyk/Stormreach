package com.stormreach.client.wild;

import com.stormreach.Stormreach;
import com.stormreach.chase.InterceptVehicle;
import com.stormreach.chase.WeatherBalloon;
import com.stormreach.client.anim.RigRenderer;
import com.stormreach.client.anim.RigState;
import com.stormreach.client.model.gen.*;
import com.stormreach.wild.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Renderers for the 5.2 creatures, the intercept vehicle and the weather balloon. */
public final class WildRenderers {
    private WildRenderers() {}

    private static Identifier tex(String name) {
        return Stormreach.id("textures/entity/" + name + ".png");
    }

    public static class CloudWhaleRenderer extends RigRenderer<CloudWhale, CloudWhaleModel> {
        private static final Identifier TEXTURE = tex("cloud_whale");
        public CloudWhaleRenderer(EntityRendererProvider.Context c) { super(c, new CloudWhaleModel(CloudWhaleModel.createBodyLayer().bakeRoot()), 2.2f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
    }

    public static class ShockEelRenderer extends RigRenderer<ShockEel, ShockEelModel> {
        private static final Identifier TEXTURE = tex("shock_eel");
        public ShockEelRenderer(EntityRendererProvider.Context c) { super(c, new ShockEelModel(ShockEelModel.createBodyLayer().bakeRoot()), 0.3f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(ShockEel e, BlockPos p) { return Math.max(6, super.getBlockLightLevel(e, p)); }
    }

    public static class SparkskinkRenderer extends RigRenderer<Sparkskink, SparkskinkModel> {
        private static final Identifier TEXTURE = tex("sparkskink");
        public SparkskinkRenderer(EntityRendererProvider.Context c) { super(c, new SparkskinkModel(SparkskinkModel.createBodyLayer().bakeRoot()), 0.3f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override public void extractRenderState(Sparkskink e, RigState s, float partial) { super.extractRenderState(e, s, partial); s.charged = e.isCharged(); }
        @Override protected int getBlockLightLevel(Sparkskink e, BlockPos p) { return e.isCharged() ? Math.max(9, super.getBlockLightLevel(e, p)) : super.getBlockLightLevel(e, p); }
        @Override protected void scale(RigState s, com.mojang.blaze3d.vertex.PoseStack pose) { if (s.isBaby) pose.scale(0.5f, 0.5f, 0.5f); }
    }

    public static class StormJellyRenderer extends RigRenderer<StormJelly, StormJellyModel> {
        private static final Identifier TEXTURE = tex("storm_jelly");
        public StormJellyRenderer(EntityRendererProvider.Context c) { super(c, new StormJellyModel(StormJellyModel.createBodyLayer().bakeRoot()), 0.0f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(StormJelly e, BlockPos p) { return 12; }
    }

    public static class GaleSpriteRenderer extends RigRenderer<GaleSprite, GaleSpriteModel> {
        private static final Identifier TEXTURE = tex("gale_sprite");
        public GaleSpriteRenderer(EntityRendererProvider.Context c) { super(c, new GaleSpriteModel(GaleSpriteModel.createBodyLayer().bakeRoot()), 0.15f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(GaleSprite e, BlockPos p) { return Math.max(10, super.getBlockLightLevel(e, p)); }
    }

    public static class HermitRenderer extends RigRenderer<TidewrackHermit, TidewrackHermitModel> {
        private static final Identifier TEXTURE = tex("tidewrack_hermit");
        public HermitRenderer(EntityRendererProvider.Context c) { super(c, new TidewrackHermitModel(TidewrackHermitModel.createBodyLayer().bakeRoot()), 0.35f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override public void extractRenderState(TidewrackHermit e, RigState s, float partial) { super.extractRenderState(e, s, partial); s.hide = smooth(e, e.isHiding() ? 1f : 0f, 0.35f); }
        @Override protected void scale(RigState s, com.mojang.blaze3d.vertex.PoseStack pose) { if (s.isBaby) pose.scale(0.5f, 0.5f, 0.5f); }
    }

    public static class ThunderbirdRenderer extends RigRenderer<Thunderbird, ThunderbirdModel> {
        private static final Identifier TEXTURE = tex("thunderbird");
        public ThunderbirdRenderer(EntityRendererProvider.Context c) { super(c, new ThunderbirdModel(ThunderbirdModel.createBodyLayer().bakeRoot()), 1.2f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
    }

    public static class VehicleRenderer extends RigRenderer<InterceptVehicle, InterceptVehicleModel> {
        private static final Identifier TEXTURE = tex("intercept_vehicle");
        public VehicleRenderer(EntityRendererProvider.Context c) { super(c, new InterceptVehicleModel(InterceptVehicleModel.createBodyLayer().bakeRoot()), 1.4f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override public void extractRenderState(InterceptVehicle e, RigState s, float partial) {
            super.extractRenderState(e, s, partial);
            s.anchored = e.isAnchored();
            s.hide = smooth(e, s.anchored ? 1f : 0f, 0.35f);   // spikes slam down in about a third of a second
            var mc = net.minecraft.client.Minecraft.getInstance();
            s.firstPerson = mc.player != null && e.hasPassenger(mc.player) && mc.getCameraEntity() == mc.player && mc.options.getCameraType().isFirstPerson();
        }
    }

    public static class BalloonRenderer extends RigRenderer<WeatherBalloon, WeatherBalloonModel> {
        private static final Identifier TEXTURE = tex("weather_balloon");
        public BalloonRenderer(EntityRendererProvider.Context c) { super(c, new WeatherBalloonModel(WeatherBalloonModel.createBodyLayer().bakeRoot()), 0.0f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
    }
}
