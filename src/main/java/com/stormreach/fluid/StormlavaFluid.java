package com.stormreach.fluid;

import com.stormreach.registry.ModFluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;

/**
 * Stormlava: lava fused with lightning into blue-white plasma. It burns like lava (it is in the lava tag)
 * and also shocks, it is as bright as possible, and it cools into storm blocks instead of stone:
 * Charged Stormstone or Stormstone next to water, and Storm Crystal Ore next to stormwater.
 * Lightning that hits it can throw off Ball Lightning.
 */
public abstract class StormlavaFluid extends LavaFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_STORMLAVA;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.STORMLAVA;
    }

    @Override
    public Item getBucket() {
        return ModFluids.STORMLAVA_BUCKET;
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return ModFluids.STORMLAVA_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.STORMLAVA || fluid == ModFluids.FLOWING_STORMLAVA;
    }

    public static class Flowing extends StormlavaFluid {
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

    public static class Source extends StormlavaFluid {
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
