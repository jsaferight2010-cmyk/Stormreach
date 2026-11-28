package com.stormreach.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/** Stormhawk: a raptor of the storm sky. It circles high above its prey, then dives talons-first. */
public class Stormhawk extends Monster {
    public Stormhawk(EntityType<? extends Stormhawk> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 15, true);
        this.setNoGravity(true);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DiveGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.CREATURE_SMALL_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.stormreach.registry.ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_SMALL_DEATH; }
    @Override public float getVoicePitch() { return 0.7f; }

    static class DiveGoal extends Goal {
        private final Stormhawk hawk;
        private float angle;

        DiveGoal(Stormhawk hawk) {
            this.hawk = hawk;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { return hawk.getTarget() != null && hawk.getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = hawk.getTarget();
            if (t == null) return;
            angle += 0.06f;
            boolean dive = (hawk.tickCount / 50) % 3 == 2;
            double r = dive ? 0.3 : 6.0;
            double y = t.getY() + (dive ? 0.6 : 8.0);
            hawk.getMoveControl().setWantedPosition(t.getX() + Math.cos(angle) * r, y, t.getZ() + Math.sin(angle) * r, dive ? 2.0 : 1.0);
            hawk.getLookControl().setLookAt(t, 30f, 30f);
            if (dive && hawk.distanceToSqr(t) < 3.0 && hawk.tickCount % 10 == 0 && hawk.level() instanceof ServerLevel server) {
                hawk.doHurtTarget(server, t);
            }
        }
    }
}
