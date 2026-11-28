package com.stormreach.entity;

import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModItems;
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

/**
 * Cliff Crab: scuttles along the Stormreach shore. Breeds with Stormberries, drops Crab Claws.
 * During storms it hides in the water (it panics when lightning is near).
 */
public class CliffCrab extends Animal {
    public CliffCrab(EntityType<? extends CliffCrab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.STORMBERRIES);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CLIFF_CRAB.create(level, EntitySpawnReason.BREEDING);
    }

    /** Lightning turns a Cliff Crab into an angry Thunder Crab. */
    @Override
    public void thunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt lightning) {
        if (!com.stormreach.StormreachConfig.crabConversion || isBaby()) {
            super.thunderHit(level, lightning);
            return;
        }
        ThunderCrab crab = convertTo(ModEntities.THUNDER_CRAB, net.minecraft.world.entity.ConversionParams.single(this, false, false), c -> {});
        if (crab == null) super.thunderHit(level, lightning);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return com.stormreach.registry.ModSfx.CREATURE_GLIDE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.stormreach.registry.ModSfx.CREATURE_GLIDE_DEATH;
    }
}
