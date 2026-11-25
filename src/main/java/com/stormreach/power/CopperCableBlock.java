package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Copper Cable: insulated wire that joins itself and any power block it touches into one network. */
public class CopperCableBlock extends Block {
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    static {
        ARMS.put(Direction.NORTH, Block.box(6.5, 6.5, 0, 9.5, 9.5, 6));
        ARMS.put(Direction.SOUTH, Block.box(6.5, 6.5, 10, 9.5, 9.5, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6.5, 6.5, 6, 9.5, 9.5));
        ARMS.put(Direction.EAST, Block.box(10, 6.5, 6.5, 16, 9.5, 9.5));
        ARMS.put(Direction.DOWN, Block.box(6.5, 0, 6.5, 9.5, 6, 9.5));
        ARMS.put(Direction.UP, Block.box(6.5, 10, 6.5, 9.5, 16, 9.5));
    }

    public CopperCableBlock(Properties p) {
        super(p);
        BlockState s = stateDefinition.any();
        for (BooleanProperty b : PipeBlock.PROPERTY_BY_DIRECTION.values()) s = s.setValue(b, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }

    static boolean connects(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof CopperCableBlock || level.getBlockEntity(pos) instanceof PowerNode;
    }

    BlockState connected(BlockGetter level, BlockPos pos) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.values()) s = s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), connects(level, pos.relative(d)));
        return s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connected(ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess t, BlockPos pos, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        return s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), connects(level, np));
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape shape = CORE;
        for (Direction d : Direction.values()) if (s.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d))) shape = Shapes.or(shape, ARMS.get(d));
        return shape;
    }
}
