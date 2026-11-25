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
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Glimmerstag: a skittish deer of the Elderstorm Grove with softly glowing antlers. It bolts from running players. */
public class Glimmerstag extends Animal {
    public Glimmerstag(EntityType<? extends Glimmerstag> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 16).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10f, 1.3, 1.8, e -> e.isSprinting() && !e.isSpectator()));
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override public boolean isFood(ItemStack s) { return s.is(ForestRegistry.STORMFRUIT); }
    @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob o) { return ForestRegistry.GLIMMERSTAG.create(l, EntitySpawnReason.BREEDING); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.GOAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.GOAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GOAT_DEATH; }
    @Override public float getVoicePitch() { return 1.3f; }
}
