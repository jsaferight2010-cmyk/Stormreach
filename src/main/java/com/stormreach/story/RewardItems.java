package com.stormreach.story;

import com.stormreach.registry.ModEffects;
import com.stormreach.tornado.WeatherEvents;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Endgame and achievement reward items with real abilities. */
public final class RewardItems {
    private RewardItems() {}

    static void tip(Item item, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable(item.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }

    /** Weathervane Scepter: change the weather (Overworld) or, sneaking, call down a downburst where you look. */
    public static class Scepter extends Item {
        public Scepter(Properties p) { super(p); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (player.isSecondaryUseActive()) {
                Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(16));
                WeatherEvents.downburst(server, at);
            } else if (server.dimension() == Level.OVERWORLD) {
                boolean thunder = server.isThundering(), rain = server.isRaining();
                if (thunder) server.getServer().setWeatherParameters(12000, 0, false, false);
                else if (rain) server.getServer().setWeatherParameters(0, 6000, true, true);
                else server.getServer().setWeatherParameters(0, 6000, true, false);
                player.sendOverlayMessage(Component.translatable(thunder ? "weather.stormreach.clear" : rain ? "weather.stormreach.thunderstorm" : "weather.stormreach.rain"));
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        @Override public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) { tip(this, t); }
    }

    /** Weatherbreaker: the Maelstrom blade. Hits call lightning one time in four and hurl the target with wind. */
    public static class Weatherbreaker extends Item {
        public Weatherbreaker(Properties p) { super(p); }

        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            super.postHurtEnemy(stack, target, attacker);
            if (!(attacker.level() instanceof ServerLevel level)) return;
            Vec3 away = target.position().subtract(attacker.position()).normalize().scale(1.2);
            target.push(away.x, 0.4, away.z);
            target.needsSync = true;
            if (level.getRandom().nextInt(4) == 0) StormWeather.strike(level, target.blockPosition(), attacker instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
        }

        @Override public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) { tip(this, t); }
    }

    /** Lightning Charm: held in the offhand, you're Grounded (immune to lightning and storm winds). */
    public static class Charm extends Item {
        public Charm(Properties p) { super(p); }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
            if (slot == EquipmentSlot.OFFHAND && entity instanceof LivingEntity le && level.getGameTime() % 20 == 0) {
                le.addEffect(new MobEffectInstance(ModEffects.GROUNDED, 40, 0, true, false, true));
            }
        }

        @Override public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) { tip(this, t); }
    }

    /** Plain item with a tooltip (journal pages, trophies' items, collectibles). */
    public static class Lore extends Item {
        private final int lines;

        public Lore(Properties p, int lines) { super(p); this.lines = lines; }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
            for (int i = 1; i <= lines; i++) t.accept(Component.translatable(getDescriptionId() + ".tooltip" + (i == 1 ? "" : i)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
