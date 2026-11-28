package com.stormreach.weather.precip;

import com.stormreach.sim.HailSize;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Hail on the ground. It piles up in layers (at most four: hail drifts, it doesn't bury things) and melts quickly,
 * a layer at a time on a timer rather than on random ticks, so a hailstorm's cover is gone within about half a
 * minute (up to a minute where big stones fell) and can't build up into lag. It melts even in cold biomes.
 */
public class HailLayerBlock extends SnowLayerBlock {
    public static final int MAX_LAYERS = 4;
    /** Big stones (golf ball and up) in this pile: they take longer to melt. */
    public static final BooleanProperty BIG = BooleanProperty.create("big");

    public HailLayerBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(BIG, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(BIG);
    }

    /** Ticks until the next layer melts: 6–9 s for small hail, 11–15 s with big stones in the pile. */
    public static int meltDelay(boolean big, RandomSource r) {
        return big ? 220 + r.nextInt(80) : 120 + r.nextInt(60);
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide() && !old.is(this)) level.scheduleTick(pos, this, meltDelay(state.getValue(BIG), level.getRandom()));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int layers = state.getValue(LAYERS);
        if (layers <= 1) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        level.setBlock(pos, state.setValue(LAYERS, layers - 1), 3);
        level.scheduleTick(pos, this, meltDelay(state.getValue(BIG), random));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // safety net: a pile whose timer was lost (e.g. a chunk unloaded mid-melt) still goes away
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, meltDelay(state.getValue(BIG), random));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) { return true; }

    /** Adds one layer at {@code pos} (the air block on top of the ground). Returns true if hail was placed. */
    public static boolean accumulate(ServerLevel level, BlockPos pos, HailSize size) {
        boolean big = size.ordinal() >= HailSize.GOLF_BALL.ordinal();
        BlockState here = level.getBlockState(pos);
        if (here.is(PrecipContent.HAIL_LAYER)) {
            int l = here.getValue(LAYERS);
            BlockState next = here.setValue(BIG, here.getValue(BIG) || big);
            if (l < MAX_LAYERS) next = next.setValue(LAYERS, l + 1);
            if (next != here) level.setBlock(pos, next, 3);
            return true;
        }
        if (here.isAir() || here.is(PrecipContent.PUDDLE)) {
            BlockState hail = PrecipContent.HAIL_LAYER.defaultBlockState().setValue(BIG, big);
            if (hail.canSurvive(level, pos)) {
                level.setBlock(pos, hail, 3);
                return true;
            }
        }
        return false;
    }
}
