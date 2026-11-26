package com.stormreach.signs;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.StormSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 6.0 stage 10: freezing rain. Rain that falls where the biome is only just above freezing (taiga, stony shores, windswept
 * hills) freezes on contact at night and in Thunderheads, leaving a slippery glaze on the ground, and players caught
 * in it start to feel the cold (the frost vignette, never enough to hurt). Glaze melts again by day.
 */
public final class FreezingRain {
    private FreezingRain() {}

    private static final int TRIES_PER_PLAYER = 3, RANGE = 40;

    /** True if freezing rain is falling on this open spot right now (the spot must be loaded and open to the sky). */
    public static boolean fallingAt(ServerLevel level, BlockPos pos) {
        if (!StormreachConfig.freezingRain || level.dimension() != Level.OVERWORLD || !level.isRaining()) return false;
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !level.isRainingAt(pos)) return false;
        return StormSigns.freezingRain(true, level.getBiome(pos).value().getBaseTemperature(), level.isDarkOutside(), level.isThundering());
    }

    public static void tick(ServerLevel level) {
        if (!StormreachConfig.freezingRain || level.dimension() != Level.OVERWORLD || !level.isRaining() || level.getGameTime() % 10 != 3) return;
        RandomSource random = level.getRandom();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            BlockPos at = p.blockPosition();
            if (p.canFreeze() && fallingAt(level, at.above())) p.setTicksFrozen(Math.max(p.getTicksFrozen(), 70));
            for (int i = 0; i < TRIES_PER_PLAYER; i++) {
                int x = at.getX() + random.nextInt(RANGE * 2 + 1) - RANGE, z = at.getZ() + random.nextInt(RANGE * 2 + 1) - RANGE;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                glaze(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)));
            }
        }
    }

    /** Glazes the open spot {@code top} (the air above the ground) if freezing rain is falling there. */
    public static boolean glaze(ServerLevel level, BlockPos top) {
        if (!level.getBlockState(top).isAir() || !fallingAt(level, top)) return false;
        BlockState glaze = SignsRegistry.GLAZE.defaultBlockState();
        if (!glaze.canSurvive(level, top)) return false;
        return level.setBlock(top, glaze, net.minecraft.world.level.block.Block.UPDATE_ALL);
    }
}
