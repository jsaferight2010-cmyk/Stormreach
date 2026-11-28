package com.stormreach.signs;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** 6.0 stage 10: the signs of the storm (Weatherglass, glaze from freezing rain, St. Elmo's fire). */
public final class SignsRegistry {
    private SignsRegistry() {}

    public static final Block WEATHERGLASS, GLAZE;
    public static final Item WEATHERGLASS_ITEM;

    static {
        ResourceKey<Block> gk = ResourceKey.create(Registries.BLOCK, Stormreach.id("weatherglass"));
        WEATHERGLASS = Registry.register(BuiltInRegistries.BLOCK, gk, new WeatherglassBlock(BlockBehaviour.Properties.of().setId(gk)
                .mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED)
                .lightLevel(s -> s.getValue(WeatherglassBlock.READING) >= 15 ? 5 : 0)));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id("weatherglass"));
        WEATHERGLASS_ITEM = Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(WEATHERGLASS, new Item.Properties().setId(ik).useBlockDescriptionPrefix()));

        ResourceKey<Block> zk = ResourceKey.create(Registries.BLOCK, Stormreach.id("glaze"));
        GLAZE = Registry.register(BuiltInRegistries.BLOCK, zk, new GlazeBlock(BlockBehaviour.Properties.of().setId(zk)
                .mapColor(MapColor.ICE).strength(0.1f).friction(0.98f).sound(SoundType.GLASS).noOcclusion().replaceable()
                .noLootTable().pushReaction(PushReaction.POPPED).isValidSpawn((s, l, p, t) -> false)));
    }

    public static void init() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> ElmosFire.resetForNewWorld());
    }

    /** Called from the server level tick. */
    public static void tick(net.minecraft.server.level.ServerLevel level) {
        FreezingRain.tick(level);
        ElmosFire.tick(level);
        StormCrops.tick(level);
    }
}
