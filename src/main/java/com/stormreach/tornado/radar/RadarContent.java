package com.stormreach.tornado.radar;

import com.stormreach.Stormreach;
import com.stormreach.tornado.WeatherItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Set;

/** Registration for the Doppler radar structure. */
public final class RadarContent {
    private RadarContent() {}

    public static final Block RADAR_PART;
    public static final BlockEntityType<DopplerRadarBlockEntity> RADAR_ENTITY;

    static {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id("radar_part"));
        RADAR_PART = Registry.register(BuiltInRegistries.BLOCK, key, new RadarPartBlock(BlockBehaviour.Properties.of().setId(key)
                .mapColor(MapColor.METAL).strength(2.5f, 6.0f).sound(SoundType.COPPER).noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE)));
        RADAR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Stormreach.id("doppler_radar"),
                new BlockEntityType<>(DopplerRadarBlockEntity::new, Set.of(WeatherItems.TORNADO_RADAR)));
    }

    public static void init() {}
}
