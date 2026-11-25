package com.stormreach.entity;

import com.stormreach.weather.StormWeather;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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

/** Static Wisp: a flitting mote of charge. It zaps you up close and arcs sparks at you from a distance. */
public class StaticWisp extends Monster {
    public StaticWisp(EntityType<? extends StaticWisp> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ZapGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }

    @Override public void thunderHit(ServerLevel level, LightningBolt lightning) { heal(6.0f); }
    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.MAGIC_HUM; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.stormreach.registry.ModSfx.MAGIC_CHIME; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.MAGIC_SHATTER; }

    static class ZapGoal extends Goal {
        private final StaticWisp wisp;
        private int cooldown;

        ZapGoal(StaticWisp wisp) {
            this.wisp = wisp;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { return wisp.getTarget() != null && wisp.getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = wisp.getTarget();
            if (t == null || !(wisp.level() instanceof ServerLevel server)) return;
            double d = wisp.distanceToSqr(t);
            wisp.getMoveControl().setWantedPosition(t.getX(), t.getY() + (d < 16 ? 1.2 : 2.5), t.getZ(), d < 16 ? 1.3 : 1.0);
            wisp.getLookControl().setLookAt(t, 30f, 30f);
            if (--cooldown > 0) return;
            if (d < 4.5) {
                wisp.doHurtTarget(server, t);
                t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
                cooldown = 25;
            } else if (d < 100 && wisp.hasLineOfSight(t) && !StormWeather.isLightningProof(t)) {
                for (int i = 0; i <= 10; i++) {
                    double k = i / 10.0;
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, wisp.getX() + (t.getX() - wisp.getX()) * k,
                            wisp.getY() + (t.getY(0.5) - wisp.getY()) * k, wisp.getZ() + (t.getZ() - wisp.getZ()) * k, 1, 0, 0, 0, 0);
                }
                t.hurtServer(server, server.damageSources().lightningBolt(), 2.0f);
                wisp.playSound(com.stormreach.registry.ModSfx.ITEM_SPEAR_HIT, 0.8f, 2.0f);
                cooldown = 60;
            }
        }
    }
}
