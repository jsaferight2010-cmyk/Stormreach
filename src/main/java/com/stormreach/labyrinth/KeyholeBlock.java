package com.stormreach.labyrinth;

import com.stormreach.progress.StormAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The treasure chamber's lock. It takes the three Labyrinth Keys from the corner vaults; then the seal crumbles. */
public class KeyholeBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty KEYS = IntegerProperty.create("keys", 0, 3);

    public KeyholeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KEYS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, KEYS);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(LabyrinthRegistry.LABYRINTH_KEY)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int keys = state.getValue(KEYS);
        if (keys >= 3) return InteractionResult.FAIL;
        stack.consume(1, player);
        keys++;
        server.setBlock(pos, state.setValue(KEYS, keys), Block.UPDATE_ALL);
        server.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.5f + keys * 0.15f);
        player.sendOverlayMessage(Component.translatable("block.stormreach.labyrinth_keyhole.keys", keys).withStyle(ChatFormatting.GOLD));
        if (keys == 3) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, -3, -6), pos.offset(6, 5, 6))) {
                if (server.getBlockState(p).is(LabyrinthRegistry.SEAL)) {
                    server.destroyBlock(p, false);
                    server.sendParticles(ParticleTypes.SCULK_SOUL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
            server.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.BLOCKS, 2f, 1.4f);
            if (player instanceof ServerPlayer sp) StormAdvancements.award(sp, "labyrinth_unsealed");
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.stormreach.labyrinth_keyhole.hint", state.getValue(KEYS)));
        return InteractionResult.SUCCESS;
    }
}
