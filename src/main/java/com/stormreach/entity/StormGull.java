package com.stormreach.entity;

import com.stormreach.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Storm Gull: wheels over the coast and rides the wind. It flies faster while a storm is blowing
 * and drops Gull Feathers, used for Tempest Staffs and Thunderhead Horns.
 */
public class StormGull extends Animal {
    public StormGull(EntityType<? extends StormGull> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    private int eggTime = 6000 + (int) (Math.random() * 6000);

    @Override
    public void aiStep() {
        super.aiStep();
        // Like chickens, adult gulls lay an egg every so often.
        if (level() instanceof ServerLevel server && isAlive() && !isBaby() && --eggTime <= 0) {
            spawnAtLocation(server, com.stormreach.registry.ModItems.GULL_EGG);
            playSound(com.stormreach.registry.ModSfx.ITEM_POP, 1.0f, 1.2f);
            eggTime = 6000 + random.nextInt(6000);
        }
        Vec3 motion = getDeltaMovement();
        // Glide down gently instead of dropping like a stone.
        if (!onGround() && motion.y < 0.0) {
            setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.FISHES);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.STORM_GULL.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.stormreach.registry.ModSfx.CREATURE_SMALL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return com.stormreach.registry.ModSfx.CREATURE_SMALL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.stormreach.registry.ModSfx.CREATURE_SMALL_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.75f + this.random.nextFloat() * 0.2f;
    }
}
