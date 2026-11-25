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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One of the three locks built into the Vault Door. Each takes one particular key (Gale, Thunder or Hollow, found
 * in the three corner vaults); everything is checked on the server. When all three locks in the door are set, the
 * door opens (see {@link VaultDoor}).
 */
public class KeyholeBlock extends HorizontalDirectionalBlock {
    /** 0 Gale, 1 Thunder, 2 Hollow. */
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 2);
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final String[] KEY_NAMES = {"item.stormreach.labyrinth_key_gale", "item.stormreach.labyrinth_key_thunder", "item.stormreach.labyrinth_key_hollow"};

    public KeyholeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, 0).setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, KIND, FILLED);
    }

    public static Item keyFor(int kind) {
        return LabyrinthRegistry.KEYS[Math.max(0, Math.min(2, kind))];
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean anyKey = false;
        for (Item k : LabyrinthRegistry.KEYS) anyKey |= stack.is(k);
        if (!anyKey) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int kind = state.getValue(KIND);
        if (state.getValue(FILLED)) return InteractionResult.FAIL;
        if (!stack.is(keyFor(kind))) {
            player.sendOverlayMessage(Component.translatable("block.stormreach.labyrinth_keyhole.wrong",
                    Component.translatable(KEY_NAMES[kind])).withStyle(ChatFormatting.RED));
            server.playSound(null, pos, com.stormreach.registry.ModSfx.BLOCK_CHAIN, SoundSource.BLOCKS, 1f, 0.6f);
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        server.setBlock(pos, state.setValue(FILLED, true), Block.UPDATE_ALL);
        server.playSound(null, pos, com.stormreach.registry.ModSfx.BLOCK_METAL_DOOR_OPEN, SoundSource.BLOCKS, 1.2f, 0.55f + kind * 0.15f);
        server.sendParticles(com.stormreach.particle.ModParticles.VAULT_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.4);
        int set = VaultDoor.locksSet(server, pos);
        player.sendOverlayMessage(Component.translatable("block.stormreach.labyrinth_keyhole.keys", set).withStyle(ChatFormatting.GOLD));
        if (set == 3 && VaultDoor.open(server, pos)) {
            if (player instanceof ServerPlayer sp) StormAdvancements.award(sp, "labyrinth_unsealed");
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(state.getValue(FILLED) ? "block.stormreach.labyrinth_keyhole.set"
                : "block.stormreach.labyrinth_keyhole.hint", Component.translatable(KEY_NAMES[state.getValue(KIND)])));
        return InteractionResult.SUCCESS;
    }
}
