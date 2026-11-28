package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Sprinkler: a powered spray head. Every two seconds, with 80 SU and a twentieth of a bucket from a tank it sits on
 * or beside, it waters a 9x9 area: farmland stays wet, crops and saplings get an extra growth tick (two with
 * Stormwater), and fires go out.
 */
public class SprinklerBlock extends NodeBlock {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 7, 11);
    public static final int COST = 80, MB = 50, RADIUS = 4;

    public SprinklerBlock(Properties p) {
        super(p);
    }

    @Override protected Supplier<? extends BlockEntityType<? extends NodeEntity>> type() { return () -> PowerRegistry.SPRINKLER_ENTITY; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    public static class Entity extends NodeEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(PowerRegistry.SPRINKLER_ENTITY, pos, state, Role.CONSUMER, 1000);
        }

        @Override public int rate() { return COST / 40; }

        @Override
        public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 40 != 0 || stored() < COST) return;
            WaterTank tank = null;
            for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                if (level.getBlockEntity(pos.relative(d)) instanceof WaterTank t && t.water() >= MB) { tank = t; break; }
            }
            if (tank == null) return;
            boolean storm = tank.isStormwater();
            tank.drain(MB);
            spend(COST);
            spray(level, pos, storm);
        }
    }

    public static void spray(ServerLevel level, BlockPos pos, boolean storm) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -2, -RADIUS), pos.offset(RADIUS, 1, RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FarmlandBlock && s.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE) {
                level.setBlock(p, s.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
            } else if (s.getBlock() instanceof BaseFireBlock) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if ((s.getBlock() instanceof BonemealableBlock || s.is(BlockTags.SAPLINGS)) && s.isRandomlyTicking() && !s.is(BlockTags.DIRT)
                    && !s.is(Blocks.GRASS_BLOCK)) {
                int ticks = storm ? 2 : 1;
                for (int i = 0; i < ticks; i++) {
                    BlockState now = level.getBlockState(p);
                    if (now.isRandomlyTicking() && level.getRandom().nextInt(3) == 0) now.randomTick(level, p.immutable(), level.getRandom());
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            level.sendParticles(storm ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SPLASH, pos.getX() + 0.5 + Math.cos(a) * 2, pos.getY() + 1,
                    pos.getZ() + 0.5 + Math.sin(a) * 2, 8, 1.2, 0.2, 1.2, 0.1);
        }
        level.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, RADIUS * 0.6, 0.3, RADIUS * 0.6, 0);
        level.playSound(null, pos, SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, 0.25f, 1.6f);
    }
}
