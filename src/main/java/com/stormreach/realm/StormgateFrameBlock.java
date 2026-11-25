package com.stormreach.realm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One piece of a Stormgate ring. A Tempest Eye can be set into it. */
public class StormgateFrameBlock extends Block {
    public static final BooleanProperty EYE = BlockStateProperties.EYE;

    public StormgateFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EYE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EYE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(RealmItems.TEMPEST_EYE) || state.getValue(EYE)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(EYE, true), Block.UPDATE_ALL);
            stack.consume(1, player);
            server.playSound(null, pos, com.stormreach.registry.ModSfx.MAGIC_CHIME, SoundSource.BLOCKS, 1.0f, 0.7f);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.2);
            Stormgate.describe(server, pos, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) Stormgate.describe(server, pos, player);
        else return InteractionResult.SUCCESS;
        return InteractionResult.SUCCESS_SERVER;
    }

    static Component hint(String key, Object... args) {
        return Component.translatable("block.stormreach.stormgate." + key, args).withStyle(ChatFormatting.AQUA);
    }
}
