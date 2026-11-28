package com.stormreach.registry;

import com.stormreach.Stormreach;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;

public final class ModEffects {
    /** Static Charge: anything that hits you gets shocked back. Sparks fly off you. */
    public static final Holder<MobEffect> STATIC_CHARGE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            Stormreach.id("static_charge"), new StaticChargeEffect());
    /** Grounded: lightning can't hurt you and storm winds can't push you. */
    public static final Holder<MobEffect> GROUNDED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            Stormreach.id("grounded"), new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8C6A3E));

    /** Hidden: you're tucked inside a locker; the Hollow Keeper can't see you unless you give yourself away. */
    public static final Holder<MobEffect> HIDDEN = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            Stormreach.id("hidden"), new SimpleEffect(MobEffectCategory.NEUTRAL, 0x3A3F47));
    /** Marks a player as inside the Hollow Labyrinth (drives the adaptive music on the client; no icon). */
    public static final Holder<MobEffect> LABYRINTH = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            Stormreach.id("labyrinth"), new SimpleEffect(MobEffectCategory.NEUTRAL, 0x5B4D80));

    public static final Holder<Potion> STATIC_POTION = Registry.registerForHolder(BuiltInRegistries.POTION,
            Stormreach.id("static_charge"), new Potion("static_charge", new MobEffectInstance(STATIC_CHARGE, 3600)));
    public static final Holder<Potion> LONG_STATIC_POTION = Registry.registerForHolder(BuiltInRegistries.POTION,
            Stormreach.id("long_static_charge"), new Potion("static_charge", new MobEffectInstance(STATIC_CHARGE, 9600)));
    public static final Holder<Potion> GROUNDED_POTION = Registry.registerForHolder(BuiltInRegistries.POTION,
            Stormreach.id("grounded"), new Potion("grounded", new MobEffectInstance(GROUNDED, 3600)));
    public static final Holder<Potion> LONG_GROUNDED_POTION = Registry.registerForHolder(BuiltInRegistries.POTION,
            Stormreach.id("long_grounded"), new Potion("grounded", new MobEffectInstance(GROUNDED, 9600)));

    private ModEffects() {}

    public static void init() {
        // Loading this class registers everything above.
    }

    static class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    static class StaticChargeEffect extends MobEffect {
        StaticChargeEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x6FDCFF, com.stormreach.particle.ModParticles.STORM_SPARK);
        }

        @Override
        public void onMobHurt(ServerLevel level, LivingEntity entity, int amplifier, DamageSource source, float damage) {
            if (source.getEntity() instanceof LivingEntity attacker && attacker != entity && attacker.distanceToSqr(entity) < 36) {
                attacker.hurtServer(level, level.damageSources().lightningBolt(), 2.0f + amplifier * 2.0f);
                level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 15, 0.3, 0.5, 0.3, 0.2);
            }
        }
    }
}
