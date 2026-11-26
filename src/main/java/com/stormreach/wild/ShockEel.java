package com.stormreach.wild;

import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shock Eel: a long, dark eel of storm coasts and marshes, spotted with glowing yellow. It leaves you alone - unless
 * it's thundering and you're in its water, or you hit it. Its bite carries a shock. A Tide Conch soothes them.
 * 6.0: the shock is real. A bite does electric damage (its own "shock" damage type, so lightning immunity doesn't
 * cover it) and jolts the victim stiff for a moment (Slowness III, Mining Fatigue). Hit it and it discharges into
 * everything touching the same water within 4 blocks (or just you, if you're on the bank within 2.5), at most every
 * 2 seconds. Shocks show as a burst of sparks, a jagged arc of sparks with a white flash, and a new discharge buzz.
 */
public class ShockEel extends WaterAnimal {
    private int soothed;
    private long lastDischarge = -100;
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> SHOCK =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, com.stormreach.Stormreach.id("shock"));

    /** An electric shock from this eel: damage, a stiffening jolt, sparks along the arc, and the zap. */
    void shock(ServerLevel level, LivingEntity victim, float damage) {
        victim.hurtServer(level, level.damageSources().source(SHOCK, this), damage);
        victim.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 30, 2), this);
        victim.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 40, 0), this);
        Vec3 from = position().add(0, getBbHeight() * 0.5, 0), to = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        for (int i = 0; i <= 8; i++) {   // a jagged arc from the eel to the victim
            Vec3 at = from.lerp(to, i / 8.0).add((random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.02, 0.02, 0.02, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 16, 0.3, 0.4, 0.3, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 6, 0.2, 0.3, 0.2, 0.05);   // a white flash
    }

    /** Discharges into everything in the same water nearby (or a close attacker on the bank). */
    void discharge(ServerLevel level, Entity attacker) {
        long now = level.getGameTime();
        if (now - lastDischarge < 40) return;
        lastDischarge = now;
        playSound(ModSfx.CREATURE_EEL_DISCHARGE, 1.4f, 0.9f + random.nextFloat() * 0.2f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 30, 1.2, 0.6, 1.2, 0.2);
        boolean wet = isInWater();
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0),
                e -> e != this && !(e instanceof ShockEel) && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
            boolean conducts = wet && le.isInWater() && le.distanceToSqr(this) < 16;
            boolean touching = le == attacker && le.distanceToSqr(this) < 2.5 * 2.5;
            if (conducts || touching) shock(level, le, conducts ? 4.0f : 3.0f);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive() && source.getEntity() != null) discharge(level, source.getEntity());
        return hurt;
    }

    public ShockEel(EntityType<? extends ShockEel> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.06f, 0.1f, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 14).add(Attributes.MOVEMENT_SPEED, 1.1)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 20));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (t, level) -> soothed <= 0 && level.isThundering() && t.isInWater()));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    /** A Tide Conch was blown nearby: calm for a while. */
    public void soothe(int ticks) {
        soothed = ticks;
        setTarget(null);
    }

    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (soothed > 0) soothed--;
        if (!isInWater() && onGround() && random.nextInt(12) == 0) {
            setDeltaMovement(getDeltaMovement().add((random.nextFloat() - 0.5f) * 0.3f, 0.35f, (random.nextFloat() - 0.5f) * 0.3f));
            setOnGround(false);
        }
        if (level().isClientSide() && (getTarget() != null || level().isThundering()) && random.nextFloat() < 0.08f) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity le) {
            // 6.0: the bite shocks (it used to give the victim Static Charge, which is a protective buff)
            shock(level, le, 3.0f);
            playSound(ModSfx.CREATURE_EEL_ZAP, 1.0f, 1.0f);
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_EEL_CRACKLE; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_EEL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_EEL_DEATH; }
}
