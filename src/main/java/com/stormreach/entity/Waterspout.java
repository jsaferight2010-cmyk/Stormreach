package com.stormreach.entity;

import com.stormreach.weather.StormWeather;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Waterspout: a spinning column of sea water that forms over the ocean in a Thunderhead.
 * It wanders with the wind, lifts anything that gets close and flings it out. It can't be hurt
 * and collapses on its own after about a minute, or as soon as the storm ends.
 */
public class Waterspout extends PathfinderMob {
    private int life = 1200;

    public Waterspout(EntityType<? extends Waterspout> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Only /kill and the void can stop a waterspout.
        if (source.getEntity() instanceof Player player && player.isCreative()) return super.hurtServer(level, source, amount);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (--life <= 0 || !server.isThundering()) {
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 60, 1.0, 2.0, 1.0, 0.2);
            discard();
            return;
        }
        // Drift with the wind.
        Vec3 wind = StormWeather.windVector().scale(0.04);
        setDeltaMovement(wind.x, getDeltaMovement().y, wind.z);

        if (tickCount % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = (tickCount * 0.4) + i * Math.PI / 3;
                double h = random.nextDouble() * 7;
                double r = 0.4 + h * 0.18;
                server.sendParticles(ParticleTypes.SPLASH, getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 7.5, getZ(), 2, 1.0, 0.3, 1.0, 0.01);
        }

        List<Entity> caught = server.getEntities(this, new AABB(blockPosition()).inflate(3.0, 6.0, 3.0), e -> !(e instanceof Waterspout) && !e.isSpectator());
        for (Entity e : caught) {
            if (e instanceof Player p && p.isCreative()) continue;
            Vec3 toCenter = position().subtract(e.position()).multiply(1, 0, 1);
            double lift = e.getY() - getY() < 6 ? 0.18 : 0.0;
            Vec3 swirl = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(0.25);
            if (lift == 0.0) {
                // Reached the top: fling outward.
                swirl = toCenter.normalize().scale(-1.2);
                lift = 0.4;
            }
            e.push(swirl.x + toCenter.x * 0.05, lift, swirl.z + toCenter.z * 0.05);
            e.needsSync = true;
            e.resetFallDistance();
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
