package com.stormreach.storm;

import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModFluids;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

/**
 * What makes storm mobs "storm": 25% more health, immune to lightning, sparks, faster in the rain,
 * and a chance to shock what they hit. Storm bosses also call lightning on their target and summon
 * Squalls when badly hurt. This also applies the stormwater and stormlava effects to anything swimming in them.
 */
public final class StormPerks {
    private static final net.minecraft.resources.Identifier VIGOR = com.stormreach.Stormreach.id("storm_vigor");

    private StormPerks() {}

    public static void tick(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return;
        if (e.tickCount % 20 == 0) fluidEffects(level, e);
        if (!StormMobs.isStorm(e.getType()) || !StormreachConfig.stormMobPerks) return;

        if (e.tickCount == 1) {
            AttributeInstance hp = e.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null && !hp.hasModifier(VIGOR)) {
                hp.addPermanentModifier(new AttributeModifier(VIGOR, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                e.setHealth(e.getMaxHealth());
            }
        }
        if (e.tickCount % 30 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(),
                    2 + (int) e.getBbWidth(), e.getBbWidth() * 0.4, e.getBbHeight() * 0.3, e.getBbWidth() * 0.4, 0.05);
        }
        if (e.tickCount % 40 == 0 && level.isRainingAt(e.blockPosition())) {
            e.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
        }
        if (StormMobs.BOSSES.contains(e.getType()) && e.tickCount % 100 == 0) bossTick(level, e);
    }

    private static void bossTick(ServerLevel level, LivingEntity boss) {
        LivingEntity target = boss instanceof Mob mob ? mob.getTarget() : null;
        if (target == null) target = level.getNearestPlayer(boss, 48.0);
        if (target == null || (target instanceof Player p && (p.isCreative() || p.isSpectator()))) return;
        if (level.canSeeSky(target.blockPosition())) StormWeather.strike(level, target.blockPosition(), null);
        // Once below half health, call in Squalls (only once per minute).
        if (boss.getHealth() < boss.getMaxHealth() / 2 && boss.tickCount % 1200 < 100) {
            for (int i = 0; i < 2; i++) {
                Mob squall = ModEntities.SQUALL.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (squall == null) continue;
                squall.snapTo(boss.getX() + level.getRandom().nextInt(7) - 3, boss.getY() + 2, boss.getZ() + level.getRandom().nextInt(7) - 3, 0f, 0f);
                squall.setTarget(target);
                level.addFreshEntity(squall);
            }
        }
    }

    /** Called after a storm mob lands a melee hit. */
    public static void onHit(ServerLevel level, Mob attacker, Entity target) {
        if (!StormMobs.isStorm(attacker.getType()) || !StormreachConfig.stormMobPerks) return;
        if (target instanceof LivingEntity living && level.getRandom().nextInt(4) == 0 && !StormWeather.isLightningProof(living)) {
            living.hurtServer(level, level.damageSources().lightningBolt(), 2.0f);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), attacker);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 1, living.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
        }
    }

    private static void fluidEffects(ServerLevel level, LivingEntity e) {
        FluidState fluid = level.getFluidState(e.blockPosition());
        if (fluid.isEmpty()) return;
        boolean proof = StormWeather.isLightningProof(e) || StormMobs.isStorm(e.getType());
        if (fluid.getType().isSame(ModFluids.STORMWATER)) {
            e.addEffect(new MobEffectInstance(ModEffects.STATIC_CHARGE, 100, 0, true, true));
            e.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, true));
            if (!proof && StormreachConfig.stormwaterShocks && e.tickCount % 40 == 0) {
                e.hurtServer(level, level.damageSources().lightningBolt(), 1.0f);
            }
        } else if (fluid.getType().isSame(ModFluids.STORMLAVA)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
            if (!proof) e.hurtServer(level, level.damageSources().lightningBolt(), 2.0f);
        }
    }

    /** Lightning striking stormwater runs through the water and shocks everything swimming nearby. */
    public static void electrify(ServerLevel level, net.minecraft.core.BlockPos pos) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(10.0),
                x -> x.isAlive() && level.getFluidState(x.blockPosition()).getType().isSame(ModFluids.STORMWATER))) {
            if (StormWeather.isLightningProof(e) || StormMobs.isStorm(e.getType())) continue;
            e.hurtServer(level, level.damageSources().lightningBolt(), 6.0f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 0.5, e.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
        }
    }
}
