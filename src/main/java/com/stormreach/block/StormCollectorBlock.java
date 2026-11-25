package com.stormreach.block;

import com.stormreach.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Soaks up a storm. Under open sky it charges slowly in rain and quickly in a thunderstorm.
 * When full, use it with an empty hand to take a Storm Cell. Comparators read the charge.
 */
public class StormCollectorBlock extends Block {
    public static final int MAX = 4;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, MAX);

    public StormCollectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 100);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        if (charge < MAX && level.canSeeSky(pos.above())) {
            int chance = level.isThundering() ? 2 : level.isRaining() ? 12 : 0;
            if (chance > 0 && random.nextInt(chance) == 0) {
                level.setBlock(pos, state.setValue(CHARGE, charge + 1), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.1);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.5f);
            }
        }
        level.scheduleTick(pos, this, 100);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(CHARGE) < MAX) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            Block.popResource(server, pos.above(), new ItemStack(ModItems.STORM_CELL));
            server.setBlock(pos, state.setValue(CHARGE, 0), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7f, 1.6f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(CHARGE) * 15 / MAX;
    }
}
