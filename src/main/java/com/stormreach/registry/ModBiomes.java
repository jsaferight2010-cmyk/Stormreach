package com.stormreach.registry;

import com.stormreach.Stormreach;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** The biomes themselves are data-driven: data/stormreach/worldgen/biome/*.json */
public final class ModBiomes {
    public static final ResourceKey<Biome> STORMREACH_COAST = key("stormreach_coast");
    public static final ResourceKey<Biome> SQUALLWIND_BLUFFS = key("squallwind_bluffs");
    public static final ResourceKey<Biome> THUNDERSPIRE_HIGHLANDS = key("thunderspire_highlands");
    public static final ResourceKey<Biome> BRINEWATER_MARSH = key("brinewater_marsh");
    /** Nether and End biomes the Stormblight paints. */
    public static final ResourceKey<Biome> STORM_WASTES = key("storm_wastes");
    public static final ResourceKey<Biome> STORM_VOID = key("storm_void");

    private ModBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Stormreach.id(name));
    }
}
