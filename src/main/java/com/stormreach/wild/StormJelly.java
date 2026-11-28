package com.stormreach.wild;

import com.stormreach.anim.AnimPulse;
import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Storm Jelly: a jellyfish that swims through the sky of the Tempest Realm, glowing and pulsing. It drifts where the
 * air takes it; touch one and it stings with a jolt of static. Drops Glowgel.
 */
public class StormJelly extends Animal {
    private int stingCooldown;

    public StormJelly(EntityType<? extends StormJelly> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 6, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.FLYING_SPEED, 0.12);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return random.nextInt(80) == 0; }
            @Override public boolean canContinueToUse() { return false; }
            @Override public void start() {
                getMoveControl().setWantedPosition(getX() + (random.nextDouble() - 0.5) * 16, getY() + (random.nextDouble() - 0.5) * 6,
                        getZ() + (random.nextDouble() - 0.5) * 16, 0.6);
            }
        });
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (stingCooldown > 0) { stingCooldown--; return; }
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.15),
                    e -> e != this && !(e instanceof StormJelly) && e.isAlive() && !e.isSpectator())) {
                e.hurtServer(server, damageSources().mobAttack(this), 1.5f);
                e.addEffect(new MobEffectInstance(ModEffects.STATIC_CHARGE, 100, 0), this);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY(0.5), e.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                AnimPulse.fire(this, AnimPulse.ATTACK);
                stingCooldown = 20;
            }
        } else if (random.nextFloat() < 0.06f) {
            level().addParticle(ParticleTypes.GLOW, getRandomX(0.7), getY() + random.nextDouble() * 0.6, getRandomZ(0.7), 0, -0.02, 0);
        }
    }

    @Override public boolean causeFallDamage(double d, float m, net.minecraft.world.damagesource.DamageSource s) { return false; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_JELLY_CHIME; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_JELLY_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_JELLY_DEATH; }
    @Override public float getVoicePitch() { return 0.8f + random.nextFloat() * 0.5f; }
}
