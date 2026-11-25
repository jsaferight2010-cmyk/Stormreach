package com.stormreach.wild;

import com.stormreach.forest.ForestRegistry;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Gale Sprite: a tiny wind spirit with dragonfly wings. Offer it Thistle Down and it gives you a Gale Essence and
 * keeps you company for a few minutes; while it's with you, it catches you on the wind when you fall.
 */
public class GaleSprite extends Animal {
    private UUID friend;
    private int friendTicks, giftCooldown;

    public GaleSprite(EntityType<? extends GaleSprite> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new FollowFriendGoal());
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, s -> s.is(ForestRegistry.THISTLE_DOWN), false));
        goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    public Player friend() {
        return friend == null || friendTicks <= 0 ? null : level().getPlayerByUUID(friend);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ForestRegistry.THISTLE_DOWN)) {
            if (giftCooldown > 0) return InteractionResult.PASS;
            if (level() instanceof ServerLevel server) {
                stack.consume(1, player);
                friend = player.getUUID();
                friendTicks = 3600;
                giftCooldown = 6000;
                spawnAtLocation(server, new ItemStack(WildRegistry.GALE_ESSENCE));
                server.broadcastEntityEvent(this, (byte) 18);
                playSound(ModSfx.CREATURE_SPRITE_TRILL, 1.0f, 1.4f);
                if (player instanceof ServerPlayer sp) StormAdvancements.award(sp, "gale_friend");
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server)) {
            if (random.nextFloat() < 0.1f) level().addParticle(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 0, -0.01, 0);
            return;
        }
        if (giftCooldown > 0) giftCooldown--;
        if (friendTicks > 0) friendTicks--;
        Player f = friend();
        if (f != null && distanceToSqr(f) < 20 * 20 && f.fallDistance > 2.5 && !f.onGround()) {
            f.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, true));
            if (tickCount % 4 == 0) server.sendParticles(ParticleTypes.CLOUD, f.getX(), f.getY(), f.getZ(), 3, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_SPRITE_TRILL; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_WIND_DEATH; }
    @Override public float getVoicePitch() { return 1.2f + random.nextFloat() * 0.4f; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Friend", friend == null ? "" : friend.toString());
        out.putInt("FriendTicks", friendTicks);
        out.putInt("GiftCooldown", giftCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String f = in.getStringOr("Friend", "");
        try { friend = f.isEmpty() ? null : UUID.fromString(f); } catch (IllegalArgumentException e) { friend = null; }
        friendTicks = in.getIntOr("FriendTicks", 0);
        giftCooldown = in.getIntOr("GiftCooldown", 0);
    }

    /** Keeps within a few blocks of its friend, circling at shoulder height. */
    class FollowFriendGoal extends Goal {
        FollowFriendGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { Player f = friend(); return f != null && distanceToSqr(f) > 9; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            Player f = friend();
            if (f == null) return;
            if (distanceToSqr(f) > 40 * 40) { snapTo(f.getX(), f.getY() + 1.5, f.getZ(), getYRot(), getXRot()); return; }
            double a = tickCount * 0.08;
            getMoveControl().setWantedPosition(f.getX() + Math.cos(a) * 1.8, f.getY() + 1.7, f.getZ() + Math.sin(a) * 1.8, 1.4);
        }
    }
}
