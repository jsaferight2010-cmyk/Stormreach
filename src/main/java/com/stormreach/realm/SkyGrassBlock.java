package com.stormreach.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Static Grass: the Tempest Realm's turf. It dies back to Skysoil when covered and spreads onto Skysoil in the open. */
public class SkyGrassBlock extends Block {
    public SkyGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (smothered(level, pos)) {
            level.setBlockAndUpdate(pos, RealmBlocks.SKYSOIL.defaultBlockState());
            return;
        }
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(target).is(RealmBlocks.SKYSOIL) && !smothered(level, target)) {
                level.setBlockAndUpdate(target, defaultBlockState());
            }
        }
    }

    private static boolean smothered(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.getFluidState().isEmpty() || above.isSolidRender();
    }
}
