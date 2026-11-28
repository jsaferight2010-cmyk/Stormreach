package com.stormreach.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A tall machine built from one controller block (the one you place, the one with the block entity) and a column of
 * invisible {@link MachinePartBlock}s above it that give the visible model its collision and let you click or break
 * it anywhere. Placement is refused while anything solid is in the way; breaking any part takes the whole machine
 * down and drops the item once; a part whose controller is gone removes itself.
 */
public interface Multiblock {
    /** Height of the part column above the controller (0 = no parts). */
    int partHeight(BlockState controller);

    /** Collision shape of the part at a given height above the controller. */
    default MachinePartBlock.Shape partShape(BlockState controller, int dy) { return MachinePartBlock.Shape.POLE; }

    static List<BlockPos> footprint(BlockPos controller, int height) {
        List<BlockPos> out = new ArrayList<>();
        for (int dy = 0; dy <= height; dy++) out.add(controller.above(dy));
        return out;
    }

    /** Cells that stop the machine being placed (empty = it fits). The controller cell itself is checked by vanilla. */
    static List<BlockPos> blocked(Level level, BlockPos controller, int height) {
        List<BlockPos> out = new ArrayList<>();
        for (int dy = 1; dy <= height; dy++) {
            BlockPos p = controller.above(dy);
            if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced()) out.add(p);
        }
        return out;
    }

    static void buildParts(Level level, BlockPos controller, BlockState cs, Multiblock m) {
        int h = m.partHeight(cs);
        for (int dy = 1; dy <= h; dy++)
            level.setBlock(controller.above(dy), MachinePartBlock.state(dy, m.partShape(cs, dy)), 2 | 16);
    }

    static void removeParts(Level level, BlockPos controller, int height) {
        for (int dy = 1; dy <= height; dy++) {
            BlockPos p = controller.above(dy);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof MachinePartBlock && s.getValue(MachinePartBlock.DY) == dy) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }
}
