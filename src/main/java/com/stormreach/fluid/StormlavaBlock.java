package com.stormreach.fluid;

import com.stormreach.registry.ModBlocks;
import com.stormreach.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;

/** The Stormlava block: hardens into storm blocks when water touches it. */
public class StormlavaBlock extends LiquidBlock {
    public StormlavaBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!harden(level, pos, state)) super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean movedByPiston) {
        if (!harden(level, pos, state)) super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
    }

    private boolean harden(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return false;
        boolean source = level.getFluidState(pos).isSource();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            FluidState touching = level.getFluidState(pos.relative(dir));
            if (!touching.is(FluidTags.WATER)) continue;
            boolean stormwater = touching.getType().isSame(ModFluids.STORMWATER);
            Block result = stormwater && source ? ModBlocks.STORM_CRYSTAL_ORE
                    : source ? ModBlocks.CHARGED_STORMSTONE : ModBlocks.STORMSTONE;
            level.setBlockAndUpdate(pos, result.defaultBlockState());
            level.levelEvent(1501, pos, 0); // lava fizz sound + smoke
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.6f);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.2);
            }
            return true;
        }
        return false;
    }
}
