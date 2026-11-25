package com.stormreach.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A Hollow Lamp set in the Labyrinth ceiling. Its light drifts: it slowly brightens and dims every few seconds, and
 * now and then gutters almost out, so the maze's lighting is never quite the same twice.
 */
public class HollowLampBlock extends Block {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 1, 12);

    public HollowLampBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 7));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIGHT); }

    @Override
    protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) level.scheduleTick(pos, this, 20 + level.getRandom().nextInt(60));
    }

    /** Next light level: a small drift most of the time, a gutter (very dim) now and then, a recovery after. */
    public static int next(int light, RandomSource r) {
        if (light <= 2) return 5 + r.nextInt(4);
        if (r.nextInt(12) == 0) return 1 + r.nextInt(2);
        return Math.max(3, Math.min(12, light + r.nextInt(5) - 2));
    }

    @Override
    protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
        int n = next(s.getValue(LIGHT), r);
        if (n != s.getValue(LIGHT)) level.setBlock(pos, s.setValue(LIGHT, n), 2);
        level.scheduleTick(pos, this, n <= 2 ? 10 + r.nextInt(20) : 40 + r.nextInt(80));
    }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 20);
    }
}
