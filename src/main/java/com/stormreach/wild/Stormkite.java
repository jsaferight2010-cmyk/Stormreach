package com.stormreach.wild;

import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Stormkite: hold it (either hand) while falling and you glide - a slow descent that carries you the way you're
 * looking, faster as you build speed, steeper when you point down. Runs on both sides (the client moves the local
 * player, the server cancels fall damage and wears the kite). Storm winds push you along.
 */
public final class Stormkite {
    private static final Map<Player, Integer> GLIDING = new WeakHashMap<>();

    private Stormkite() {}

    public static boolean isGliding(Player p) {
        return GLIDING.getOrDefault(p, 0) > 0;
    }

    public static void tick(LivingEntity e) {
        if (!(e instanceof Player p)) return;
        InteractionHand hand = p.getMainHandItem().is(WildRegistry.STORMKITE) ? InteractionHand.MAIN_HAND
                : p.getOffhandItem().is(WildRegistry.STORMKITE) ? InteractionHand.OFF_HAND : null;
        Vec3 v = p.getDeltaMovement();
        if (hand == null || p.getAbilities().flying || p.onGround() || p.isInWater() || p.isPassenger() || p.isFallFlying() || v.y > -0.04) {
            GLIDING.remove(p);
            return;
        }
        int ticks = GLIDING.merge(p, 1, Integer::sum);
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        double speed = Math.min(0.62, Math.hypot(v.x, v.z) * 0.96 + 0.035);
        double sink = -0.09 + Math.min(0, look.y + 0.35) * 0.35;   // pointing down steepens the dive
        if (com.stormreach.StormreachConfig.wind && p.level().isThundering()) {
            Vec3 w = com.stormreach.weather.StormWeather.windVector().scale(0.02);
            flat = flat.scale(speed).add(w);
        } else {
            flat = flat.scale(speed);
        }
        p.setDeltaMovement(flat.x, Math.max(v.y, sink), flat.z);
        p.resetFallDistance();
        if (p.level().isClientSide()) {
            if (ticks % 3 == 0) p.level().addParticle(ParticleTypes.CLOUD, p.getX() - flat.x * 2, p.getY() + 2.2, p.getZ() - flat.z * 2, 0, 0, 0);
        } else if (ticks % 20 == 0 && p instanceof ServerPlayer sp) {
            p.getItemInHand(hand).hurtAndBreak(1, p, hand);
            if (ticks % 60 == 0) p.level().playSound(null, p.blockPosition(), ModSfx.ITEM_KITE_FLAP, SoundSource.PLAYERS, 0.6f, 0.9f + p.getRandom().nextFloat() * 0.2f);
            if (ticks >= 200) StormAdvancements.award(sp, "stormkite_glide");
        }
    }
}
