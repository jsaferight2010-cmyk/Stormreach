package com.stormreach.power;

import com.stormreach.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Supplier;

/**
 * Electric Pump: set it over water and it lifts a quarter bucket a second into a Rain Barrel or Cistern beside or
 * above it, for 80 SU. A source that has two or more source neighbours (a pond, a river, the sea) is never used up;
 * a lone source block is drawn dry.
 */
public class ElectricPumpBlock extends NodeBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int COST = 80, MB = 250;

    public ElectricPumpBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override protected Supplier<? extends BlockEntityType<? extends NodeEntity>> type() { return () -> PowerRegistry.PUMP_ENTITY; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(POWERED); }

    public static class Entity extends NodeEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(PowerRegistry.PUMP_ENTITY, pos, state, Role.CONSUMER, 2000);
        }

        @Override public int rate() { return COST / 20; }

        @Override
        public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
            boolean ran = pump(level, pos);
            if (state.getValue(POWERED) != ran) level.setBlock(pos, state.setValue(POWERED, ran), Block.UPDATE_CLIENTS);
        }

        boolean pump(ServerLevel level, BlockPos pos) {
            BlockPos src = pos.below();
            FluidState f = level.getFluidState(src);
            if (!f.isSource() || !f.is(FluidTags.WATER) || stored() < COST) return false;
            boolean storm = f.getType().isSame(ModFluids.STORMWATER);
            WaterTank tank = null;
            for (Direction d : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                if (level.getBlockEntity(pos.relative(d)) instanceof WaterTank t && t.waterCapacity() - t.water() >= MB) { tank = t; break; }
            }
            if (tank == null) return false;
            tank.fill(MB, storm);
            spend(COST);
            int neighbours = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) if (level.getFluidState(src.relative(d)).isSource()) neighbours++;
            if (neighbours < 2 && level.getRandom().nextInt(4) == 0) level.setBlock(src, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (level.getRandom().nextInt(3) == 0) level.playSound(null, pos, com.stormreach.registry.ModSfx.BLOCK_MACHINE_HUM, SoundSource.BLOCKS, 0.5f, 1.0f);
            return true;
        }
    }
}
