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
 */
public class ShockEel extends WaterAnimal {
    private int soothed;

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
            le.addEffect(new MobEffectInstance(ModEffects.STATIC_CHARGE, 120, 0), this);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
            playSound(ModSfx.CREATURE_EEL_ZAP, 1.0f, 1.0f);
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_EEL_CRACKLE; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_EEL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_EEL_DEATH; }
}
