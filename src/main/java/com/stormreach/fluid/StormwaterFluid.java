package com.stormreach.fluid;

import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;

/**
 * Stormwater: sea water charged by the Stormblight. Swims and flows like water (it is in the water tag,
 * so boats, drowning, farmland and Riptide all work), but it glows, gives swimmers Static Charge and
 * Dolphin's Grace, shocks anyone who isn't Grounded, and carries lightning through the whole pool.
 * In Unlimited spread mode it slowly turns the water next to it into stormwater.
 */
public abstract class StormwaterFluid extends WaterFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_STORMWATER;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.STORMWATER;
    }

    @Override
    public Item getBucket() {
        return ModFluids.STORMWATER_BUCKET;
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return ModFluids.STORMWATER_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.STORMWATER || fluid == ModFluids.FLOWING_STORMWATER;
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
        if (StormreachConfig.spreadMode != StormreachConfig.SpreadMode.UNLIMITED || !state.isSource()) return;
        BlockPos target = pos.relative(Direction.getRandom(random));
        FluidState there = level.getFluidState(target);
        if (there.is(Fluids.WATER) && there.isSource() && level.getBlockState(target).getBlock() instanceof LiquidBlock) {
            level.setBlockAndUpdate(target, ModFluids.STORMWATER_BLOCK.defaultBlockState());
        }
    }

    public static class Flowing extends StormwaterFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends StormwaterFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
