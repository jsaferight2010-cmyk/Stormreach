package com.stormreach.entity;

import com.stormreach.registry.ModEntities;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * The Stormwarden: a towering storm spirit summoned with a Storm Cell on a Block of Storm Crystal
 * during a thunderstorm. It circles above you, calls lightning on its target, unleashes gust
 * shockwaves, and summons Squalls once it is badly hurt. Drops the Tempest Core.
 */
public class Stormwarden extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.stormreach.stormwarden"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int lightningCooldown = 60;
    private int gustCooldown = 140;
    private boolean summonedHelp;

    public Stormwarden(EntityType<? extends Stormwarden> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CircleTargetGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 3, 1.2, 1.0, 1.2, 0.02);
            server.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, getX(), getY() + 2.5, getZ(), 2, 0.8, 0.8, 0.8, 0.1);
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (--lightningCooldown <= 0) {
            // Strike where the target is standing; telegraph first with sparks.
            BlockPos at = target.blockPosition();
            server.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 30, 0.6, 0.1, 0.6, 0.1);
            StormWeather.strike(server, at, null);
            lightningCooldown = getHealth() < getMaxHealth() / 2 ? 50 : 80;
        }
        if (--gustCooldown <= 0) {
            gust(server);
            gustCooldown = 160;
        }
        if (!summonedHelp && getHealth() < getMaxHealth() * 0.4f) {
            summonedHelp = true;
            for (int i = 0; i < 3; i++) {
                Squall squall = ModEntities.SQUALL.create(server, EntitySpawnReason.MOB_SUMMONED);
                if (squall == null) continue;
                squall.snapTo(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4, random.nextFloat() * 360f, 0f);
                squall.setTarget(target);
                com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.CAST);
                server.addFreshEntity(squall);
            }
            playSound(com.stormreach.registry.ModSfx.CREATURE_TITAN_ROAR, 3.0f, 1.4f);
        }
    }

    /** Knocks every nearby player away from the boss. */
    private void gust(ServerLevel server) {
        com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.ATTACK);
        server.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        playSound(com.stormreach.registry.ModSfx.WEATHER_GUST, 3.0f, 0.6f);
        for (Player p : server.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(10.0), p -> !p.isSpectator() && !p.isCreative())) {
            Vec3 away = p.position().subtract(position()).normalize().scale(1.6);
            p.push(away.x, 0.7, away.z);
            p.needsSync = true;
            p.hurtServer(server, damageSources().mobAttack(this), 4.0f);
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        // Its own lightning doesn't hurt it.
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

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

    @Override
    protected SoundEvent getAmbientSound() {
        return com.stormreach.registry.ModSfx.WEATHER_WIND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return com.stormreach.registry.ModSfx.CREATURE_WIND_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.stormreach.registry.ModSfx.CREATURE_TITAN_DEATH;
    }

    /** Circles the target about six blocks up, swooping in occasionally. */
    static class CircleTargetGoal extends Goal {
        private final Stormwarden boss;
        private float angle;

        CircleTargetGoal(Stormwarden boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return boss.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target == null) return;
            angle += 0.03f;
            boolean swoop = (boss.tickCount / 100) % 3 == 2;
            double r = swoop ? 1.5 : 7.0;
            double y = target.getY() + (swoop ? 1.0 : 6.0);
            boss.getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * r, y, target.getZ() + Math.sin(angle) * r, swoop ? 1.4 : 1.0);
            boss.getLookControl().setLookAt(target, 30f, 30f);
            if (swoop && boss.distanceToSqr(target) < 9.0 && boss.tickCount % 20 == 0 && boss.level() instanceof ServerLevel server) {
                boss.doHurtTarget(server, target);
            }
        }
    }
}
