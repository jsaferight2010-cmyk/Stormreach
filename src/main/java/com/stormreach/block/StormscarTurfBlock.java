package com.stormreach.block;

import com.stormreach.StormreachConfig;
import com.stormreach.blight.Stormblight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stormscar Turf: grass scorched grey-blue by the Stormblight. Plants grow on it like grass.
 * In UNLIMITED spread mode it keeps converting the land around it on its own.
 * Smothered under a solid block, it dies back to dirt.
 */
public class StormscarTurfBlock extends Block {
    public StormscarTurfBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isSolidRender()) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            return;
        }
        if (StormreachConfig.spreadMode != StormreachConfig.SpreadMode.UNLIMITED) return;
        for (int i = 0; i < 2; i++) {
            BlockPos target = pos.offset(random.nextInt(5) - 2, random.nextInt(5) - 3, random.nextInt(5) - 2);
            if (level.isLoaded(target)) Stormblight.convertBlock(level, target, random);
        }
    }
}
