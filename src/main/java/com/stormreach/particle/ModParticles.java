package com.stormreach.particle;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Stormreach's own particles (5.1), each with its own textures and motion (see the client's StormParticle):
 * storm sparks, rain splashes, hail shards, debris dust, wind streaks and vault dust.
 */
public final class ModParticles {
    private ModParticles() {}

    public static final SimpleParticleType STORM_SPARK = register("storm_spark");
    public static final SimpleParticleType RAIN_SPLASH = register("rain_splash");
    public static final SimpleParticleType HAIL_SHARD = register("hail_shard");
    public static final SimpleParticleType DEBRIS_DUST = register("debris_dust");
    public static final SimpleParticleType WIND_STREAK = register("wind_streak");
    public static final SimpleParticleType VAULT_DUST = register("vault_dust");

    private static SimpleParticleType register(String name) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Stormreach.id(name), FabricParticleTypes.simple());
    }

    public static void init() {}
}
