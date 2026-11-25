package com.stormreach.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Cyclone: a howling little whirlwind that roams the islands and flings whatever it touches into the air. */
public class Cyclone extends Monster {
    private int flingCooldown;

    public Cyclone(EntityType<? extends Cyclone> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount % 3 == 0) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 2, 0.4, 0.6, 0.4, 0.05);
        if (flingCooldown > 0) { flingCooldown--; return; }
        for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.4), p -> !p.isSpectator() && !p.isCreative())) {
            Vec3 side = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize().scale(0.9);
            p.push(side.x, 1.1, side.z);
            p.needsSync = true;
            com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.ATTACK);
            p.hurtServer(server, damageSources().mobAttack(this), 2.0f);
            playSound(com.stormreach.registry.ModSfx.WEATHER_GUST, 1.2f, 1.2f);
            flingCooldown = 40;
        }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.WEATHER_WIND; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.stormreach.registry.ModSfx.CREATURE_WIND_HURT; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_WIND_DEATH; }
}
