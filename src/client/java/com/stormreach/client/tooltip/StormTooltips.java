package com.stormreach.client.tooltip;

import com.stormreach.Stormreach;
import com.stormreach.storm.StormBlocks;
import com.stormreach.storm.StormMobs;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;

/**
 * One tidy tooltip layout for every Stormreach item:
 *
 *   Name
 *   A short description, word-wrapped                 (soft storm grey)
 *   ⇧ Shift: how to use it                            (only when there is more to say)
 *   ...then whatever the game adds (attributes, durability, the mod name in advanced mode)
 *
 * Holding Shift swaps the hint for the details, as bullets. The details are the item's ".tooltip" lines; items that
 * used to print those themselves have them lifted out of the list here, so nothing is shown twice. Lines that carry
 * live values (a logbook's count, a backpack's contents) stay where the item put them.
 *
 * Descriptions are the "desc.stormreach.<id>" lang keys (tools/descriptions.py writes them, and validate.py checks that
 * every item has one). The generated Storm blocks and Storm mob eggs share a template that names the vanilla original.
 */
public final class StormTooltips {
    private static final int WIDTH = 36;
    private static final Style DESC = Style.EMPTY.withColor(TextColor.fromRgb(0xA9BCCB));
    private static final Style DETAIL = Style.EMPTY.withColor(TextColor.fromRgb(0x8E9AA8));
    private static final Style BULLET = Style.EMPTY.withColor(TextColor.fromRgb(0x6FC8E8));
    private static final Style HINT = Style.EMPTY.withColor(TextColor.fromRgb(0x5E6874)).withItalic(true);

    private StormTooltips() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> decorate(stack, lines));
    }

    static void decorate(ItemStack stack, List<Component> lines) {
        if (lines.isEmpty()) return;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!Stormreach.MOD_ID.equals(id.getNamespace())) return;
        String base = stack.getItem().getDescriptionId();

        // lift out the static ".tooltip" lines the item printed itself; they become the Shift details
        List<String> details = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            String key = base + ".tooltip" + (i == 1 ? "" : String.valueOf(i));
            if (!net.minecraft.locale.Language.getInstance().has(key)) break;
            details.add(key);
        }
        lines.removeIf(c -> c.getContents() instanceof TranslatableContents tc && tc.getArgs().length == 0 && tc.getKey().startsWith(base + ".tooltip"));
        details.removeIf(key -> I18n.get(key).contains("%s"));

        List<Component> add = new ArrayList<>();
        String desc = description(stack.getItem(), id);
        if (desc != null) for (String row : wrap(desc, WIDTH)) add.add(Component.literal(row).withStyle(DESC));
        if (!details.isEmpty()) {
            if (Minecraft.getInstance().hasShiftDown()) {
                for (String key : details) {
                    List<String> rows = wrap(I18n.get(key), WIDTH - 2);
                    for (int r = 0; r < rows.size(); r++) {
                        MutableComponent line = Component.literal(r == 0 ? "• " : "  ").withStyle(BULLET);
                        add.add(line.append(Component.literal(rows.get(r)).withStyle(DETAIL)));
                    }
                }
            } else {
                add.add(Component.translatable("tooltip.stormreach.more").withStyle(HINT));
            }
        }
        lines.addAll(1, add);
    }

    /** The item's description, or the shared one for a generated Storm block or Storm mob egg. */
    static String description(Item item, Identifier id) {
        String key = "desc.stormreach." + id.getPath();
        if (net.minecraft.locale.Language.getInstance().has(key)) return I18n.get(key);
        String path = id.getPath();
        if (item instanceof SpawnEggItem && StormMobs.EGGS.contains(item) && path.startsWith("storm_") && path.endsWith("_spawn_egg")) {
            String mob = path.substring(6, path.length() - "_spawn_egg".length());
            String name = I18n.get("entity.minecraft." + mob);
            return I18n.get("desc.stormreach.storm_egg", name);
        }
        if (item instanceof BlockItem && StormBlocks.ITEMS.contains(item) && path.startsWith("storm_")) {
            Item vanilla = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path.substring(6)));
            String name = vanilla == null ? path.substring(6).replace('_', ' ') : I18n.get(vanilla.getDescriptionId());
            return I18n.get("desc.stormreach.storm_block", name);
        }
        return null;
    }

    /** Greedy word wrap to about {@code width} characters a row. */
    static List<String> wrap(String text, int width) {
        List<String> rows = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder row = new StringBuilder();
            for (String word : para.split(" ")) {
                if (row.length() > 0 && row.length() + 1 + word.length() > width) { rows.add(row.toString()); row.setLength(0); }
                if (row.length() > 0) row.append(' ');
                row.append(word);
            }
            if (row.length() > 0) rows.add(row.toString());
        }
        return rows;
    }
}
