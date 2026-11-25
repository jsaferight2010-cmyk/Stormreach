package com.stormreach.labyrinth;

import com.stormreach.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Vault Door: a 5 x 5 wall of unbreakable vault plating at the heart of the Labyrinth with its three locks built
 * in. When all three are set it sinks into the floor one row at a time, with dust, grinding and a deep boom.
 */
public final class VaultDoor {
    private VaultDoor() {}

    /** How far from a lock to look for the rest of its door. */
    static final int REACH = 4;
    private static final List<Opening> OPENING = new ArrayList<>();

    private record Opening(ServerLevel level, List<BlockPos> blocks, int topY, int bottomY, int[] step) {}

    /** Door blocks (plating and locks) connected to this lock. */
    static List<BlockPos> doorBlocks(ServerLevel level, BlockPos lock) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(lock.offset(-REACH, -REACH, -REACH), lock.offset(REACH, REACH, REACH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(LabyrinthRegistry.VAULT_DOOR) || s.is(LabyrinthRegistry.KEYHOLE)) out.add(p.immutable());
        }
        return out;
    }

    /** Number of this door's locks that hold their key (0..3). */
    public static int locksSet(ServerLevel level, BlockPos lock) {
        int set = 0;
        for (BlockPos p : doorBlocks(level, lock)) {
            BlockState s = level.getBlockState(p);
            if (s.is(LabyrinthRegistry.KEYHOLE) && s.getValue(KeyholeBlock.FILLED)) set++;
        }
        return set;
    }

    /** Starts opening the door if all three of its locks are set. Returns true if it started. */
    public static boolean open(ServerLevel level, BlockPos lock) {
        List<BlockPos> door = doorBlocks(level, lock);
        int locks = 0, filled = 0, top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
        for (BlockPos p : door) {
            BlockState s = level.getBlockState(p);
            if (s.is(LabyrinthRegistry.KEYHOLE)) { locks++; if (s.getValue(KeyholeBlock.FILLED)) filled++; }
            top = Math.max(top, p.getY());
            bottom = Math.min(bottom, p.getY());
        }
        if (locks != 3 || filled != 3) return false;
        OPENING.add(new Opening(level, door, top, bottom, new int[]{0}));
        level.playSound(null, lock, SoundEvents.WARDEN_EMERGE, SoundSource.BLOCKS, 2.5f, 0.6f);
        return true;
    }

    /** Runs from the level tick: lowers opening doors a row every 10 ticks. */
    public static void tick(ServerLevel level) {
        for (Iterator<Opening> it = OPENING.iterator(); it.hasNext(); ) {
            Opening o = it.next();
            if (o.level() != level) continue;
            int step = o.step()[0]++;
            if (step % 10 != 0) continue;
            int row = o.topY() - step / 10;
            if (row < o.bottomY()) { it.remove(); continue; }
            for (BlockPos p : o.blocks()) {
                if (p.getY() != row) continue;
                BlockState s = level.getBlockState(p);
                if (!s.is(LabyrinthRegistry.VAULT_DOOR) && !s.is(LabyrinthRegistry.KEYHOLE)) continue;
                level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LabyrinthRegistry.VAULT_DOOR.defaultBlockState()),
                        p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);
            }
            BlockPos mid = o.blocks().get(o.blocks().size() / 2);
            level.playSound(null, mid, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 2f, 0.4f);
            if (row == o.bottomY()) level.playSound(null, mid, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5f, 0.5f);
        }
    }

    /** True while a door is still sinking (for tests). */
    public static boolean anyOpening() { return !OPENING.isEmpty(); }
}
