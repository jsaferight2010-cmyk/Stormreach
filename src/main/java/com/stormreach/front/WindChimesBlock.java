package com.stormreach.front;

import com.stormreach.registry.ModSfx;
import com.stormreach.tornado.Instruments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wind Chimes: five copper tubes that hang under a block (or a fence or chain) and ring in the wind: now and then in
 * a breeze, constantly in a gale. Indoors they hardly stir. The notes are a pentatonic scale, so they always sound
 * right together.
 */
public class WindChimesBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 16, 12);
    private static final float[] SCALE = {0.75f, 0.84f, 0.94f, 1.12f, 1.26f, 1.5f};

    public WindChimesBlock(Properties p) {
        super(p);
    }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos up = pos.above();
        BlockState above = level.getBlockState(up);
        return above.isFaceSturdy(level, up, Direction.DOWN) || above.is(net.minecraft.tags.BlockTags.FENCES) || above.is(Blocks.IRON_CHAIN)
                || above.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        return dir == Direction.UP && !canSurvive(s, level, pos) ? Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 20);
    }

    /** Chance per second that the chimes ring, for this gust speed. */
    public static float ringChance(double gustMph, boolean outdoors) {
        return (float) Math.min(1.0, Math.max(0, gustMph - 3) / 30.0) * (outdoors ? 1f : 0.2f);
    }

    @Override
    protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        double[] wind = Instruments.windMph(level, pos);
        boolean outdoors = level.canSeeSky(pos);
        float chance = ringChance(wind[1], outdoors);
        if (random.nextFloat() < chance) {
            int notes = 1 + (wind[1] > 25 ? random.nextInt(3) : 0);
            for (int i = 0; i < notes; i++) {
                level.playSound(null, pos, ModSfx.BLOCK_WIND_CHIME, SoundSource.BLOCKS, 0.35f + Math.min(0.6f, (float) wind[1] / 60f),
                        SCALE[random.nextInt(SCALE.length)]);
            }
        }
        level.scheduleTick(pos, this, 14 + random.nextInt(14));
    }
}
