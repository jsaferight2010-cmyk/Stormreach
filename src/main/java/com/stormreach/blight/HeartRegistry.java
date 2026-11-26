package com.stormreach.blight;

import com.stormreach.Stormreach;
import com.stormreach.registry.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** 6.0: the Stormheart's block entity (its powered frontier). */
public final class HeartRegistry {
    private HeartRegistry() {}

    public static BlockEntityType<StormheartBlockEntity> HEART_ENTITY;

    public static void init() {
        HEART_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Stormreach.id("stormheart"),
                new BlockEntityType<>(StormheartBlockEntity::new, java.util.Set.of(ModBlocks.STORMHEART)));
    }
}
