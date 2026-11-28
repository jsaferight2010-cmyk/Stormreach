package com.stormreach.wild;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tidewrack Hermit: a hermit crab living in a pearly spiral shell. Walk up to one and it pulls into its shell (and
 * takes far less damage) until you back off; sneak, or hold Kelp, and it stays out. Breed with Kelp.
 */
public class TidewrackHermit extends Animal {
    private static final EntityDataAccessor<Boolean> HIDING = SynchedEntityData.defineId(TidewrackHermit.class, EntityDataSerializers.BOOLEAN);
    private int hideTicks;

    public TidewrackHermit(EntityType<? extends TidewrackHermit> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.ARMOR, 6).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    static boolean checkSpawn(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL)) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(HIDING, false);
    }

    public boolean isHiding() { return entityData.get(HIDING); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel)) return;
        Player near = level().getNearestPlayer(this, 4.5);
        boolean threat = near != null && !near.isSpectator() && !near.isShiftKeyDown()
                && !near.getMainHandItem().is(Items.KELP) && !near.getOffhandItem().is(Items.KELP);
        if (threat || hurtTime > 0) {
            if (!isHiding()) playSound(ModSfx.CREATURE_HERMIT_HIDE, 0.8f, 1.0f);
            hideTicks = 60;
        }
        if (hideTicks > 0) {
            hideTicks--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        entityData.set(HIDING, hideTicks > 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, isHiding() ? amount * 0.35f : amount);
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.KELP); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return WildRegistry.TIDEWRACK_HERMIT.create(level, EntitySpawnReason.BREEDING); }
    @Override protected SoundEvent getAmbientSound() { return isHiding() ? null : ModSfx.CREATURE_HERMIT_CLICK; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SMALL_DEATH; }
}
