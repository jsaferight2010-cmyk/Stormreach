package com.stormreach.entity;

import com.stormreach.StormreachConfig;
import com.stormreach.blight.Stormblight;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Storm Front: a travelling black storm cloud released from a Stormheart with a Thunderhead Horn.
 * It drifts with the wind about a dozen blocks above the ground, raining, throwing lightning and
 * turning the land and mobs beneath it into Stormreach ones. It breaks up after a few minutes.
 */
public class StormFront extends PathfinderMob {
    private static final int RADIUS = 8;
    private int life = -1;

    public StormFront(EntityType<? extends StormFront> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvisible(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative()) return super.hurtServer(level, source, amount);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (life < 0) life = StormreachConfig.stormFrontMinutes * 60 * 20;
        if (--life <= 0 || !StormreachConfig.stormFront) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 80, RADIUS * 0.6, 1.5, RADIUS * 0.6, 0.05);
            discard();
            return;
        }
        RandomSource random = getRandom();

        // Drift with the wind and hold about 12 blocks over the ground.
        BlockPos ground = server.dimensionType().hasCeiling()
                ? java.util.Objects.requireNonNullElse(StormWeather.surfaceBelow(server, blockPosition(), 40), blockPosition().below(12))
                : server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPosition());
        double dy = (ground.getY() + (server.dimensionType().hasCeiling() ? 6 : 12) - getY()) * 0.05;
        Vec3 wind = StormWeather.windVector().scale(0.035);
        setDeltaMovement(Vec3.ZERO);
        setPos(getX() + wind.x * 2, getY() + dy, getZ() + wind.z * 2);

        // The cloud itself, plus rain falling beneath it.
        if (tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 12, RADIUS * 0.5, 0.8, RADIUS * 0.5, 0.01);
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 8, RADIUS * 0.5, 0.6, RADIUS * 0.5, 0.01);
            server.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() - 1, getZ(), 20, RADIUS * 0.6, 0.2, RADIUS * 0.6, 0.0);
        }

        // Convert the land under the cloud.
        int columns = 6 + StormreachConfig.spreadSpeed * 3;
        for (int i = 0; i < columns; i++) {
            BlockPos column = BlockPos.containing(getX() + random.nextInt(RADIUS * 2 + 1) - RADIUS, getY(), getZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockPos top = server.dimensionType().hasCeiling() ? StormWeather.surfaceBelow(server, column, 40)
                    : server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
            if (top == null || !server.isLoaded(top)) continue;
            if (server.dimensionType().hasCeiling()) top = top.above();
            for (int d = 1; d <= 4; d++) Stormblight.convertBlock(server, top.below(d), random);
        }
        if (tickCount % 20 == 0) {
            Stormblight.convertOneMob(server, new AABB(blockPosition()).inflate(RADIUS + 2, 16, RADIUS + 2), random);
        }
        if (random.nextInt(60) == 0) {
            BlockPos strike = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    BlockPos.containing(getX() + random.nextInt(RADIUS * 2) - RADIUS, getY(), getZ() + random.nextInt(RADIUS * 2) - RADIUS));
            if (StormreachConfig.lightningHarmful) StormWeather.strike(server, strike, null);
            else StormWeather.strikeVisual(server, strike);
        }
    }
}
