package com.stormreach.item;

import com.stormreach.StormreachConfig;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Storm Shard Sword: during a thunderstorm, hits can call lightning down on the target. */
public class StormbladeItem extends Item {
    public StormbladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (target.level() instanceof ServerLevel server && server.isThundering()
                && server.canSeeSky(target.blockPosition())
                && server.getRandom().nextInt(100) < StormreachConfig.stormbladeLightningChance) {
            // A harmless-looking bolt plus direct damage, so the wielder standing next to the target is safe.
            StormWeather.strikeVisual(server, target.blockPosition());
            target.hurtServer(server, server.damageSources().lightningBolt(), 5.0f);
            target.igniteForSeconds(3.0f);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.stormshard_sword.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
