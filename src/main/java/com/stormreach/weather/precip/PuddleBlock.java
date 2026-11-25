package com.stormreach.weather.precip;

import com.stormreach.sim.Rainfall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A rain puddle: a thin sheet of water that collects on open ground during long or heavy rain, grows while it
 * keeps raining, and dries up afterwards. It's only a surface film: no collision, it can't be scooped with a bucket,
 * and anything placed there replaces it. Walking through splashes, and it puts out burning mobs and players.
 */
public class PuddleBlock extends Block {
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 1, 3);
    private static final VoxelShape[] SHAPES = {
            Block.box(4, 0, 4, 12, 0.5, 12), Block.box(2, 0, 2, 14, 0.5, 14), Block.box(0, 0, 0, 16, 0.5, 16)};

    public PuddleBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(SIZE, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(SIZE); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPES[s.getValue(SIZE) - 1]; }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

    @Override
    protected boolean canBeReplaced(BlockState s, BlockPlaceContext ctx) { return true; }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState n, RandomSource r) {
        return !s.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        double chance = Rainfall.evaporation(level.isRainingAt(pos), level.getBiome(pos).value().getBaseTemperature());
        if (random.nextDouble() >= chance) return;
        int size = s.getValue(SIZE);
        level.setBlock(pos, size <= 1 ? Blocks.AIR.defaultBlockState() : s.setValue(SIZE, size - 1), 3);
    }

    @Override
    protected void entityInside(BlockState s, Level level, BlockPos pos, Entity e, InsideBlockEffectApplier effects, boolean intersects) {
        if (e.isOnFire()) e.clearFire();
        if (level instanceof ServerLevel server && e.getDeltaMovement().horizontalDistanceSqr() > 0.004 && e.tickCount % 3 == 0) {
            server.sendParticles(com.stormreach.particle.ModParticles.RAIN_SPLASH, e.getX(), pos.getY() + 0.1, e.getZ(), 4, 0.2, 0.0, 0.2, 0.05);
            if (e.tickCount % 9 == 0) server.playSound(null, pos, com.stormreach.registry.ModSfx.WEATHER_PUDDLE_SPLASH, net.minecraft.sounds.SoundSource.BLOCKS, 0.35f, 0.9f + server.getRandom().nextFloat() * 0.3f);
        }
    }

    /** Starts or grows a puddle in the air block {@code pos} above the ground. */
    public static boolean grow(ServerLevel level, BlockPos pos, int maxSize) {
        BlockState here = level.getBlockState(pos);
        if (here.is(PrecipContent.PUDDLE)) {
            int size = here.getValue(SIZE);
            if (size < maxSize) level.setBlock(pos, here.setValue(SIZE, size + 1), 3);
            return true;
        }
        if (!here.isAir()) return false;
        BlockState p = PrecipContent.PUDDLE.defaultBlockState();
        if (!p.canSurvive(level, pos)) return false;
        level.setBlock(pos, p, 3);
        return true;
    }
}
