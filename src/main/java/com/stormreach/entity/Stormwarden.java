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
 * during a thunderstorm. Drops the Tempest Core.
 * <p>6.0: the fight is now mostly on the ground, so it can be fought hand to hand. It walks you down and swings
 * (with the odd lightning strike). Only after a long ground phase (and never in the first 15 seconds) does it take to
 * the air: it circles, calls lightning and gusts for about seven seconds, then crashes down near you in a shockwave
 * and is <em>staggered</em> for a few seconds, taking extra damage. It also takes off if you are out of its reach
 * (pillared up), so towering isn't a free win. Hurt below half, its ground phases get shorter; below 40% it calls
 * Squalls once.
 */
public class Stormwarden extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.stormreach.stormwarden"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int lightningCooldown = 60;
    private int gustCooldown = 140;
    private boolean summonedHelp;
    // 6.0 phases
    private boolean airborne;
    private int modeTicks, engagedTicks, stagger, unreachableTicks, attackCooldown;
    private final PathNavigation groundNav;
    private final PathNavigation airNav;
    private final net.minecraft.world.entity.ai.control.MoveControl groundMove;
    private final net.minecraft.world.entity.ai.control.MoveControl airMove;
    public static final int FIRST_TAKEOFF = 300, GROUND_PHASE = 400, GROUND_PHASE_HURT = 260, AIR_PHASE = 140, STAGGER = 60;

    public Stormwarden(EntityType<? extends Stormwarden> type, Level level) {
        super(type, level);
        this.groundMove = new net.minecraft.world.entity.ai.control.MoveControl(this);
        this.airMove = new FlyingMoveControl(this, 10, true);
        this.groundNav = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
        FlyingPathNavigation fly = new FlyingPathNavigation(this, level);
        fly.setCanOpenDoors(false);
        this.airNav = fly;
        this.moveControl = groundMove;
        this.navigation = groundNav;
        this.setNoGravity(false);
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
        return new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
    }

    public boolean airborne() { return airborne; }
    public boolean staggered() { return stagger > 0; }

    private void takeOff(ServerLevel server) {
        airborne = true;
        modeTicks = 0;
        navigation.stop();
        moveControl = airMove;
        navigation = airNav;
        setNoGravity(true);
        setDeltaMovement(getDeltaMovement().add(0, 0.6, 0));
        playSound(com.stormreach.registry.ModSfx.WEATHER_GUST, 3.0f, 0.5f);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 30, 1.2, 0.2, 1.2, 0.08);
    }

    private void startLanding() {
        airborne = false;
        modeTicks = 0;
        navigation.stop();
        moveControl = groundMove;
        navigation = groundNav;
        setNoGravity(false);
        landing = true;
    }

    private boolean landing;

    /** Lands in a shockwave (knock-up, no long push) and is staggered: the melee window. */
    private void slam(ServerLevel server) {
        landing = false;
        stagger = STAGGER;
        com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.ATTACK);
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 3, 1.5, 0.1, 1.5, 0);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 40, 2.5, 0.1, 2.5, 0.15);
        playSound(com.stormreach.registry.ModSfx.CREATURE_TITAN_ROAR, 3.0f, 0.7f);
        for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(5.0, 2.0, 5.0), p -> !p.isSpectator() && !p.isCreative())) {
            p.push(0, 0.55, 0);
            p.needsSync = true;
            p.hurtServer(server, damageSources().mobAttack(this), 5.0f);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // staggered after a landing: the opening to hit it hard
        return super.hurtServer(level, source, stagger > 0 ? amount * 1.4f : amount);
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
        if (target == null || !target.isAlive()) {
            if (airborne) startLanding();
            return;
        }
        engagedTicks++;
        modeTicks++;
        if (stagger > 0) stagger--;
        if (landing && onGround()) slam(server);
        boolean hurt = getHealth() < getMaxHealth() / 2;
        // out of reach (pillared up or across a gap) for three seconds: go after it in the air
        boolean unreachable = !airborne && (target.getY() - getY() > 3.5 || navigation.isDone() && distanceToSqr(target) > 36);
        unreachableTicks = unreachable ? unreachableTicks + 1 : 0;
        if (!airborne && !landing && stagger == 0 && engagedTicks > FIRST_TAKEOFF
                && (modeTicks > (hurt ? GROUND_PHASE_HURT : GROUND_PHASE) || unreachableTicks > 60)) takeOff(server);
        else if (airborne && modeTicks > AIR_PHASE) startLanding();

        if (--lightningCooldown <= 0) {
            // Strike where the target is standing; telegraph first with sparks.
            BlockPos at = target.blockPosition();
            server.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 30, 0.6, 0.1, 0.6, 0.1);
            StormWeather.strike(server, at, null);
            // on the ground lightning is an occasional threat; in the air it is the main one
            lightningCooldown = airborne ? (hurt ? 35 : 50) : (hurt ? 110 : 150);
        }
        if (airborne && --gustCooldown <= 0) {
            gust(server);
            gustCooldown = 100;
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

    /** From the air: knocks nearby players back a little (6.0: shorter, so the fight can return to melee). */
    private void gust(ServerLevel server) {
        com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.ATTACK);
        server.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        playSound(com.stormreach.registry.ModSfx.WEATHER_GUST, 3.0f, 0.6f);
        for (Player p : server.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(10.0), p -> !p.isSpectator() && !p.isCreative())) {
            Vec3 away = p.position().subtract(position()).normalize().scale(0.9);
            p.push(away.x, 0.45, away.z);
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

    /** On the ground: walk the target down and swing. In the air: circle six blocks up. Staggered: stand still. */
    static class CircleTargetGoal extends Goal {
        private final Stormwarden boss;
        private float angle;
        private int repath;

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
            boss.getLookControl().setLookAt(target, 30f, 30f);
            if (boss.attackCooldown > 0) boss.attackCooldown--;
            if (boss.stagger > 0) { boss.getNavigation().stop(); return; }
            if (boss.airborne) {
                angle += 0.035f;
                boss.getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * 7, target.getY() + 6, target.getZ() + Math.sin(angle) * 7, 1.0);
                return;
            }
            if (--repath <= 0) {
                boss.getNavigation().moveTo(target, 1.15);
                repath = 10;
            }
            if (boss.attackCooldown == 0 && boss.isWithinMeleeAttackRange(target) && boss.level() instanceof ServerLevel server) {
                com.stormreach.anim.AnimPulse.fire(boss, com.stormreach.anim.AnimPulse.ATTACK);
                boss.doHurtTarget(server, target);
                boss.attackCooldown = 24;
            }
        }
    }
}
