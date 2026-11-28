package com.stormreach.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stormreach.Stormreach;
import com.stormreach.guide.Guidebooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * The two guidebooks. The Field Guide is laid out from GUIDE.md (chapters, headings, bullets, tables), wrapped to
 * the page. The Crafter's Handbook draws each recipe as a crafting grid with the real item icons (tag ingredients
 * cycle through their items), the result, and its name; hover an icon to see what it is.
 */
public class GuidebookScreen extends Screen {
    private static final int W = 300, H = 200, PAGE_W = 132;
    private final int kind;
    private final List<List<FormattedCharSequence>> textPages = new ArrayList<>();
    private final List<JsonObject> recipes = new ArrayList<>();
    private int page;
    private int ticks;

    public GuidebookScreen(int kind) {
        super(Component.translatable(kind == Guidebooks.FIELD_GUIDE ? "item.stormreach.stormreach_field_guide" : "item.stormreach.storm_crafters_handbook"));
        this.kind = kind;
    }

    private JsonObject load(String name) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(Stormreach.id("guide/" + name));
            if (res.isEmpty()) return new JsonObject();
            try (Reader r = new InputStreamReader(res.get().open())) { return JsonParser.parseReader(r).getAsJsonObject(); }
        } catch (Exception e) {
            Stormreach.LOGGER.warn("Couldn't read the guidebook {}", name, e);
            return new JsonObject();
        }
    }

    @Override
    protected void init() {
        textPages.clear();
        recipes.clear();
        if (kind == Guidebooks.FIELD_GUIDE) layoutGuide(load("guide.json"));
        else for (JsonElement e : load("crafting.json").getAsJsonArray("recipes")) recipes.add(e.getAsJsonObject());
        int l = (width - W) / 2, t = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 2)).bounds(l + 4, t + H - 20, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page = Math.min(lastPage(), page + 2)).bounds(l + W - 24, t + H - 20, 20, 16).build());
    }

    private int pages() { return kind == Guidebooks.FIELD_GUIDE ? textPages.size() : (recipes.size() + 1) / 2; }

    private int lastPage() { return Math.max(0, (pages() - 1) & ~1); }

    /** Wraps the guide into pages: each chapter starts on a new page. */
    private void layoutGuide(JsonObject guide) {
        int linesPerPage = (H - 40) / 10;
        JsonArray chapters = guide.has("chapters") ? guide.getAsJsonArray("chapters") : new JsonArray();
        // contents page
        List<FormattedCharSequence> contents = new ArrayList<>();
        contents.addAll(font.split(Component.translatable("item.stormreach.stormreach_field_guide").withStyle(s -> s.withBold(true)), PAGE_W));
        contents.add(FormattedCharSequence.EMPTY);
        int n = 1;
        for (JsonElement c : chapters)
            contents.addAll(font.split(Component.literal((n++) + ". " + c.getAsJsonObject().get("title").getAsString()), PAGE_W));
        // 6.0: the contents run on over as many pages as they need (they used to overflow the bottom of one page)
        for (int i = 0; i < contents.size(); i += linesPerPage)
            textPages.add(new ArrayList<>(contents.subList(i, Math.min(contents.size(), i + linesPerPage))));
        for (JsonElement ce : chapters) {
            JsonObject ch = ce.getAsJsonObject();
            List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.literal(ch.get("title").getAsString()).withStyle(s -> s.withBold(true).withColor(0x3A2A10)), PAGE_W));
            for (JsonElement le : ch.getAsJsonArray("lines")) {
                JsonObject l = le.getAsJsonObject();
                String style = l.get("s").getAsString(), text = l.get("t").getAsString();
                Component c = switch (style) {
                    case "h" -> Component.literal(text).withStyle(s -> s.withBold(true).withColor(0x5A3A10));
                    case "bullet" -> Component.literal("  ".repeat(l.has("d") ? l.get("d").getAsInt() : 0) + "• " + text);
                    case "table" -> Component.literal(text).withStyle(s -> s.withColor(0x2A4A6A));
                    case "note" -> Component.literal(text).withStyle(s -> s.withItalic(true).withColor(0x6A4A2A));
                    default -> Component.literal(text);
                };
                if (style.equals("h") || style.equals("p")) lines.add(FormattedCharSequence.EMPTY);
                lines.addAll(font.split(c, PAGE_W));
            }
            for (int i = 0; i < lines.size(); i += linesPerPage) textPages.add(lines.subList(i, Math.min(lines.size(), i + linesPerPage)));
        }
    }

    @Override
    public void tick() { ticks++; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        int l = (width - W) / 2, t = (height - H) / 2;
        g.fill(l - 4, t - 4, l + W + 4, t + H + 4, 0xFF4A2E18);          // leather cover
        g.fill(l, t, l + W / 2 - 1, t + H, 0xFFF3E7C9);                  // left page
        g.fill(l + W / 2 + 1, t, l + W, t + H, 0xFFF3E7C9);              // right page
        g.fill(l + W / 2 - 1, t, l + W / 2 + 1, t + H, 0xFFB89A6A);      // spine
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        int l = (width - W) / 2, t = (height - H) / 2;
        for (int side = 0; side < 2; side++) {
            int p = page + side, x = l + 8 + side * (W / 2);
            if (p >= pages()) continue;
            if (kind == Guidebooks.FIELD_GUIDE) {
                int y = t + 8;
                for (FormattedCharSequence line : textPages.get(p)) { g.text(font, line, x, y, 0xFF2A2018, false); y += 10; }
            } else {
                for (int k = 0; k < 2; k++) {
                    int r = p * 2 + k;
                    if (r < recipes.size()) recipe(g, recipes.get(r), x, t + 8 + k * 88, mx, my);
                }
            }
            g.centeredText(font, String.valueOf(p + 1), x + PAGE_W / 2, t + H - 16, 0xFF8A7050);
        }
    }

    private ItemStack stack(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        if (id.startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
            List<Item> items = new ArrayList<>();
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) items.add(h.value());
            return items.isEmpty() ? new ItemStack(Items.BARRIER) : new ItemStack(items.get((ticks / 20) % items.size()));
        }
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return new ItemStack(it);
    }

    /** 6.0: a potion bottle carrying {@code potion} (for the brewing pages). */
    private static ItemStack potion(ItemStack bottle, String potion) {
        if (potion == null || potion.isEmpty()) return bottle;
        BuiltInRegistries.POTION.get(Identifier.parse(potion)).ifPresent(h ->
                bottle.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(h)));
        return bottle;
    }

    private void recipe(GuiGraphicsExtractor g, JsonObject r, int x, int y, int mx, int my) {
        String kindName = r.get("kind").getAsString();
        ItemStack result = stack(r.get("result").getAsString());
        if (r.has("potion")) result = potion(result, r.get("potion").getAsString());
        result.setCount(Math.max(1, r.get("count").getAsInt()));
        g.text(font, Component.empty().append(result.getHoverName()), x, y, 0xFF3A2A10, false);
        g.text(font, Component.translatable("gui.stormreach.guide.kind." + kindName), x, y + 10, 0xFF8A7050, false);
        JsonArray grid = r.getAsJsonArray("grid");
        int gx = x, gy = y + 22;
        boolean brewing = kindName.equals("brewing");
        boolean single = !kindName.equals("shaped") && !kindName.equals("shapeless") && !brewing;
        for (int i = 0; i < (single ? 1 : brewing ? 2 : 9); i++) {
            int sx = gx + (i % 3) * 18, sy = gy + (i / 3) * 18;
            if (single) { sx = gx + 18; sy = gy + 18; }
            if (brewing) { sx = gx + 18; sy = gy + i * 18; }   // the ingredient over the bottle, as in a brewing stand
            slot(g, sx, sy);
            ItemStack s = stack(grid.get(i).getAsString());
            if (brewing && i == 1 && r.has("input_potion")) s = potion(s, r.get("input_potion").getAsString());
            if (!s.isEmpty()) {
                g.item(s, sx + 1, sy + 1);
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) g.setTooltipForNextFrame(font, s.getHoverName(), mx, my);
            }
        }
        // arrow and result
        g.fill(gx + 60, gy + 25, gx + 76, gy + 27, 0xFF8A7050);
        g.fill(gx + 74, gy + 23, gx + 76, gy + 29, 0xFF8A7050);
        slot(g, gx + 84, gy + 18);
        g.item(result, gx + 85, gy + 19);
        g.itemDecorations(font, result, gx + 85, gy + 19, null);
        if (mx >= gx + 84 && mx < gx + 102 && my >= gy + 18 && my < gy + 36) g.setTooltipForNextFrame(font, result.getHoverName(), mx, my);
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    public static void registerOpener() {
        Guidebooks.opener = k -> Minecraft.getInstance().gui.setScreen(new GuidebookScreen(k));
    }
}
