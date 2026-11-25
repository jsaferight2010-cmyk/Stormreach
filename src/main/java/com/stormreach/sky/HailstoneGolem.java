package com.stormreach.sky;

import com.stormreach.anim.AnimPulse;
import com.stormreach.registry.ModSfx;
import com.stormreach.sim.HailSize;
import com.stormreach.weather.precip.HailstoneEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Hailstone Golem: a lumbering heap of packed hail and ice around a glowing glacial core, formed in thunderstorms.
 * It pounds what it reaches and hurls fistfuls of hailstones at what it can't. Out of the storm it melts: sunshine and
 * warm places wear it down, and fire hurts it double. Drops its Glacial Core (sometimes), ice and snowballs.
 */
public class HailstoneGolem extends Monster {
    public HailstoneGolem(EntityType<? extends HailstoneGolem> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.ARMOR, 10).add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new HurlHailGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Whether it is melting here: dry weather in daylight under the sky, or a warm biome. */
    public boolean melting() {
        Level l = level();
        boolean warm = l.getBiome(blockPosition()).value().getBaseTemperature() > 1.0f;
        return warm || (!l.isRaining() && l.isBrightOutside() && l.canSeeSky(blockPosition().above()));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (tickCount % 40 == 0 && melting()) {
                hurtServer(server, damageSources().dryOut(), 2.0f);
                server.sendParticles(ParticleTypes.DRIPPING_WATER, getX(), getY() + 1.5, getZ(), 6, 0.5, 0.8, 0.5, 0);
            }
        } else if (random.nextFloat() < 0.08f) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, -0.02, 0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ? amount * 2 : amount);
    }

    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_GOLEM_CRACKLE; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_GOLEM_DEATH; }

    /** Every few seconds, at range, scoops up hail and hurls a spread of hailstones in an arc. */
    class HurlHailGoal extends Goal {
        private int cooldown = 40;

        HurlHailGoal() { setFlags(EnumSet.of(Flag.LOOK)); }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && --cooldown <= 0 && distanceToSqr(t) > 25 && distanceToSqr(t) < 18 * 18 && hasLineOfSight(t);
        }

        @Override public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            if (!(level() instanceof ServerLevel server) || t == null) return;
            AnimPulse.fire(HailstoneGolem.this, AnimPulse.CAST);
            playSound(ModSfx.CREATURE_GOLEM_THROW, 1.4f, 0.9f);
            Vec3 hand = position().add(0, 2.6, 0);
            for (int i = 0; i < 4; i++) {
                HailstoneEntity h = HailstoneEntity.drop(server, hand, i == 0 ? HailSize.GOLF_BALL : HailSize.MARBLE, 0, 0);
                if (h == null) continue;
                double dx = t.getX() - hand.x, dz = t.getZ() - hand.z, dist = Math.hypot(dx, dz);
                double flight = Math.max(8, dist / 0.7);   // ticks
                double vy = (t.getY() + 1 - hand.y) / flight + 0.5 * 0.05 * flight;
                h.setDeltaMovement(dx / flight + random.nextGaussian() * 0.05, vy, dz / flight + random.nextGaussian() * 0.05);
            }
            cooldown = 60 + random.nextInt(40);
        }
    }
}
