package com.stormreach.forest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Thistlehop: a puffball critter that bounces through the undergrowth, shedding thistle down. */
public class Thistlehop extends Animal {
    private int shed = 4000 + (int) (Math.random() * 4000);

    public Thistlehop(EntityType<? extends Thistlehop> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // it hops rather than walks
        if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 0.0004 && random.nextInt(6) == 0) {
            setDeltaMovement(getDeltaMovement().add(0, 0.38, 0));
        }
        if (level() instanceof ServerLevel server && --shed <= 0 && !isBaby()) {
            spawnAtLocation(server, ForestRegistry.THISTLE_DOWN);
            shed = 4000 + random.nextInt(4000);
        }
    }

    @Override public boolean isFood(ItemStack s) { return s.is(ForestRegistry.SPARKFERN.asItem()); }
    @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob o) { return ForestRegistry.THISTLEHOP.create(l, EntitySpawnReason.BREEDING); }
    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.CREATURE_SMALL_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return com.stormreach.registry.ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_SMALL_DEATH; }
    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
}
