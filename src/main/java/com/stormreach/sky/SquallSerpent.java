package com.stormreach.sky;

import com.stormreach.anim.AnimPulse;
import com.stormreach.entity.BallLightning;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Squall Serpent: a long sky serpent of the Tempest Realm, all scales, fins and crackling cloud. It weaves through
 * the air around its prey, lunges to bite, and breathes out balls of lightning; when wounded it lashes the air into
 * a gust that throws everything back. Drops Serpent Scales and, often, its Squall Heart.
 */
public class SquallSerpent extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(),
            Component.translatable("entity.stormreach.squall_serpent"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public SquallSerpent(EntityType<? extends SquallSerpent> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 160).add(Attributes.ARMOR, 8).add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new HuntGoal());
        goalSelector.addGoal(5, new WeaveGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (t, l) -> !t.isSpectator()));
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
        if (level().isClientSide()) {
            if (random.nextFloat() < 0.35f) {
                Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-2 - random.nextDouble() * 5);
                level().addParticle(random.nextBoolean() ? ParticleTypes.CLOUD : ParticleTypes.ELECTRIC_SPARK,
                        getX() + back.x, getY() + 0.6, getZ() + back.z, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && random.nextInt(4) == 0) lashGust(level);
        return hurt;
    }

    /** A tail-lash gust that throws everything nearby back. */
    private void lashGust(ServerLevel level) {
        AnimPulse.fire(this, AnimPulse.ATTACK);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, getX(), getY() + 0.5, getZ(), 2, 1, 0.5, 1, 0);
        playSound(ModSfx.WEATHER_GUST, 2.0f, 0.7f);
        for (Entity e : level.getEntities(this, getBoundingBox().inflate(6), e -> e instanceof LivingEntity && !e.isSpectator())) {
            Vec3 away = e.position().subtract(position()).normalize();
            e.push(away.x * 1.4, 0.6, away.z * 1.4);
            e.needsSync = true;
        }
    }

    @Override public void thunderHit(ServerLevel level, LightningBolt bolt) { heal(10); }
    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public int getAmbientSoundInterval() { return 220; }
    @Override protected float getSoundVolume() { return 4.0f; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_SERPENT_ROAR; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SERPENT_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SERPENT_DEATH; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossBar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossBar.removePlayer(p); }

    /** Weaves around its prey, lunges every few seconds, and breathes lightning. */
    class HuntGoal extends Goal {
        private int t;

        HuntGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel server)) return;
            t++;
            int phase = t % 160;
            if (phase < 110) {
                double a = t * 0.05;
                double r = 9 + Math.sin(t * 0.03) * 3;
                getMoveControl().setWantedPosition(target.getX() + Math.cos(a) * r, target.getY() + 4 + Math.sin(t * 0.09) * 3,
                        target.getZ() + Math.sin(a) * r, 1.1);
            } else {
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 1, target.getZ(), 2.2);
                if (distanceToSqr(target) < 7 && phase % 6 == 0) doHurtTarget(server, target);
            }
            getLookControl().setLookAt(target, 20f, 20f);
            if (phase == 60 && hasLineOfSight(target)) {
                AnimPulse.fire(SquallSerpent.this, AnimPulse.CAST);
                playSound(ModSfx.CREATURE_SERPENT_ROAR, 4.0f, 1.3f);
                for (int i = 0; i < 3; i++) {
                    BallLightning orb = ModEntities.BALL_LIGHTNING.create(server, EntitySpawnReason.MOB_SUMMONED);
                    if (orb == null) continue;
                    Vec3 mouth = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.5)).add(0, 0.6, 0);
                    orb.snapTo(mouth.x + (i - 1) * 0.8, mouth.y, mouth.z, 0f, 0f);
                    orb.setTarget(target);
                    server.addFreshEntity(orb);
                }
            }
        }
    }

    /** With no prey: long figure-eights through the sky. */
    class WeaveGoal extends Goal {
        private Vec3 centre;
        private float a;

        WeaveGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { return getTarget() == null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (centre == null || distanceToSqr(centre) > 80 * 80) centre = position();
            a += 0.025f;
            getMoveControl().setWantedPosition(centre.x + Mth.sin(a) * 22, centre.y + Mth.sin(a * 2) * 4, centre.z + Mth.sin(a * 2) * 12, 0.8);
        }
    }
}
