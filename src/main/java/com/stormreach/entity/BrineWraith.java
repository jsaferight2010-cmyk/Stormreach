package com.stormreach.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * Brine Wraith: a drowned sailor that walks the salt marshes and storm coasts at night.
 * 6.0: its own creature (no longer a reskinned zombie): a hunched, waterlogged sailor with its own model, animations
 * and sounds. It shambles, dragging one leg, and grabs with both hands; its touch leaves you parched (Hunger), and if
 * you're in water when it grabs you it drags you under. Every few seconds it retches a cone of brine at a target
 * 3 to 8 blocks away: Hunger, a moment of Slowness and a little damage to everything in the spray. Burns in daylight,
 * never turns into a Drowned, never turns villagers into zombies, and never carries gear.
 * Drops Salt and, rarely, a Storm Shard.
 */
public class BrineWraith extends Zombie {
    private int spitCooldown = 60;
    private int spitWindup;

    public BrineWraith(EntityType<? extends BrineWraith> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, net.minecraft.world.DifficultyInstance difficulty) {
        // a drowned sailor carries nothing (its model has no hands for gear)
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        return true;   // no zombie villagers
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.ATTACK);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 0), this);
            if (living.isInWater()) {   // the undertow: it drags you down
                living.setDeltaMovement(living.getDeltaMovement().multiply(0.3, 1, 0.3).add(0, -0.45, 0));
                living.needsSync = true;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP, living.getX(), living.getY() + 0.5, living.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            }
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextFloat() < 0.25f)   // seawater runs off it
                level().addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER, getRandomX(0.5), getY() + 0.4 + random.nextDouble() * 1.3, getRandomZ(0.5), 0, 0, 0);
            return;
        }
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        LivingEntity target = getTarget();
        if (spitWindup > 0) {
            if (--spitWindup == 0 && target != null) spit(server, target);
            return;
        }
        if (--spitCooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target)) {
            double d = distanceToSqr(target);
            if (d > 9 && d < 64) {
                com.stormreach.anim.AnimPulse.fire(this, com.stormreach.anim.AnimPulse.CAST);
                playSound(com.stormreach.registry.ModSfx.CREATURE_BRINE_SPIT, 1.0f, 0.9f + random.nextFloat() * 0.2f);
                spitWindup = 9;   // matches the retch in the animation
            }
            spitCooldown = 100 + random.nextInt(60);
        }
    }

    /** A cone of brine (about 35 degrees either side, 8 blocks): Hunger II, a moment of Slowness and 2 damage. */
    private void spit(ServerLevel level, LivingEntity target) {
        net.minecraft.world.phys.Vec3 mouth = getEyePosition();
        net.minecraft.world.phys.Vec3 dir = target.getEyePosition().subtract(mouth).normalize();
        for (int i = 0; i < 40; i++) {
            double k = 1 + random.nextDouble() * 7;
            net.minecraft.world.phys.Vec3 at = mouth.add(dir.scale(k)).add((random.nextDouble() - 0.5) * k * 0.5, (random.nextDouble() - 0.5) * k * 0.3, (random.nextDouble() - 0.5) * k * 0.5);
            level.sendParticles(random.nextBoolean() ? net.minecraft.core.particles.ParticleTypes.SPLASH : net.minecraft.core.particles.ParticleTypes.FALLING_WATER,
                    at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.02);
            if (i % 4 == 0) level.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.01);   // salt
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8), e -> e != this && !(e instanceof BrineWraith) && e.isAlive())) {
            net.minecraft.world.phys.Vec3 to = e.getBoundingBox().getCenter().subtract(mouth);
            if (to.length() > 8.5 || to.normalize().dot(dir) < 0.82 || !hasLineOfSight(e)) continue;
            if (e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) continue;
            e.hurtServer(level, damageSources().mobAttack(this), 2.0f);
            e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1), this);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), this);
        }
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.CREATURE_BRINE_AMBIENT; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return com.stormreach.registry.ModSfx.CREATURE_BRINE_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_BRINE_DEATH; }
    @Override protected net.minecraft.sounds.SoundEvent getStepSound() { return com.stormreach.registry.ModSfx.CREATURE_BRINE_STEP; }
    @Override public float getVoicePitch() { return 0.85f + random.nextFloat() * 0.2f; }
}
