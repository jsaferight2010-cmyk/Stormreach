package com.stormreach.realm;

import com.stormreach.StormreachConfig;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Clouds of the Tempest Realm. You fall into them rather than onto them.
 * Nimbus clouds catch you softly, thunderclouds fling you upward, gale clouds carry you with the wind.
 */
public class CloudBlock extends Block {
    public enum Kind { NIMBUS, THUNDER, GALE }

    private final Kind kind;

    public CloudBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (!StormreachConfig.cloudBlocks) return;
        entity.resetFallDistance();
        Vec3 m = entity.getDeltaMovement();
        switch (kind) {
            case NIMBUS -> {
                // Sink slowly; sneaking lets you drop through faster.
                double y = entity.isShiftKeyDown() ? Math.max(m.y, -0.3) : Math.max(m.y, -0.06);
                entity.setDeltaMovement(m.x * 0.8, y, m.z * 0.8);
            }
            case THUNDER -> {
                if (m.y < 1.0) {
                    entity.setDeltaMovement(m.x, 1.3, m.z);
                    entity.needsSync = true;
                    if (level instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY(), entity.getZ(), 12, 0.4, 0.2, 0.4, 0.2);
                        level.playSound(null, pos, com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.BLOCKS, 0.4f, 1.8f);
                    }
                }
            }
            case GALE -> {
                Vec3 wind = StormWeather.windVector().scale(0.12);
                entity.setDeltaMovement(m.x + wind.x, Math.max(m.y, 0.02), m.z + wind.z);
                entity.needsSync = true;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.THUNDER && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0, 0);
        } else if (kind == Kind.GALE && random.nextInt(12) == 0) {
            Vec3 wind = StormWeather.windVector().scale(0.2);
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), wind.x, 0, wind.z);
        }
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, net.minecraft.core.Direction direction) {
        return neighbor.is(this) || super.skipRendering(state, neighbor, direction);
    }
}
