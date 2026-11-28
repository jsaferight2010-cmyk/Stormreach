package com.stormreach.sky;

import com.stormreach.StormreachConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.WeatherData;

/**
 * What storms bring with them in the Overworld: Hailstone Golems forming in thunderstorms (hailGolemChance, percent per
 * minute for each player out under the sky), and swarms of Stormflies in the minutes before a storm and while it lasts.
 */
public final class StormArrivals {
    private StormArrivals() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null || !StormreachConfig.wildlifeSpawning) return;
        long t = level.getGameTime();
        if (t % 1200 == 300 && level.isThundering() && StormreachConfig.hailGolemChance > 0) golems(level);
        if (t % 600 == 150) stormflies(level, server.getWeatherData());
    }

    private static BlockPos surfaceNear(ServerLevel level, ServerPlayer p, double min, double max) {
        double a = level.getRandom().nextDouble() * Math.PI * 2, r = min + level.getRandom().nextDouble() * (max - min);
        BlockPos col = BlockPos.containing(p.getX() + Math.cos(a) * r, 0, p.getZ() + Math.sin(a) * r);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
        return level.getFluidState(top.below()).isEmpty() && level.getFluidState(top).isEmpty() ? top : null;
    }

    private static void golems(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || !level.canSeeSky(p.blockPosition())) continue;
            if (level.getRandom().nextInt(100) >= StormreachConfig.hailGolemChance) continue;
            if (level.getEntitiesOfClass(HailstoneGolem.class, p.getBoundingBox().inflate(64)).size() >= 2) continue;
            BlockPos at = surfaceNear(level, p, 18, 30);
            if (at == null) continue;
            HailstoneGolem g = SkyRegistry.HAILSTONE_GOLEM.create(level, EntitySpawnReason.EVENT);
            if (g == null) continue;
            g.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            if (level.noCollision(g)) level.addFreshEntity(g);
        }
    }

    private static void stormflies(ServerLevel level, WeatherData w) {
        boolean due = !level.isRaining() && w.getClearWeatherTime() == 0 && w.getRainTime() < 3600;
        if (!due && !level.isThundering()) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || level.getRandom().nextInt(2) == 0) continue;
            if (level.getEntitiesOfClass(Stormfly.class, p.getBoundingBox().inflate(48)).size() > 12) continue;
            BlockPos at = surfaceNear(level, p, 8, 18);
            if (at == null) continue;
            int n = 6 + level.getRandom().nextInt(6);
            for (int i = 0; i < n; i++) {
                Stormfly f = SkyRegistry.STORMFLY.create(level, EntitySpawnReason.EVENT);
                if (f == null) continue;
                f.snapTo(at.getX() + level.getRandom().nextGaussian() * 2, at.getY() + 1 + level.getRandom().nextDouble() * 2,
                        at.getZ() + level.getRandom().nextGaussian() * 2, 0f, 0f);
                level.addFreshEntity(f);
            }
        }
    }
}
