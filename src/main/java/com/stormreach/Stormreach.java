package com.stormreach;

import com.stormreach.command.StormreachCommand;
import com.stormreach.registry.ModBlocks;
import com.stormreach.registry.ModEffects;
import com.stormreach.weather.StormWeather;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Stormreach implements ModInitializer {
    public static final String MOD_ID = "stormreach";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        StormreachConfig.load();

        ModBlocks.init();
        ModEffects.init();
        ModEntities.init();
        ModItems.init();
        com.stormreach.registry.ModFluids.init();
        com.stormreach.realm.RealmBlocks.init();
        com.stormreach.realm.RealmEntities.init();
        com.stormreach.realm.RealmItems.init();
        com.stormreach.registry.ModSounds.init();
        com.stormreach.registry.ModSfx.init();
        com.stormreach.registry.ModSoundTypes.init();
        com.stormreach.particle.ModParticles.init();
        com.stormreach.tornado.TornadoTypes.init();
        com.stormreach.tornado.WeatherItems.init();
        com.stormreach.structure.StructureRegistry.init();
        com.stormreach.weather.WeatherHud.init();
        com.stormreach.chase.TivControls.init();
        com.stormreach.weather.precip.HailSim.init();
        com.stormreach.weather.precip.PrecipContent.init();
        com.stormreach.guide.Guidebooks.init();
        com.stormreach.realm.SovereignArena.init();
        com.stormreach.backpack.Backpacks.init();
        com.stormreach.units.PlayerUnits.init();
        com.stormreach.wind.WindRegistry.init();
        com.stormreach.blight.HeartRegistry.init();
        com.stormreach.labyrinth.LabyrinthRegistry.init();
        com.stormreach.forest.ForestRegistry.init();
        com.stormreach.wild.WildRegistry.init();
        com.stormreach.chase.ChaseRegistry.init();
        com.stormreach.sky.SkyRegistry.init();
        com.stormreach.front.FrontRegistry.init();
        com.stormreach.power.PowerRegistry.init();
        com.stormreach.story.StoryRegistry.init();
        com.stormreach.storm.StormBlocks.registerAll();
        com.stormreach.storm.StormMobs.registerAll();


        TabOrganizer.register();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("storm_blocks"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.stormreach.storm_blocks"))
                        .icon(() -> new ItemStack(com.stormreach.registry.ModFluids.STORMWATER_BUCKET))
                        .displayItems((params, output) -> {
                            // the Stormheart starts the Stormblight that turns blocks into these, so it leads the tab
                            output.accept(com.stormreach.registry.ModBlocks.STORMHEART);
                            for (Item item : com.stormreach.storm.StormBlocks.ITEMS) output.accept(item);
                        })
                        .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("storm_mobs"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.stormreach.storm_mobs"))
                        .icon(() -> new ItemStack(ModItems.STORMWARDEN_SPAWN_EGG))
                        .displayItems((params, output) -> {
                            for (Item item : com.stormreach.storm.StormMobs.EGGS) output.accept(item);
                        })
                        .build());


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                StormreachCommand.register(dispatcher));

        // Full Stormshard armor or the Grounded effect makes you immune to lightning.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(source.is(DamageTypeTags.IS_LIGHTNING) && (StormWeather.isLightningProof(entity)
                        || com.stormreach.storm.StormMobs.isStorm(entity.getType()))));

        StormreachFishing.register();

        LOGGER.info("Stormreach loaded: the coast is getting stormy.");
    }
}
