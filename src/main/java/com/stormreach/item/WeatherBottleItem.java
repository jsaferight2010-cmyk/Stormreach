package com.stormreach.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Uncork it to change the weather: sunshine clears the sky, rain starts a Gale, thunder starts a short Thunderhead. */
public class WeatherBottleItem extends Item {
    public enum Weather { CLEAR, RAIN, THUNDER }

    private final Weather weather;

    public WeatherBottleItem(Weather weather, Properties properties) {
        super(properties);
        this.weather = weather;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (server.dimension() != Level.OVERWORLD) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.thunderhead_horn.no_sky"));
            return InteractionResult.FAIL;
        }
        switch (weather) {
            case CLEAR -> server.getServer().setWeatherParameters(12000, 0, false, false);
            case RAIN -> server.getServer().setWeatherParameters(0, 6000, true, false);
            case THUNDER -> server.getServer().setWeatherParameters(0, 2400, true, true);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                weather == Weather.CLEAR ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("item.stormreach.weather_bottle." + weather.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.AQUA));
        stack.consume(1, player);
        if (!player.hasInfiniteMaterials()) player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        return InteractionResult.SUCCESS_SERVER;
    }
}
