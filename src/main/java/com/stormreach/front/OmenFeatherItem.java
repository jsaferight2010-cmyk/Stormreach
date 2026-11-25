package com.stormreach.front;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Omen Feather: a Storm Petrel's feather that stirs before a storm. Hold it up (use) and it tells you how long the
 * weather will hold: when the rain comes or stops, and whether thunder is on its way. Works in the Overworld.
 */
public class OmenFeatherItem extends Item {
    public OmenFeatherItem(Properties p) {
        super(p);
    }

    /** Minutes, rounded, from ticks (at least 1). */
    static long minutes(int ticks) { return Math.max(1, Math.round(ticks / 1200.0)); }

    public static Component reading(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return Component.translatable("item.stormreach.omen_feather.still");
        var w = level.getServer().getWeatherData();
        if (level.isRaining()) {
            Component rain = Component.translatable("item.stormreach.omen_feather.rain_ends", minutes(w.getRainTime()));
            if (level.isThundering()) return Component.translatable("item.stormreach.omen_feather.thunder_now", minutes(Math.min(w.getThunderTime(), w.getRainTime())));
            if (w.getThunderTime() < w.getRainTime()) return Component.translatable("item.stormreach.omen_feather.thunder_soon", minutes(w.getThunderTime())).append(" ").append(rain);
            return rain;
        }
        if (w.getClearWeatherTime() > 0) return Component.translatable("item.stormreach.omen_feather.settled", minutes(w.getClearWeatherTime()));
        return Component.translatable("item.stormreach.omen_feather.rain_coming", minutes(w.getRainTime()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            player.sendOverlayMessage(reading(server).copy().withStyle(ChatFormatting.AQUA));
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 40);
        }
        return InteractionResult.SUCCESS;
    }
}
