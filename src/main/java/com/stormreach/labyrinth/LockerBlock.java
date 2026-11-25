package com.stormreach.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;

/**
 * Rusted Locker: a two-block-tall hiding spot. Right-click to step inside; while you're inside and keep still,
 * the Hollow Keeper can't see you. You are locked in place and look out through the grate; press Use to step out.
 */
public class LockerBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    // back and side panels only, so a player fits inside the open front
    private static final VoxelShape NORTH = Shapes.or(Block.box(0, 0, 14, 16, 16, 16), Block.box(0, 0, 0, 1, 16, 16), Block.box(15, 0, 0, 16, 16, 16));
    private static final VoxelShape SOUTH = Shapes.or(Block.box(0, 0, 0, 16, 16, 2), Block.box(0, 0, 0, 1, 16, 16), Block.box(15, 0, 0, 16, 16, 16));
    private static final VoxelShape EAST = Shapes.or(Block.box(0, 0, 0, 2, 16, 16), Block.box(0, 0, 0, 16, 16, 1), Block.box(0, 0, 15, 16, 16, 16));
    private static final VoxelShape WEST = Shapes.or(Block.box(14, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 1), Block.box(0, 0, 15, 16, 16, 16));

    public LockerBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // FACING is the direction the open front faces; the back panel is on the opposite side
        return switch (s.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    /** The player hiding inside isn't pushed out by the locker's own walls. */
    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ecc && ecc.getEntity() instanceof Player p) {
            BlockPos lower = s.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            if (p instanceof ServerPlayer sp ? (HidingTracker.isHidden(sp) && HidingTracker.hide(sp).locker().equals(lower))
                    : lower.equals(clientHiddenAt)) return Shapes.empty();
        }
        return getShape(s, level, pos, ctx);
    }

    /** Set by the client when the server says the local player is hiding (see LockerClient). */
    public static volatile BlockPos clientHiddenAt;

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        if (pos.getY() >= ctx.getLevel().getMaxY() || !ctx.getLevel().getBlockState(pos.above()).canBeReplaced(ctx)) return null;
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState n, RandomSource r) {
        DoubleBlockHalf half = s.getValue(HALF);
        if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
            return n.is(this) && n.getValue(HALF) != half ? s : Blocks.AIR.defaultBlockState();
        }
        return s;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            if (HidingTracker.isHidden(sp)) HidingTracker.leave(sp, true);
            else HidingTracker.enter(sp, lower, state.getValue(FACING));
        }
        return InteractionResult.SUCCESS;
    }
}
