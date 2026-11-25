package com.stormreach.structure;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** 6.0: the shared invisible part block for tall machines (turbine tower, siren pole, weather station mast). */
public final class StructureRegistry {
    private StructureRegistry() {}

    public static final Block MACHINE_PART;

    static {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id("machine_part"));
        MACHINE_PART = Registry.register(BuiltInRegistries.BLOCK, key, new MachinePartBlock(BlockBehaviour.Properties.of().setId(key)
                .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE)));
    }

    /** Block entity of the siren and weather station (they need a renderer for their whole structure). */
    public static final net.minecraft.world.level.block.entity.BlockEntityType<com.stormreach.tornado.InstrumentBlockEntity> INSTRUMENT_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Stormreach.id("instrument_machine"),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(com.stormreach.tornado.InstrumentBlockEntity::new,
                            java.util.Set.of(com.stormreach.tornado.WeatherItems.TORNADO_SIREN, com.stormreach.tornado.WeatherItems.WEATHER_STATION)));

    public static void init() {}
}
