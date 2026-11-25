package com.stormreach.realm;

import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * The Stormgate: a flat 5x5 ring of 16 Stormgate Frames around a 3x3 pool of stormwater sources.
 * With enough Tempest Eyes set in the ring, a lightning strike on the pool tears it open.
 */
public final class Stormgate {
    private Stormgate() {}

    /** What a gate centred at {@code center} looks like right now. */
    public record Status(BlockPos center, int frames, int eyes, int pool, int portal) {
        public boolean ringComplete() { return frames == 16; }
        public boolean poolReady() { return pool == 9; }
        public boolean open() { return portal == 9; }
        public boolean ready() { return ringComplete() && poolReady() && eyes >= StormreachConfig.eyesRequired; }
    }

    public static Status check(ServerLevel level, BlockPos center) {
        int frames = 0, eyes = 0, pool = 0, portal = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = center.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    if (s.is(RealmBlocks.STORMGATE_FRAME)) {
                        frames++;
                        if (s.getValue(StormgateFrameBlock.EYE)) eyes++;
                    }
                } else if (s.is(RealmBlocks.STORMGATE)) {
                    portal++;
                } else if (isStormwaterSource(level.getFluidState(p))) {
                    pool++;
                }
            }
        }
        return new Status(center, frames, eyes, pool, portal);
    }

    private static boolean isStormwaterSource(FluidState fluid) {
        return fluid.isSource() && fluid.getType().isSame(ModFluids.STORMWATER);
    }

    /** The best-formed gate whose ring contains {@code framePos}. */
    public static Status around(ServerLevel level, BlockPos framePos) {
        Status best = null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
                Status s = check(level, framePos.offset(-dx, 0, -dz));
                if (best == null || score(s) > score(best)) best = s;
            }
        }
        return best;
    }

    private static int score(Status s) {
        return s.frames() * 4 + s.pool() * 2 + s.eyes() + s.portal() * 3;
    }

    /** Tells a player how far along the gate around this frame is, and what to do next. */
    public static void describe(ServerLevel level, BlockPos framePos, Player player) {
        Status s = around(level, framePos);
        int need = StormreachConfig.eyesRequired;
        String key;
        if (s.open()) key = "open";
        else if (!s.ringComplete()) key = "ring";
        else if (s.eyes() < need) key = "eyes";
        else if (!s.poolReady()) key = "pool";
        else key = "ready";
        player.sendOverlayMessage(StormgateFrameBlock.hint(key, s.frames(), s.eyes(), need, s.pool()));
    }

    /** Called for every lightning strike. Opens any finished gate whose pool was hit. */
    public static void onLightning(ServerLevel level, BlockPos hit) {
        for (BlockPos p : BlockPos.betweenClosed(hit.offset(-1, -1, -1), hit.offset(1, 1, 1))) {
            if (!isStormwaterSource(level.getFluidState(p))) continue;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Status s = check(level, p.offset(dx, 0, dz));
                    if (s.ready()) {
                        open(level, s.center());
                        return;
                    }
                }
            }
        }
    }

    public static void open(ServerLevel level, BlockPos center) {
        BlockState portal = RealmBlocks.STORMGATE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(center.offset(dx, 0, dz), portal, Block.UPDATE_CLIENTS);
            }
        }
        level.playSound(null, center, com.stormreach.registry.ModSfx.MAGIC_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 200, 1.5, 0.5, 1.5, 0.5);
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(center)) < 48 * 48)) {
            p.sendSystemMessage(StormgateFrameBlock.hint("opened"));
        }
    }

    /** Builds a ready, open gate on a small platform (used for the return gate in the Tempest Realm). */
    public static void build(ServerLevel level, BlockPos center) {
        BlockState floor = RealmBlocks.SKYSHALE_BRICKS.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos below = center.offset(dx, -1, dz);
                if (!level.getBlockState(below).isSolidRender()) level.setBlock(below, floor, Block.UPDATE_ALL);
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos air = center.offset(dx, dy, dz);
                    if (!level.getBlockState(air).isAir()) level.setBlock(air, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockPos at = center.offset(dx, 0, dz);
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean inside = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                if (ring && Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                    level.setBlock(at, RealmBlocks.STORMGATE_FRAME.defaultBlockState().setValue(StormgateFrameBlock.EYE, true), Block.UPDATE_ALL);
                } else if (!inside) {
                    level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        open(level, center);
    }
}
