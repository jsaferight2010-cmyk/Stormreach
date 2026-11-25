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
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.SKYRAY, com.stormreach.client.render.RealmRenderers.SkyrayRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.PUFFLING, com.stormreach.client.render.RealmRenderers.PufflingRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.STATIC_WISP, com.stormreach.client.render.RealmRenderers.StaticWispRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.CYCLONE, com.stormreach.client.render.RealmRenderers.CycloneRenderer::new);
        EntityRendererRegistry.register(com.stormreach.realm.RealmEntities.STORMHAWK, com.stormreach.client.render.RealmRenderers.StormhawkRenderer::new);
        EntityRendererRegistry.register(com.stormreach.tornado.TornadoTypes.TORNADO, com.stormreach.client.tornado.TornadoRenderer::new);
        EntityRendererRegistry.register(com.stormreach.weather.precip.PrecipContent.HAILSTONE, com.stormreach.client.weather.HailstoneRenderer::new);
        EntityRendererRegistry.register(com.stormreach.weather.precip.PrecipContent.STORM_CELL, com.stormreach.client.weather.StormCellRenderer::new);
        EntityRendererRegistry.register(com.stormreach.labyrinth.LabyrinthRegistry.HOLLOW_KEEPER, com.stormreach.client.story.StoryRenderers.KeeperRenderer::new);
        EntityRendererRegistry.register(com.stormreach.story.StoryRegistry.STRANGER, com.stormreach.client.story.StoryRenderers.StrangerRenderer::new);
        EntityRendererRegistry.register(com.stormreach.story.StoryRegistry.CYRRUS, com.stormreach.client.story.StoryRenderers.CyrrusRenderer::new);
        EntityRendererRegistry.register(com.stormreach.forest.ForestRegistry.GLIMMERSTAG, com.stormreach.client.forest.ForestModels.StagRenderer::new);
        EntityRendererRegistry.register(com.stormreach.forest.ForestRegistry.THISTLEHOP, com.stormreach.client.forest.ForestModels.HopRenderer::new);
        com.stormreach.client.backpack.BackpackClient.init();
        com.stormreach.client.labyrinth.LockerClient.init();
        com.stormreach.client.tornado.TornadoSeeThrough.init();
        com.stormreach.client.weather.WeatherScreen.registerReceiver();
        com.stormreach.client.weather.RadarPlacementOutline.init();
        com.stormreach.client.guide.GuidebookScreen.registerOpener();
        com.stormreach.client.particle.StormParticle.register();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.stormreach.tornado.radar.RadarContent.RADAR_ENTITY,
                com.stormreach.client.weather.DopplerRadarRenderer::new);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.stormreach.client.story.LabyrinthMusic::tick);
        StormClientContent.init();
    }
}
