package com.stormreach.item;

import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Stormbreaker: a hammer forged around the Stormwarden's Tempest Core. Use it to slam a
 * thunderclap shockwave that damages and throws back everything around you.
 */
public class StormbreakerItem extends Item {
    public StormbreakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Vec3 c = player.position();
        server.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.5, c.z, 60, 3.0, 0.3, 3.0, 0.3);
        level.playSound(null, c.x, c.y, c.z, com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.PLAYERS, 1.5f, 1.3f);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(6.0),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 away = e.position().subtract(c).normalize().scale(1.5);
            e.hurtServer(server, server.damageSources().playerAttack(player), 8.0f);
            e.push(away.x, 0.6, away.z);
            e.needsSync = true;
        }
        if (server.isThundering()) StormWeather.strikeVisual(server, player.blockPosition());
        stack.hurtAndBreak(5, player, hand);
        player.getCooldowns().addCooldown(stack, 160);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.stormbreaker.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
