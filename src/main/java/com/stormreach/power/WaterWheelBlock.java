package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Water Wheel: an axle block whose paddle wheel (drawn by its renderer) turns in moving water. It makes 4 SU a tick for
 * each side or the bottom with flowing water against it (up to 16), twice that in flowing Stormwater, and sends it
 * down the cable.
 */
public class WaterWheelBlock extends NodeBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty FLOW = IntegerProperty.create("flow", 0, 4);
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    public WaterWheelBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(FLOW, 0));
    }

    @Override protected Supplier<? extends BlockEntityType<? extends NodeEntity>> type() { return () -> PowerRegistry.WATER_WHEEL_ENTITY; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AXIS, FLOW); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // the axle runs across the way you're facing, so the wheel turns toward you
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis());
    }

    /** How many faces (sides and bottom) have moving water against them, and whether any of it is Stormwater. */
    static int[] flow(ServerLevel level, BlockPos pos) {
        int n = 0, storm = 0;
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
            BlockPos p = pos.relative(d);
            FluidState f = level.getFluidState(p);
            if (f.isEmpty() || f.getFlow(level, p).lengthSqr() < 1e-4) continue;
            n++;
            if (f.getType().isSame(com.stormreach.registry.ModFluids.STORMWATER)) storm = 1;
        }
        return new int[]{Math.min(4, n), storm};
    }

    public static class Entity extends NodeEntity {
        private int rate;
        /** Client-side wheel angle for the renderer. */
        public float spin;
        public long spunAt = -1;

        public Entity(BlockPos pos, BlockState state) {
            super(PowerRegistry.WATER_WHEEL_ENTITY, pos, state, Role.GENERATOR, 4000);
        }

        @Override public int rate() { return rate; }

        @Override
        public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
            int[] f = flow(level, pos);
            rate = f[0] * 4 * (f[1] == 1 ? 2 : 1);
            insert(rate * 20);
            if (state.getValue(FLOW) != f[0]) level.setBlock(pos, state.setValue(FLOW, f[0]), Block.UPDATE_CLIENTS);
            PowerNet.share(level, pos, this);
        }
    }
}
