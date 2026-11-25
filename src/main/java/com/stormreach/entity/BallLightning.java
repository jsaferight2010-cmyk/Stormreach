package com.stormreach.entity;

import com.stormreach.weather.StormWeather;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Ball Lightning: a crackling orb that drifts out of a Thunderhead toward players and bursts
 * into a small lightning strike when it gets close. It fizzles out after half a minute.
 */
public class BallLightning extends Monster {
    private int life = 600;

    public BallLightning(EntityType<? extends BallLightning> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DriftAtTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (tickCount % 3 == 0) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.3, getZ(), 3, 0.15, 0.15, 0.15, 0.08);
            }
            if (--life <= 0) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
                discard();
            }
        }
    }

    /** Bursts into lightning on the spot. */
    public void burst(ServerLevel level) {
        StormWeather.strike(level, blockPosition(), null);
        discard();
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        // Lightning only feeds it.
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.stormreach.registry.ModSfx.MAGIC_HUM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.stormreach.registry.ModSfx.BLOCK_BOOM;
    }

    static class DriftAtTargetGoal extends Goal {
        private final BallLightning orb;

        DriftAtTargetGoal(BallLightning orb) {
            this.orb = orb;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return orb.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = orb.getTarget();
            if (target == null) return;
            orb.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 0.9);
            if (orb.distanceToSqr(target) < 4.0 && orb.level() instanceof ServerLevel server) {
                orb.burst(server);
            }
        }
    }
}
