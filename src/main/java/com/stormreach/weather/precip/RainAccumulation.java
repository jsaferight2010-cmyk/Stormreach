package com.stormreach.weather.precip;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.Rainfall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Rain that builds up. Each level tracks how long it has rained without a break; puddles then appear on open,
 * level ground around players, faster in a downpour and the longer the rain lasts, and grow bigger while it keeps
 * raining. Hollows (ground lower than its neighbours) fill first. When the rain stops the puddles dry up on their own.
 */
public final class RainAccumulation {
    private static final Map<ResourceKey<Level>, Long> RAIN_TICKS = new HashMap<>();

    private RainAccumulation() {}

    public static long rainTicks(ServerLevel level) {
        return RAIN_TICKS.getOrDefault(level.dimension(), 0L);
    }

    /** Current wetness of the ground in this level, 0..1. */
    public static double wetness(ServerLevel level) {
        return Rainfall.wetness(Rainfall.intensity(level.isRaining(), level.isThundering()), rainTicks(level));
    }

    public static void tick(ServerLevel level) {
        if (!level.isRaining()) {
            RAIN_TICKS.remove(level.dimension());
            return;
        }
        RAIN_TICKS.merge(level.dimension(), 1L, Long::sum);
        if (!StormreachConfig.puddles) return;
        double wet = wetness(level);
        if (wet <= 0) return;
        RandomSource r = level.getRandom();
        int max = Rainfall.maxPuddleSize(wet);
        for (ServerPlayer p : level.players()) {
            double attempts = Rainfall.puddleAttempts(wet);
            while (attempts > 0) {
                if (attempts < 1 && r.nextDouble() > attempts) break;
                attempts -= 1;
                tryPuddle(level, p.getBlockX() + r.nextInt(65) - 32, p.getBlockZ() + r.nextInt(65) - 32, max, r);
            }
        }
    }

    static void tryPuddle(ServerLevel level, int x, int z, int max, RandomSource r) {
        if (!level.hasChunk(x >> 4, z >> 4)) return;   // 6.0: before the heightmap read, which would load the chunk
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState here = level.getBlockState(pos);
        if (!(here.isAir() || here.is(PrecipContent.PUDDLE))) return;
        if (!level.isRainingAt(pos)) return; // snow, deserts, roofs
        Biome biome = level.getBiome(pos).value();
        if (biome.coldEnoughToSnow(pos, level.getSeaLevel())) return;
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(below).isEmpty()) return;
        // level or low ground holds water; on a slope it runs off
        int higher = 0, lower = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!level.hasChunk((x + d.getStepX()) >> 4, (z + d.getStepZ()) >> 4)) return;
            int ny = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + d.getStepX(), z + d.getStepZ());
            if (ny > y) higher++;
            if (ny < y) lower++;
        }
        if (lower > 1) return;
        if (higher == 0 && lower == 1 && r.nextInt(3) != 0) return;
        PuddleBlock.grow(level, pos, higher >= 2 ? Math.min(3, max + 1) : max);
    }
}
