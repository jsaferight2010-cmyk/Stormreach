package com.stormreach.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Reads the sky: gives a redstone signal of 0 in clear weather, 7 in rain and 15 in a thunderstorm.
 * Its face also changes so you can read it without redstone.
 */
public class StormBarometerBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final int CHECK_INTERVAL = 20;

    public StormBarometerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int power = level.isThundering() ? 15 : level.isRaining() ? 7 : 0;
        if (state.getValue(POWER) != power) {
            level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level instanceof ServerLevel server) com.stormreach.tornado.InstrumentBlock.report(com.stormreach.tornado.InstrumentBlock.Kind.BAROMETER, server, pos, player);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
