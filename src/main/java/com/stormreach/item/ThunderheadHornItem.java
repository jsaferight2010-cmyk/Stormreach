package com.stormreach.item;

import com.stormreach.StormreachConfig;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** One blast summons a Thunderhead: a thunderstorm for a few minutes. The horn cracks afterwards. */
public class ThunderheadHornItem extends Item {
    public ThunderheadHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (server.dimension() != Level.OVERWORLD) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.thunderhead_horn.no_sky"));
            return InteractionResult.FAIL;
        }
        int ticks = StormreachConfig.hornStormMinutes * 60 * 20;
        server.getServer().setWeatherParameters(0, ticks, true, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), com.stormreach.registry.ModSfx.EVENT_HORN,
                SoundSource.PLAYERS, 2.0f, 0.8f);
        player.sendOverlayMessage(Component.translatable("item.stormreach.thunderhead_horn.used").withStyle(ChatFormatting.AQUA));
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.thunderhead_horn.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
