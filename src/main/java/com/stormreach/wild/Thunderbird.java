package com.stormreach.wild;

import com.stormreach.anim.AnimPulse;
import com.stormreach.registry.ModSfx;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Thunderbird: a huge storm raptor that arrives with thunderstorms. It circles high over its prey, folds its wings
 * and dives to strike with its talons, and calls lightning down on whoever it's hunting. When the storm passes, it
 * leaves. Drops Thunderbird Plumes (for the Stormkite) and Charged Scales.
 */
public class Thunderbird extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(),
            Component.translatable("entity.stormreach.thunderbird"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_6);
    private int calmTicks;
    private Vec3 pendingStrike;
    private int strikeDelay;

    public Thunderbird(EntityType<? extends Thunderbird> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 120).add(Attributes.ARMOR, 6).add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.FLYING_SPEED, 0.7).add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DiveGoal());
        goalSelector.addGoal(2, new CallLightningGoal());
        goalSelector.addGoal(5, new SoarGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, (t, l) -> !t.isSpectator()));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (!(level() instanceof ServerLevel server)) {
            if (random.nextFloat() < 0.25f) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, 0, 0);
            return;
        }
        if (pendingStrike != null && --strikeDelay <= 0) {
            StormWeather.strike(server, BlockPos.containing(pendingStrike), null);
            pendingStrike = null;
        }
        calmTicks = level().isThundering() ? 0 : calmTicks + 1;
        if (calmTicks > 1800 && getTarget() == null) {
            // the storm has passed: it climbs away into the clouds
            setDeltaMovement(getDeltaMovement().add(0, 0.08, 0));
            if (getY() > level().getMaxY() - 24 || calmTicks > 2600) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 30, 1.5, 1, 1.5, 0.05);
                discard();
            }
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        heal(20);
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public int getAmbientSoundInterval() { return 200; }
    @Override protected float getSoundVolume() { return 4.0f; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_THUNDERBIRD_CRY; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_THUNDERBIRD_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_THUNDERBIRD_DEATH; }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    /** Circles 14 blocks above its prey, then folds and dives every few seconds. */
    class DiveGoal extends Goal {
        private float angle;
        private int timer;

        DiveGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            timer++;
            angle += 0.035f;
            boolean dive = timer % 120 > 85;
            double r = dive ? 0.5 : 14.0;
            double y = t.getY() + (dive ? 0.8 : 14.0);
            getMoveControl().setWantedPosition(t.getX() + Math.cos(angle) * r, y, t.getZ() + Math.sin(angle) * r, dive ? 2.4 : 1.0);
            getLookControl().setLookAt(t, 30f, 30f);
            if (dive && distanceToSqr(t) < 6.0 && timer % 8 == 0 && level() instanceof ServerLevel server) {
                doHurtTarget(server, t);
                t.push((t.getX() - getX()) * 0.3, 0.5, (t.getZ() - getZ()) * 0.3);
                timer = 120 * (timer / 120 + 1) - 1;   // climb back out
            }
        }
    }

    /** Every 8-11 seconds, calls lightning onto its target, after a short warning screech. */
    class CallLightningGoal extends Goal {
        private int cooldown = 100;

        @Override public boolean canUse() { return getTarget() != null && --cooldown <= 0 && hasLineOfSight(getTarget()) && distanceToSqr(getTarget()) < 48 * 48; }
        @Override public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            AnimPulse.fire(Thunderbird.this, AnimPulse.CAST);
            playSound(ModSfx.CREATURE_THUNDERBIRD_CRY, 4.0f, 1.3f);
            pendingStrike = t.position().add((random.nextDouble() - 0.5) * 3, 0, (random.nextDouble() - 0.5) * 3);
            strikeDelay = 18;
            cooldown = 160 + random.nextInt(60);
        }
    }

    /** With no prey: wide, slow circles high up. */
    class SoarGoal extends Goal {
        private Vec3 centre;
        private float angle;

        SoarGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { return getTarget() == null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (centre == null) centre = position();
            angle += 0.02f;
            getMoveControl().setWantedPosition(centre.x + Math.cos(angle) * 24, centre.y + Math.sin(angle * 0.5) * 3, centre.z + Math.sin(angle) * 24, 0.8);
        }
    }
}
