package com.stormreach.signs;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.StormSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 6.0 stage 10: storms feed the fields. Crops open to the rain get extra growth ticks while it rains (about twice as
 * fast), and lightning fixes nitrogen: crops within 4 blocks of a strike may get a burst of growth, as if bone-mealed.
 */
public final class StormCrops {
    private StormCrops() {}

    public static boolean isCrop(BlockState state) {
        return state.is(BlockTags.CROPS) && state.getBlock() instanceof BonemealableBlock;
    }

    public static void tick(ServerLevel level) {
        if (!StormreachConfig.rainFedCrops || level.dimension() != Level.OVERWORLD || !level.isRaining() || level.getGameTime() % 20 != 11) return;
        RandomSource random = level.getRandom();
        int r = StormSigns.RAIN_RANGE;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            BlockPos at = p.blockPosition();
            for (int i = 0; i < StormSigns.RAIN_SAMPLES; i++) {
                int x = at.getX() + random.nextInt(r * 2 + 1) - r, z = at.getZ() + random.nextInt(r * 2 + 1) - r;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                BlockState crop = level.getBlockState(top);
                if (!isCrop(crop)) { top = top.below(); crop = level.getBlockState(top); }   // tall crops that block motion
                if (isCrop(crop) && crop.isRandomlyTicking() && level.isRainingAt(top.above())) crop.randomTick(level, top, random);
            }
        }
    }

    /** Called for every lightning strike. */
    public static void onLightning(ServerLevel level, BlockPos hit) {
        if (!StormreachConfig.lightningFertilizes) return;
        RandomSource random = level.getRandom();
        int rad = StormSigns.LIGHTNING_FERTILE_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(hit.offset(-rad, -2, -rad), hit.offset(rad, 2, rad))) {
            if (!level.isLoaded(p)) continue;
            BlockState state = level.getBlockState(p);
            if (!isCrop(state)) continue;
            double d = Math.sqrt(p.distSqr(hit));
            if (random.nextDouble() >= StormSigns.fertilizeChance(d)) continue;
            BonemealableBlock crop = (BonemealableBlock) state.getBlock();
            if (!crop.isValidBonemealTarget(level, p, state, net.minecraft.world.level.block.BonemealSource.MOB)) continue;
            crop.performBonemeal(level, random, p, state, net.minecraft.world.level.block.BonemealSource.MOB);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
        }
    }
}
