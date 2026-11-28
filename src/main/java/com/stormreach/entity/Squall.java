package com.stormreach.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Squall: a living gust that only forms inside a Thunderhead. It rams players and knocks them back,
 * is healed by lightning, and blows apart once the storm passes. Drops Storm Shards.
 */
public class Squall extends Monster {
    private int calmTicks;

    public Squall(EntityType<? extends Squall> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RamGoal(this));
        this.goalSelector.addGoal(5, new DriftGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (tickCount % 4 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.6, getZ(), 2, 0.25, 0.3, 0.25, 0.01);
            }
            // Squalls only hold together inside a thunderstorm.
            if (!server.isThundering() && !isPersistenceRequired()) {
                if (++calmTicks > 100 + random.nextInt(100)) {
                    server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 12, 0.4, 0.4, 0.4, 0.05);
                    discard();
                }
            } else {
                calmTicks = 0;
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            Vec3 push = target.position().subtract(position()).normalize().scale(0.9);
            living.push(push.x, 0.45, push.z);
            living.needsSync = true;
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), this);
            playSound(com.stormreach.registry.ModSfx.WEATHER_GUST, 0.8f, 1.2f);
        }
        return hit;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        heal(8.0f);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.stormreach.registry.ModSfx.WEATHER_WIND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return com.stormreach.registry.ModSfx.CREATURE_WIND_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.stormreach.registry.ModSfx.CREATURE_WIND_DEATH;
    }

    /** Flies straight at the target and rams it. */
    static class RamGoal extends Goal {
        private final Squall squall;
        private int cooldown;

        RamGoal(Squall squall) {
            this.squall = squall;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = squall.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = squall.getTarget();
            if (target == null) return;
            squall.getLookControl().setLookAt(target, 30.0f, 30.0f);
            squall.getMoveControl().setWantedPosition(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1.3);
            if (cooldown > 0) cooldown--;
            if (cooldown == 0 && squall.distanceToSqr(target) < 2.6 && squall.level() instanceof ServerLevel server) {
                squall.doHurtTarget(server, target);
                cooldown = 25;
            }
        }
    }

    /** Drifts around randomly, staying a few blocks above the ground. */
    static class DriftGoal extends Goal {
        private final Squall squall;

        DriftGoal(Squall squall) {
            this.squall = squall;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return squall.getTarget() == null && squall.getRandom().nextInt(20) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos ground = squall.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    squall.blockPosition().offset(squall.getRandom().nextInt(17) - 8, 0, squall.getRandom().nextInt(17) - 8));
            double y = ground.getY() + 2 + squall.getRandom().nextInt(5);
            squall.getMoveControl().setWantedPosition(ground.getX() + 0.5, y, ground.getZ() + 0.5, 0.8);
        }
    }
}
