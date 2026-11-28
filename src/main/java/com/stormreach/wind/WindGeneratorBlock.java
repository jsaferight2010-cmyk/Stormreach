package com.stormreach.wind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 6.0: Creative Wind Generator. A creative-only fan block: its front blows wind at a speed you set (0 to 320 mph, so
 * anything from a breeze to EF5 winds), out to a reach you set. Right-click (in Creative) opens its settings; a
 * redstone signal switches it off. It is not craftable and does not drop, so it stays a testing and map-making tool.
 */
public class WindGeneratorBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Set by the client: opens the settings screen for the generator at a position. */
    public static java.util.function.Consumer<BlockPos> openScreen = p -> {};

    public WindGeneratorBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    /** The front faces the way the placer looks, so the wind blows away from you. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WindGeneratorBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        if (type != WindRegistry.GENERATOR_ENTITY) return null;
        return level.isClientSide()
                ? (l, p, st, be) -> WindGeneratorBlockEntity.clientTick(l, p, st, (WindGeneratorBlockEntity) be)
                : (l, p, st, be) -> WindGeneratorBlockEntity.serverTick((ServerLevel) l, p, st, (WindGeneratorBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isCreative()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.stormreach.wind_generator.creative_only"));
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) openScreen.accept(pos);
        return InteractionResult.SUCCESS;
    }
}
