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
    // 6.0: no boss bar (it is a dangerous wild animal, not a boss)
    private long lastStormHeal = -10_000;
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

    /** Natural lightning feeds it a little (6 health, at most every 10 s); a player's lightning hurts it. */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        if (bolt.getCause() != null) { super.thunderHit(level, bolt); return; }
        long now = level.getGameTime();
        if (now - lastStormHeal >= 200) { heal(6); lastStormHeal = now; }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public int getAmbientSoundInterval() { return 200; }
    @Override protected float getSoundVolume() { return 4.0f; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_THUNDERBIRD_CRY; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_THUNDERBIRD_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_THUNDERBIRD_DEATH; }


    /**
     * 6.0: a real hunt. Circles 10 blocks up for 3-5 s, then locks onto where its prey is and stoops straight at it,
     * accelerating; talons hit when it reaches you (8 damage and a hard knock), then it climbs out and goes round
     * again. A miss (you dodged, or it overshot) also sends it back up. Hit it while it recovers.
     */
    class DiveGoal extends Goal {
        private static final int CIRCLE = 0, STOOP = 1, RECOVER = 2;
        private int state = CIRCLE, timer, circleFor = 70;
        private float angle;
        private Vec3 aim;

        DiveGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { state = CIRCLE; timer = 0; }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            timer++;
            getLookControl().setLookAt(t, 30f, 30f);
            switch (state) {
                case CIRCLE -> {
                    angle += 0.05f;
                    getMoveControl().setWantedPosition(t.getX() + Math.cos(angle) * 12, t.getY() + 10, t.getZ() + Math.sin(angle) * 12, 1.2);
                    if (timer > circleFor && hasLineOfSight(t)) {
                        state = STOOP; timer = 0;
                        aim = t.position().add(0, t.getBbHeight() * 0.5, 0).add(t.getDeltaMovement().scale(8));   // leads a running target
                        playSound(ModSfx.CREATURE_THUNDERBIRD_CRY, 4.0f, 0.8f);
                    }
                }
                case STOOP -> {
                    // track the target a little (it can still be dodged), accelerating to about 1.5 blocks a tick
                    aim = aim.lerp(t.position().add(0, t.getBbHeight() * 0.5, 0), 0.08);
                    Vec3 to = aim.subtract(position());
                    double speed = Math.min(1.5, 0.5 + timer * 0.06);
                    Vec3 want = to.normalize().scale(speed);
                    setDeltaMovement(getDeltaMovement().lerp(want, 0.35));
                    getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, 2.0);
                    if (getBoundingBox().inflate(0.8).intersects(t.getBoundingBox()) && level() instanceof ServerLevel server) {
                        AnimPulse.fire(Thunderbird.this, AnimPulse.ATTACK);
                        doHurtTarget(server, t);
                        Vec3 knock = getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.9);
                        t.push(knock.x, 0.45, knock.z);
                        t.needsSync = true;
                        recover();
                    } else if (timer > 50 || to.lengthSqr() < 1.0 || onGround() || horizontalCollision) {
                        recover();   // missed
                    }
                }
                case RECOVER -> {
                    setDeltaMovement(getDeltaMovement().add(0, 0.07, 0));
                    getMoveControl().setWantedPosition(getX() + getDeltaMovement().x * 6, t.getY() + 10, getZ() + getDeltaMovement().z * 6, 1.3);
                    if (timer > 30) { state = CIRCLE; timer = 0; circleFor = 60 + random.nextInt(40); angle = (float) Math.atan2(getZ() - t.getZ(), getX() - t.getX()); }
                }
            }
        }

        private void recover() { state = RECOVER; timer = 0; }
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
