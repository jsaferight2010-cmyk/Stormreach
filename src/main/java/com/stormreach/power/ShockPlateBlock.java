package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Shock Plate: a copper grid laid on the floor. When a hostile mob steps on it, it shocks it for 4 damage
 * (100 SU a shock, at most twice a second). Players, pets and passive mobs walk over it safely.
 */
public class ShockPlateBlock extends NodeBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    public static final int COST = 100;
    public static final float DAMAGE = 4f;

    public ShockPlateBlock(Properties p) {
        super(p);
    }

    @Override protected Supplier<? extends BlockEntityType<? extends NodeEntity>> type() { return () -> PowerRegistry.SHOCK_PLATE_ENTITY; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected void entityInside(BlockState s, Level level, BlockPos pos, Entity e, InsideBlockEffectApplier fx, boolean b) {
        if (!(level instanceof ServerLevel server) || !(e instanceof LivingEntity le) || !(e instanceof Enemy) || !le.isAlive()) return;
        if (!(level.getBlockEntity(pos) instanceof Plate plate) || server.getGameTime() < plate.ready || !plate.spend(COST)) return;
        plate.ready = server.getGameTime() + 10;
        le.hurtServer(server, server.damageSources().lightningBolt(), DAMAGE);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 0.3, e.getZ(), 16, 0.3, 0.2, 0.3, 0.2);
        server.playSound(null, pos, com.stormreach.registry.ModSfx.CREATURE_EEL_ZAP, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    public static class Plate extends NodeEntity {
        long ready;

        public Plate(BlockPos pos, BlockState state) {
            super(PowerRegistry.SHOCK_PLATE_ENTITY, pos, state, Role.CONSUMER, 2000);
        }

        @Override public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {}
    }
}
