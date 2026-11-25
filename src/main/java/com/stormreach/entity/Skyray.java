package com.stormreach.entity;

import com.stormreach.StormreachConfig;
import com.stormreach.realm.RealmEntities;
import com.stormreach.realm.RealmItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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
 * Skyray: a gentle, gliding ray of the Tempest Realm. Saddle one and it carries you through the sky;
 * it flies wherever you look. Breeds with Cloudberries.
 */
public class Skyray extends Animal {
    public Skyray(EntityType<? extends Skyray> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0f));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE ? isAlive() && !isBaby() : super.canUseSlot(slot);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSaddled() && !isVehicle() && !player.isSecondaryUseActive() && !isFood(stack)) {
            if (!level().isClientSide()) player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return isSaddled() && getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player rider) {
            setYRot(rider.getYRot());
            yRotO = getYRot();
            setXRot(rider.getXRot() * 0.5f);
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            double max = StormreachConfig.skyraySpeed / 20.0;
            Vec3 look = rider.getLookAngle();
            Vec3 side = new Vec3(look.z, 0, -look.x).normalize();
            Vec3 wanted = look.scale(rider.zza * max).add(side.scale(rider.xxa * max * 0.5));
            Vec3 motion = getDeltaMovement().scale(0.85).add(wanted.scale(0.15));
            setDeltaMovement(motion);
            move(MoverType.SELF, motion);
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(RealmItems.CLOUDBERRIES);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return RealmEntities.SKYRAY.create(level, EntitySpawnReason.BREEDING);
    }

    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.CREATURE_GLIDE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.stormreach.registry.ModSfx.CREATURE_GLIDE_HURT; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_GLIDE_DEATH; }
    @Override public float getVoicePitch() { return 0.6f + random.nextFloat() * 0.2f; }
}
