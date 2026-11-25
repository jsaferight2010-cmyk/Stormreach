package com.stormreach.entity;

import com.stormreach.StormreachConfig;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The Tempest Sovereign: the storm's crowned ruler, called down to the altar of a Tempest Spire.
 * Phase 1: lightning and storm orbs. Phase 2 (below 60%): Squalls, gusts and lightning rings.
 * Phase 3 (below 25%): enraged dives and a vortex that drags you in. Drops one Tempest Eye.
 */
public class TempestSovereign extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(),
            Component.translatable("entity.stormreach.tempest_sovereign"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_12);
    private int lightningCooldown = 60;
    private int orbCooldown = 120;
    private int gustCooldown = 160;
    private int vortexCooldown = 200;
    private int vortexTicks;
    private int phase = 1;
    private BlockPos home;

    public TempestSovereign(EntityType<? extends TempestSovereign> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 250;
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(StormreachConfig.sovereignHealth);
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setHome(BlockPos pos) {
        this.home = pos;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SovereignFlightGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    public int phase() {
        return phase;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.0, getZ(), 3, 1.0, 1.4, 1.0, 0.02);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 3.2, getZ(), 2, 0.9, 0.3, 0.9, 0.1);
        }
        updatePhase(server);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (--lightningCooldown <= 0) {
            BlockPos at = target.blockPosition();
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 40, 0.8, 0.1, 0.8, 0.1);
            StormWeather.strike(server, at, null);
            if (phase >= 2) {
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    StormWeather.strike(server, StormWeather.surfaceBelow(server, at.offset((int) (Math.cos(a) * 4), 3, (int) (Math.sin(a) * 4)), 12), null);
                }
            }
            lightningCooldown = phase == 1 ? 70 : phase == 2 ? 50 : 32;
        }
        if (--orbCooldown <= 0 && phase < 3) {
            List<BallLightning> orbs = server.getEntitiesOfClass(BallLightning.class, getBoundingBox().inflate(32));
            if (orbs.size() < 4) {
                BallLightning orb = ModEntities.BALL_LIGHTNING.create(server, EntitySpawnReason.MOB_SUMMONED);
                if (orb != null) {
                    orb.snapTo(getX(), getY() + 2, getZ(), 0f, 0f);
                    orb.setTarget(target);
                    server.addFreshEntity(orb);
                    playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0f, 1.5f);
                }
            }
            orbCooldown = 140;
        }
        if (phase >= 2 && --gustCooldown <= 0) {
            gust(server, 12.0, 1.8);
            gustCooldown = phase == 2 ? 140 : 100;
        }
        if (phase == 3) {
            if (vortexTicks > 0) {
                vortexTicks--;
                for (Player p : nearbyPlayers(server, 18.0)) {
                    Vec3 in = position().subtract(p.position()).normalize().scale(0.18);
                    p.push(in.x, in.y * 0.5 + 0.02, in.z);
                    p.needsSync = true;
                }
                server.sendParticles(ParticleTypes.GUST, getX(), getY() + 1, getZ(), 4, 4, 1, 4, 0.0);
                if (vortexTicks == 0) gust(server, 8.0, 2.6);
            } else if (--vortexCooldown <= 0) {
                vortexTicks = 40;
                vortexCooldown = 220;
                playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0f, 0.7f);
            }
        }
    }

    private void updatePhase(ServerLevel server) {
        float frac = getHealth() / getMaxHealth();
        int now = frac < 0.25f ? 3 : frac < 0.6f ? 2 : 1;
        if (now <= phase) return;
        phase = now;
        LivingEntity target = getTarget();
        int squalls = phase == 2 ? 3 : 2;
        for (int i = 0; i < squalls; i++) {
            Squall squall = ModEntities.SQUALL.create(server, EntitySpawnReason.MOB_SUMMONED);
            if (squall == null) continue;
            squall.snapTo(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4, random.nextFloat() * 360f, 0f);
            if (target != null) squall.setTarget(target);
            server.addFreshEntity(squall);
        }
        if (phase == 3) {
            addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 600, 1));
            addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 600, 1));
        }
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0f, phase == 2 ? 1.2f : 0.8f);
        for (ServerPlayer p : server.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)) {
            p.sendSystemMessage(Component.translatable("entity.stormreach.tempest_sovereign.phase" + phase)
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }

    private List<Player> nearbyPlayers(ServerLevel server, double r) {
        return server.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(r), p -> !p.isSpectator() && !p.isCreative());
    }

    private void gust(ServerLevel server, double radius, double strength) {
        server.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 3.0f, 0.5f);
        for (Player p : nearbyPlayers(server, radius)) {
            Vec3 away = p.position().subtract(position()).normalize().scale(strength);
            p.push(away.x, 0.8, away.z);
            p.needsSync = true;
            p.hurtServer(server, damageSources().mobAttack(this), 5.0f);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 6; i++) {
                StormWeather.strikeVisual(server, StormWeather.surfaceBelow(server, blockPosition().offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6), 32));
            }
            for (ServerPlayer p : server.getPlayers(p -> p.distanceToSqr(this) < 96 * 96)) {
                p.sendSystemMessage(Component.translatable("entity.stormreach.tempest_sovereign.defeated")
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
            }
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        heal(2.0f); // the storm feeds it
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
    protected boolean canRide(Entity vehicle) {
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
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Phase", phase);
        if (home != null) {
            out.putInt("HomeX", home.getX());
            out.putInt("HomeY", home.getY());
            out.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        phase = in.getIntOr("Phase", 1);
        int hy = in.getIntOr("HomeY", Integer.MIN_VALUE);
        if (hy != Integer.MIN_VALUE) home = new BlockPos(in.getIntOr("HomeX", 0), hy, in.getIntOr("HomeZ", 0));
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BREEZE_IDLE_AIR; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WITHER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    /** Circles high around its target, keeps near its spire, and dives in phase 3. */
    static class SovereignFlightGoal extends Goal {
        private final TempestSovereign boss;
        private float angle;

        SovereignFlightGoal(TempestSovereign boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { return true; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target == null) {
                if (boss.home != null && boss.distanceToSqr(Vec3.atCenterOf(boss.home)) > 36) {
                    boss.getMoveControl().setWantedPosition(boss.home.getX() + 0.5, boss.home.getY() + 6, boss.home.getZ() + 0.5, 0.8);
                }
                return;
            }
            angle += boss.phase == 3 ? 0.05f : 0.03f;
            boolean dive = boss.phase == 3 && (boss.tickCount / 60) % 2 == 1;
            boolean swoop = !dive && (boss.tickCount / 100) % 4 == 3;
            double r = dive ? 0.5 : swoop ? 2.0 : 9.0;
            double y = target.getY() + (dive ? 0.5 : swoop ? 1.5 : 7.0);
            double cx = target.getX(), cz = target.getZ();
            if (boss.home != null && target.distanceToSqr(Vec3.atCenterOf(boss.home)) > 56 * 56) {
                cx = boss.home.getX(); cz = boss.home.getZ(); y = boss.home.getY() + 8;
            }
            boss.getMoveControl().setWantedPosition(cx + Math.cos(angle) * r, y, cz + Math.sin(angle) * r, dive ? 1.8 : swoop ? 1.4 : 1.0);
            boss.getLookControl().setLookAt(target, 30f, 30f);
            if ((dive || swoop) && boss.distanceToSqr(target) < 12.0 && boss.tickCount % 15 == 0 && boss.level() instanceof ServerLevel server) {
                boss.doHurtTarget(server, target);
            }
        }
    }
}
