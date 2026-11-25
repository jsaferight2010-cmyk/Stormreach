package com.stormreach.entity;

import com.stormreach.realm.RealmBlocks;
import com.stormreach.realm.RealmEntities;
import com.stormreach.realm.RealmItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Puffling: a sheep-like puff of living cloud. Shear it for Nimbus Clouds; while fluffy it floats down gently. */
public class Puffling extends Animal implements Shearable {
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(Puffling.class, EntityDataSerializers.BOOLEAN);
    private int regrow;

    public Puffling(EntityType<? extends Puffling> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.23);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEARED, false);
    }

    public boolean isSheared() {
        return entityData.get(SHEARED);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 m = getDeltaMovement();
        if (!isSheared() && !onGround() && m.y < -0.08) setDeltaMovement(m.x, -0.08, m.z);
        if (!level().isClientSide() && isSheared() && --regrow <= 0) entityData.set(SHEARED, false);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS)) {
            if (level() instanceof ServerLevel server && readyForShearing()) {
                shear(server, SoundSource.PLAYERS, stack);
                stack.hurtAndBreak(1, player, hand);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void shear(ServerLevel level, SoundSource source, ItemStack tool) {
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0f, 1.3f);
        entityData.set(SHEARED, true);
        regrow = 3000 + random.nextInt(3000);
        int n = 1 + random.nextInt(2);
        for (int i = 0; i < n; i++) spawnAtLocation(level, new ItemStack(RealmBlocks.NIMBUS_CLOUD), 1.0f);
    }

    @Override
    public boolean readyForShearing() {
        return isAlive() && !isSheared() && !isBaby();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return isSheared() && super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Sheared", isSheared());
        out.putInt("Regrow", regrow);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(SHEARED, in.getBooleanOr("Sheared", false));
        regrow = in.getIntOr("Regrow", 0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(RealmItems.CLOUDBERRIES) || stack.is(RealmBlocks.GALE_GRASS.asItem());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return RealmEntities.PUFFLING.create(level, EntitySpawnReason.BREEDING);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SHEEP_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SHEEP_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SHEEP_DEATH; }
    @Override public float getVoicePitch() { return 1.4f + random.nextFloat() * 0.2f; }
}
