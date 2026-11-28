package com.stormreach.front;

import com.stormreach.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tesla Coil: a copper coil under a storm toroid that stores lightning and spends it on hostile mobs. It charges when
 * lightning strikes within 24 blocks, slowly during a thunderstorm under open sky, or from a Storm Cell. While it has
 * charge it zaps the nearest hostile mob in sight within 10 blocks every second. A redstone signal holds its fire.
 * Comparators read the charge; it glows brighter the more it holds.
 */
public class TeslaCoilBlock extends Block implements EntityBlock {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 5);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(5, 3, 5, 11, 13, 11), Block.box(1, 13, 1, 15, 16, 15));

    public TeslaCoilBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CHARGE); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TeslaCoilBlockEntity(pos, state); }
    @Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos, Direction d) {
        return level.getBlockEntity(pos) instanceof TeslaCoilBlockEntity be ? be.charge() * 15 / TeslaCoilBlockEntity.MAX : 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        return level.isClientSide() || type != FrontRegistry.TESLA_ENTITY ? null
                : (l, p, st, be) -> ((TeslaCoilBlockEntity) be).serverTick((ServerLevel) l, p, st);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.STORM_CELL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof TeslaCoilBlockEntity be) {
            if (be.charge() >= TeslaCoilBlockEntity.MAX) return InteractionResult.FAIL;
            be.addCharge(server, TeslaCoilBlockEntity.MAX / 2);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TeslaCoilBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("block.stormreach.tesla_coil.reading", be.charge() * 100 / TeslaCoilBlockEntity.MAX,
                    level.hasNeighborSignal(pos) ? Component.translatable("block.stormreach.tesla_coil.held") : Component.literal("")).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }
}
