package com.stormreach.realm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Items and block items whose tooltip explains how they fit into the road to the Tempest Realm. */
public final class TooltipItem {
    private TooltipItem() {}

    static void lines(String descriptionId, int count, Consumer<Component> tooltip) {
        for (int i = 1; i <= count; i++) {
            tooltip.accept(Component.translatable(descriptionId + ".tooltip" + (i == 1 ? "" : String.valueOf(i))).withStyle(ChatFormatting.GRAY));
        }
    }

    public static class Plain extends Item {
        private final int count;
        public Plain(Properties properties, int count) { super(properties); this.count = count; }
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            lines(getDescriptionId(), count, tooltip);
        }
    }

    public static class Placed extends BlockItem {
        private final int count;
        public Placed(Block block, Properties properties, int count) { super(block, properties); this.count = count; }
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            lines(getDescriptionId(), count, tooltip);
        }
    }
}
