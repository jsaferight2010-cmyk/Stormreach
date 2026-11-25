package com.stormreach.front;

import com.stormreach.StormreachConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * What a coming storm, and a hot clear day, bring: Storm Petrels gathering over the sea while rain is due or falling,
 * and Dust Devils on clear afternoons in hot, dry country.
 */
public final class FrontArrivals {
    private FrontArrivals() {}

    /** True when it isn't raining yet but rain is due within about four minutes. */
    public static boolean rainSoon(ServerLevel level) {
        if (level.isRaining() || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        var w = level.getServer().getWeatherData();
        return w.getClearWeatherTime() <= 0 && w.getRainTime() > 0 && w.getRainTime() < 4800;
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null || !StormreachConfig.wildlifeSpawning) return;
        long t = level.getGameTime();
        if (t % 600 == 220) petrels(level);
        if (t % 600 == 470) dustDevils(level);
    }

    private static void petrels(ServerLevel level) {
        if (!level.isRaining() && !rainSoon(level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || level.getRandom().nextInt(2) == 0) continue;
            if (!level.getBiome(p.blockPosition()).is(BiomeTags.IS_OCEAN) && !level.getBiome(p.blockPosition()).is(BiomeTags.IS_BEACH)) continue;
            if (level.getEntitiesOfClass(StormPetrel.class, p.getBoundingBox().inflate(64)).size() >= 8) continue;
            for (int tries = 0; tries < 6; tries++) {
                double a = level.getRandom().nextDouble() * Math.PI * 2, r = 12 + level.getRandom().nextDouble() * 16;
                BlockPos col = BlockPos.containing(p.getX() + Math.cos(a) * r, 0, p.getZ() + Math.sin(a) * r);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
                if (!level.getFluidState(top.below()).is(FluidTags.WATER)) continue;
                int n = 3 + level.getRandom().nextInt(3);
                for (int i = 0; i < n; i++) {
                    StormPetrel b = FrontRegistry.STORM_PETREL.create(level, EntitySpawnReason.EVENT);
                    if (b == null) continue;
                    b.snapTo(top.getX() + level.getRandom().nextGaussian() * 2, top.getY() + 1 + level.getRandom().nextDouble() * 2,
                            top.getZ() + level.getRandom().nextGaussian() * 2, level.getRandom().nextFloat() * 360, 0);
                    level.addFreshEntity(b);
                }
                break;
            }
        }
    }

    private static void dustDevils(ServerLevel level) {
        if (level.isRaining() || !level.isBrightOutside()) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || level.getRandom().nextInt(4) != 0) continue;
            var biome = level.getBiome(p.blockPosition());
            if (biome.value().getBaseTemperature() < 1.0f || biome.value().hasPrecipitation()) continue;
            if (!level.getEntitiesOfClass(DustDevil.class, p.getBoundingBox().inflate(80)).isEmpty()) continue;
            double a = level.getRandom().nextDouble() * Math.PI * 2, r = 20 + level.getRandom().nextDouble() * 20;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(p.getX() + Math.cos(a) * r, 0, p.getZ() + Math.sin(a) * r));
            if (!level.getFluidState(top.below()).isEmpty() || !level.canSeeSky(top)) continue;
            DustDevil d = FrontRegistry.DUST_DEVIL.create(level, EntitySpawnReason.EVENT);
            if (d == null) continue;
            d.snapTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
            level.addFreshEntity(d);
        }
    }
}
