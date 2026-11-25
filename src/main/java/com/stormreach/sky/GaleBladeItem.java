package com.stormreach.sky;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Gale Blade: a sword forged around a Squall Heart. Every hit is an updraft that throws the target into the air;
 * right-click to ride an updraft yourself (a leap, then a slow fall), every five seconds.
 */
public class GaleBladeItem extends Item {
    public GaleBladeItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.accept(net.minecraft.network.chat.Component.translatable(getDescriptionId() + ".tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        Vec3 away = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
        target.push(away.x * 0.3, 0.6, away.z * 0.3);
        target.needsSync = true;
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.GUST, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
            server.playSound(null, target.blockPosition(), ModSfx.WEATHER_GUST, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.onGround() && !player.isInWater()) return InteractionResult.PASS;
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 0.5, 1.1, look.z * 0.5);
        player.needsSync = true;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 100);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
            server.playSound(null, player.blockPosition(), ModSfx.WEATHER_GUST, SoundSource.PLAYERS, 1.0f, 1.1f);
            player.getItemInHand(hand).hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
