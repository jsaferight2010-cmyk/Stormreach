package com.stormreach.client;

import com.stormreach.client.render.StormreachRenderers;
import com.stormreach.client.render.StormreachRenderers2;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import com.stormreach.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class StormreachClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientUnits.init();
        com.stormreach.client.wind.WindGeneratorScreen.register();
        com.stormreach.client.story.CinematicCamera.register();
        EntityRendererRegistry.register(ModEntities.CLIFF_CRAB, StormreachRenderers.CliffCrabRenderer::new);
        EntityRendererRegistry.register(ModEntities.STORM_GULL, StormreachRenderers.StormGullRenderer::new);
        EntityRendererRegistry.register(ModEntities.SQUALL, StormreachRenderers.SquallRenderer::new);
        EntityRendererRegistry.register(ModEntities.THUNDER_CRAB, StormreachRenderers2.ThunderCrabRenderer::new);
        EntityRendererRegistry.register(ModEntities.HARBOR_SEAL, StormreachRenderers2.HarborSealRenderer::new);
        EntityRendererRegistry.register(ModEntities.BRINE_WRAITH, StormreachRenderers2.BrineWraithRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALL_LIGHTNING, StormreachRenderers2.BallLightningRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATERSPOUT, StormreachRenderers2.WaterspoutRenderer::new);
        EntityRendererRegistry.register(ModEntities.STORMWARDEN, StormreachRenderers2.StormwardenRenderer::new);
        EntityRendererRegistry.register(ModEntities.SQUALL_BOTTLE, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.STORM_FRONT, net.minecraft.client.renderer.entity.NoopRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.TEMPEST_SOVEREIGN, com.stormreach.client.render.RealmRenderers.SovereignRenderer::new);
        // 6.0: the Tempest Eye over the altar, drawn as the item itself, 2.5x and full-bright
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.TEMPEST_EYE_REWARD, c -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(c, 2.5f, true));
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.SKYRAY, com.stormreach.client.render.RealmRenderers.SkyrayRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.PUFFLING, com.stormreach.client.render.RealmRenderers.PufflingRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.STATIC_WISP, com.stormreach.client.render.RealmRenderers.StaticWispRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.CYCLONE, com.stormreach.client.render.RealmRenderers.CycloneRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.STORMHAWK, com.stormreach.client.render.RealmRenderers.StormhawkRenderer::new);
        EntityRendererRegistry.register(com.stormreach.tornado.TornadoTypes.TORNADO, com.stormreach.client.tornado.TornadoRenderer::new);
        EntityRendererRegistry.register(com.stormreach.labyrinth.LabyrinthRegistry.LOCKER_SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
        EntityRendererRegistry.register(com.stormreach.tornado.TornadoTypes.FIRE_WHIRL, com.stormreach.client.tornado.FireWhirlRenderer::new);
        EntityRendererRegistry.register(com.stormreach.weather.precip.PrecipContent.HAILSTONE, com.stormreach.client.weather.HailstoneRenderer::new);
        EntityRendererRegistry.register(com.stormreach.weather.precip.PrecipContent.STORM_CELL, com.stormreach.client.weather.StormCellRenderer::new);
        EntityRendererRegistry.register(com.stormreach.labyrinth.LabyrinthRegistry.HOLLOW_KEEPER, com.stormreach.client.story.StoryRenderers.KeeperRenderer::new);
        EntityRendererRegistry.register(com.stormreach.story.StoryRegistry.STRANGER, com.stormreach.client.story.StoryRenderers.StrangerRenderer::new);
        EntityRendererRegistry.register(com.stormreach.story.StoryRegistry.CYRRUS, com.stormreach.client.story.StoryRenderers.CyrrusRenderer::new);
        EntityRendererRegistry.register(com.stormreach.forest.ForestRegistry.GLIMMERSTAG, com.stormreach.client.forest.ForestModels.StagRenderer::new);
        EntityRendererRegistry.register(com.stormreach.forest.ForestRegistry.THISTLEHOP, com.stormreach.client.forest.ForestModels.HopRenderer::new);
        // 5.2 Stormwild
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.CLOUD_WHALE, com.stormreach.client.wild.WildRenderers.CloudWhaleRenderer::new);
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.SHOCK_EEL, com.stormreach.client.wild.WildRenderers.ShockEelRenderer::new);
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.SPARKSKINK, com.stormreach.client.wild.WildRenderers.SparkskinkRenderer::new);
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.STORM_JELLY, com.stormreach.client.wild.WildRenderers.StormJellyRenderer::new);
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.GALE_SPRITE, com.stormreach.client.wild.WildRenderers.GaleSpriteRenderer::new);
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.TIDEWRACK_HERMIT, com.stormreach.client.wild.WildRenderers.HermitRenderer::new);
        EntityRendererRegistry.register(com.stormreach.wild.WildRegistry.THUNDERBIRD, com.stormreach.client.wild.WildRenderers.ThunderbirdRenderer::new);
        EntityRendererRegistry.register(com.stormreach.chase.ChaseRegistry.INTERCEPT_VEHICLE_ENTITY, com.stormreach.client.wild.WildRenderers.VehicleRenderer::new);
        EntityRendererRegistry.register(com.stormreach.chase.ChaseRegistry.WEATHER_BALLOON_ENTITY, com.stormreach.client.wild.WildRenderers.BalloonRenderer::new);
        com.stormreach.client.backpack.BackpackClient.init();
        com.stormreach.client.labyrinth.LockerClient.init();
        com.stormreach.client.tornado.TornadoSeeThrough.init();
        com.stormreach.client.weather.WeatherScreen.registerReceiver();
        com.stormreach.client.render.PlacementHologram.init();   // 6.0: replaces the particle outline (RadarPlacementOutline)
        com.stormreach.client.ClientWeather.init();
        com.stormreach.client.TivKeys.init();
        com.stormreach.client.weather.HailClient.init();
        com.stormreach.client.hud.StormHudPanel.init();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.stormreach.structure.StructureRegistry.INSTRUMENT_ENTITY, com.stormreach.client.render.InstrumentMachineRenderer::new);
        com.stormreach.client.guide.GuidebookScreen.registerOpener();
        com.stormreach.client.particle.StormParticle.register();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.stormreach.tornado.radar.RadarContent.RADAR_ENTITY,
                com.stormreach.client.weather.DopplerRadarRenderer::new);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.stormreach.client.story.LabyrinthMusic::tick);
        com.stormreach.client.sky.SkyRenderers.register();
        com.stormreach.client.sky.FrontRenderers.register();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.stormreach.power.PowerRegistry.WATER_WHEEL_ENTITY, com.stormreach.client.sky.WaterWheelRenderer::new);
        com.stormreach.client.tooltip.StormTooltips.register();
        StormClientContent.init();
    }
}
