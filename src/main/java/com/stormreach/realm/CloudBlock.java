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
 * Nimbus clouds catch you softly, thunderclouds fling you upward, gale clouds gust you onward the way you came in.
 */
public class CloudBlock extends Block {
    public enum Kind { NIMBUS, THUNDER, GALE }

    private final Kind kind;
    /** When each entity last got a gust (weak keys: entities aren't kept alive by this). */
    private static final java.util.Map<Entity, Long> GUSTED = new java.util.WeakHashMap<>();

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
                        server.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, entity.getX(), entity.getY(), entity.getZ(), 12, 0.4, 0.2, 0.4, 0.2);
                        level.playSound(null, pos, com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.BLOCKS, 0.4f, 1.8f);
                    }
                }
            }
            case GALE -> {
                // 6.0: one gust per entry, aimed from the way the entity met the cloud (sim/GaleLaunch);
                // while still inside after a gust it just keeps its momentum
                long now = level.getGameTime();
                Long last;
                synchronized (GUSTED) { last = GUSTED.get(entity); }
                if (last != null && now - last < 12 && now >= last) return;
                Vec3 c = entity.getBoundingBox().getCenter();
                Vec3 look = entity.getLookAngle();
                double[] v = com.stormreach.sim.GaleLaunch.vector(m.x, m.y, m.z, c.x - (pos.getX() + 0.5), c.y - (pos.getY() + 0.5),
                        c.z - (pos.getZ() + 0.5), look.x, look.z);
                if (entity.isShiftKeyDown()) { v[0] *= 0.5; v[1] *= 0.6; v[2] *= 0.5; }   // crouching braces you
                entity.setDeltaMovement(v[0], v[1], v[2]);
                entity.needsSync = true;
                synchronized (GUSTED) { GUSTED.put(entity, now); }
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 14, 0.3, 0.3, 0.3, 0.12);
                    level.playSound(null, pos, com.stormreach.registry.ModSfx.WEATHER_GUST, SoundSource.BLOCKS, 0.7f, 1.1f + level.getRandom().nextFloat() * 0.3f);
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.THUNDER && random.nextInt(8) == 0) {
            level.addParticle(com.stormreach.particle.ModParticles.STORM_SPARK, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
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
