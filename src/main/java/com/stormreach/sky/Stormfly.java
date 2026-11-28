package com.stormreach.sky;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Stormfly: a glowing, blue-flashing insect that swarms in the minutes before a thunderstorm (and while one rages), so
 * a cloud of them is a forecast you can see. They drift in loose swarms and fade away after a few minutes. Catch one in
 * a glass bottle.
 */
public class Stormfly extends PathfinderMob {
    private Vec3 home;
    private int life = 2400;

    public Stormfly(EntityType<? extends Stormfly> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.35);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return random.nextInt(10) == 0; }
            @Override public boolean canContinueToUse() { return false; }
            @Override public void start() {
                if (home == null) home = position();
                getMoveControl().setWantedPosition(home.x + (random.nextDouble() - 0.5) * 8, home.y + random.nextDouble() * 3,
                        home.z + (random.nextDouble() - 0.5) * 8, 0.8);
            }
        });
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (level() instanceof ServerLevel) {
                stack.consume(1, player);
                ItemStack caught = new ItemStack(SkyRegistry.BOTTLED_STORMFLY);
                player.getInventory().placeItemBackInInventory(caught, net.minecraft.util.Prediction.SERVER_ONLY);
                playSound(SoundEvents.BOTTLE_FILL, 1.0f, 1.4f);
                discard();
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel) {
            if (--life <= 0 && !level().isThundering()) discard();
        } else if (random.nextFloat() < 0.05f) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_STORMFLY_BUZZ; }
    @Override protected float getSoundVolume() { return 0.25f; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SMALL_DEATH; }
}
