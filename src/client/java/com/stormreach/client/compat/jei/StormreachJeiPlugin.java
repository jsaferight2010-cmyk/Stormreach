package com.stormreach.client.compat.jei;

import com.stormreach.Stormreach;
import com.stormreach.front.FrontRegistry;
import com.stormreach.power.PowerRegistry;
import com.stormreach.registry.ModBlocks;
import com.stormreach.registry.ModFluids;
import com.stormreach.registry.ModItems;
import com.stormreach.sky.SkyRegistry;
import com.stormreach.storm.StormBlocks;
import com.stormreach.storm.StormMobs;
import com.stormreach.wild.WildRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

import static com.stormreach.client.compat.jei.JeiRow.Part;

/**
 * Stormreach in JEI (and in EMI or REI, which can load JEI plugins). Crafting, smelting and brewing recipes are data
 * files, so JEI shows them by itself; this plugin adds what isn't a recipe:
 * - Storm Power: what makes, stores, carries and uses SU, and how much.
 * - Water Capture: where captured water comes from and where it goes.
 * - Catching and Gathering: things you get by catching, bottling, or from living creatures and machines.
 * - Creature Drops: every Stormreach mob's drops with counts and chances (from assets/stormreach/jei/drops.tsv,
 *   generated from the loot tables).
 * - An information page for every Stormreach item: its description and how to use it.
 */
public class StormreachJeiPlugin implements IModPlugin {
    public static final IRecipeType<JeiRow> POWER = IRecipeType.create(Stormreach.MOD_ID, "power", JeiRow.class);
    public static final IRecipeType<JeiRow> WATER = IRecipeType.create(Stormreach.MOD_ID, "water", JeiRow.class);
    public static final IRecipeType<JeiRow> CATCHING = IRecipeType.create(Stormreach.MOD_ID, "catching", JeiRow.class);
    public static final IRecipeType<JeiRow> DROPS = IRecipeType.create(Stormreach.MOD_ID, "drops", JeiRow.class);

    /** 26.3 lightning rods weather like copper; the plain, unweathered one. */
    static final Item LIGHTNING_ROD = Items.LIGHTNING_ROD.weathering().pick(net.minecraft.world.level.block.WeatheringCopper.WeatherState.UNAFFECTED);

    @Override
    public Identifier getPluginUid() { return Stormreach.id("jei"); }

    @Override
    public void registerCategories(IRecipeCategoryRegistration r) {
        IGuiHelper gui = r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(
                new RowCategory(POWER, Component.translatable("jei.stormreach.power"), gui, PowerRegistry.MULTIMETER, 58),
                new RowCategory(WATER, Component.translatable("jei.stormreach.water"), gui, PowerRegistry.RAIN_BARREL, 48),
                new RowCategory(CATCHING, Component.translatable("jei.stormreach.catching"), gui, Items.GLASS_BOTTLE, 48),
                new RowCategory(DROPS, Component.translatable("jei.stormreach.drops"), gui, SkyRegistry.SQUALL_HEART, 84));
    }

    private static Component t(String key, Object... args) { return Component.translatable("jei.stormreach." + key, args); }

    @Override
    public void registerRecipes(IRecipeRegistration r) {
        r.addRecipes(POWER, power());
        r.addRecipes(WATER, water());
        r.addRecipes(CATCHING, catching());
        r.addRecipes(DROPS, drops());
        info(r);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        r.addCraftingStation(POWER, PowerRegistry.MULTIMETER, PowerRegistry.COPPER_CABLE);
        r.addCraftingStation(WATER, PowerRegistry.RAIN_BARREL, PowerRegistry.CISTERN);
        r.addCraftingStation(CATCHING, Items.GLASS_BOTTLE);
    }

    static List<JeiRow> power() {
        Part cable = Part.of(PowerRegistry.COPPER_CABLE);
        return List.of(
                JeiRow.of(List.of(Part.of(SkyRegistry.WIND_TURBINE)), List.of(cable), t("power.turbine")),
                JeiRow.of(List.of(Part.fluid(Fluids.FLOWING_WATER, 1000), Part.of(PowerRegistry.WATER_WHEEL)), List.of(cable), t("power.wheel")),
                JeiRow.of(List.of(Part.fluid(ModFluids.FLOWING_STORMWATER, 1000), Part.of(PowerRegistry.WATER_WHEEL)), List.of(cable), t("power.wheel_storm")),
                JeiRow.of(List.of(Part.of(LIGHTNING_ROD), Part.of(PowerRegistry.STORM_CAPACITOR)), List.of(Part.of(PowerRegistry.STORM_CAPACITOR)), t("power.capacitor")),
                JeiRow.of(List.of(cable), List.of(Part.of(PowerRegistry.ARC_LAMP)), t("power.lamp")),
                JeiRow.of(List.of(cable), List.of(Part.of(PowerRegistry.SHOCK_PLATE)), t("power.plate")),
                JeiRow.of(List.of(cable, Part.fluid(Fluids.WATER, 1000)), List.of(Part.of(PowerRegistry.ELECTRIC_PUMP), Part.fluid(Fluids.WATER, 250)), t("power.pump")),
                JeiRow.of(List.of(cable, Part.fluid(Fluids.WATER, 50)), List.of(Part.of(PowerRegistry.SPRINKLER), Part.of(Items.WHEAT)), t("power.sprinkler")),
                JeiRow.of(List.of(cable), List.of(Part.of(FrontRegistry.TESLA_COIL)), t("power.tesla")),
                JeiRow.of(List.of(Part.of(ModItems.STORM_CELL), Part.of(FrontRegistry.TESLA_COIL)), List.of(Part.of(FrontRegistry.TESLA_COIL)), t("power.tesla_cell")));
    }

    static List<JeiRow> water() {
        Part barrel = Part.of(PowerRegistry.RAIN_BARREL), cistern = Part.of(PowerRegistry.CISTERN);
        ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        return List.of(
                JeiRow.of(List.of(Part.of(ModItems.BOTTLED_RAIN), barrel), List.of(Part.fluid(Fluids.WATER, 25)), t("water.rain")),
                JeiRow.of(List.of(Part.of(ModItems.BOTTLED_THUNDER), barrel), List.of(Part.fluid(ModFluids.STORMWATER, 60)), t("water.thunder")),
                JeiRow.of(List.of(barrel, Part.fluid(Fluids.WATER, 250)), List.of(cistern), t("water.drain")),
                JeiRow.of(List.of(Part.of(PowerRegistry.ELECTRIC_PUMP), Part.fluid(Fluids.WATER, 1000)), List.of(Part.fluid(Fluids.WATER, 250)), t("water.pump")),
                JeiRow.of(List.of(Part.fluid(Fluids.WATER, 1000), Part.of(Items.BUCKET)), List.of(Part.of(Items.WATER_BUCKET)), t("water.bucket")),
                JeiRow.of(List.of(Part.fluid(ModFluids.STORMWATER, 1000), Part.of(Items.BUCKET)), List.of(Part.of(ModFluids.STORMWATER_BUCKET)), t("water.storm_bucket")),
                JeiRow.of(List.of(Part.fluid(Fluids.WATER, 250), Part.of(Items.GLASS_BOTTLE)), List.of(Part.of(waterBottle)), t("water.bottle")),
                JeiRow.of(List.of(Part.of(Items.WATER_BUCKET)), List.of(Part.fluid(Fluids.WATER, 1000), Part.of(Items.BUCKET)), t("water.pour")),
                JeiRow.of(List.of(Part.fluid(Fluids.WATER, 50), Part.of(PowerRegistry.SPRINKLER)), List.of(Part.of(Items.FARMLAND), Part.of(Items.WHEAT)), t("water.sprinkler")),
                JeiRow.of(List.of(Part.fluid(Fluids.FLOWING_WATER, 1000), Part.of(PowerRegistry.WATER_WHEEL)), List.of(Part.of(PowerRegistry.COPPER_CABLE)), t("water.wheel")));
    }

    static List<JeiRow> catching() {
        Part bottle = Part.of(Items.GLASS_BOTTLE);
        return List.of(
                JeiRow.of(List.of(bottle, Part.of(SkyRegistry.STORMFLY_SPAWN_EGG)), List.of(Part.of(SkyRegistry.BOTTLED_STORMFLY)), t("catch.stormfly")),
                JeiRow.of(List.of(bottle, Part.of(LIGHTNING_ROD)), List.of(Part.of(SkyRegistry.BOTTLED_LIGHTNING)), t("catch.lightning")),
                JeiRow.of(List.of(bottle, Part.of(FrontRegistry.DUST_DEVIL_SPAWN_EGG)), List.of(Part.of(FrontRegistry.BOTTLED_DUST_DEVIL)), t("catch.dust_devil")),
                JeiRow.of(List.of(Part.of(ModBlocks.STORM_COLLECTOR)), List.of(Part.of(ModItems.STORM_CELL)), t("catch.collector")),
                JeiRow.of(List.of(Part.of(com.stormreach.forest.ForestRegistry.THISTLE_DOWN), Part.of(WildRegistry.GALE_SPRITE_SPAWN_EGG)), List.of(Part.of(WildRegistry.GALE_ESSENCE)), t("catch.gale_sprite")),
                JeiRow.of(List.of(Part.of(WildRegistry.SPARKSKINK_SPAWN_EGG)), List.of(Part.of(WildRegistry.CHARGED_SCALE)), t("catch.sparkskink")),
                JeiRow.of(List.of(Part.of(WildRegistry.CLOUD_WHALE_SPAWN_EGG)), List.of(Part.of(WildRegistry.CLOUD_AMBERGRIS)), t("catch.whale")),
                JeiRow.of(List.of(Part.of(SkyRegistry.AURORA_MOTH_SPAWN_EGG)), List.of(Part.of(SkyRegistry.AURORA_DUST)), t("catch.moth")),
                JeiRow.of(List.of(Part.of(ModItems.STORM_GULL_SPAWN_EGG)), List.of(Part.of(ModItems.GULL_EGG)), t("catch.gull")),
                JeiRow.of(List.of(Part.of(Items.GLASS_BOTTLE), Part.of(PowerRegistry.RAIN_BARREL)), List.of(Part.of(PotionContents.createItemStack(Items.POTION, Potions.WATER))), t("catch.barrel_bottle")));
    }

    /** Creature drops, from the generated drops.tsv (mob, item, count, chance per line). */
    static List<JeiRow> drops() {
        java.util.Map<String, List<String[]>> byMob = new java.util.LinkedHashMap<>();
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(Stormreach.id("jei/drops.tsv"));
            if (res.isEmpty()) return List.of();
            try (java.io.BufferedReader in = new java.io.BufferedReader(res.get().openAsReader())) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] f = line.split("\t");
                    if (f.length == 4) byMob.computeIfAbsent(f[0], k -> new ArrayList<>()).add(f);
                }
            }
        } catch (Exception ex) {
            Stormreach.LOGGER.warn("Stormreach JEI: couldn't read creature drops: {}", ex.toString());
            return List.of();
        }
        List<JeiRow> rows = new ArrayList<>();
        for (var e : byMob.entrySet()) {
            Identifier mob = Identifier.parse(e.getKey());
            Item egg = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(mob.getNamespace(), mob.getPath() + "_spawn_egg"));
            if (egg == null || egg == Items.AIR) continue;
            List<Part> outs = new ArrayList<>();
            List<Component> notes = new ArrayList<>();
            for (String[] f : e.getValue()) {
                if (outs.size() >= 6) break;
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(f[1]));
                if (item == null || item == Items.AIR) continue;
                outs.add(Part.of(item));
                long pct = Math.round(Double.parseDouble(f[3]) * 100);
                notes.add(Component.translatable("jei.stormreach.drops.row", Component.translatable(item.getDescriptionId()), f[2], pct < 1 ? "<1" : String.valueOf(pct)));
            }
            if (!outs.isEmpty()) rows.add(new JeiRow(List.of(Part.of(egg)), outs, notes));
        }
        return rows;
    }

    /** An information page for every Stormreach item: its description, then how to use it. */
    static void info(IRecipeRegistration r) {
        Language lang = Language.getInstance();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!Stormreach.MOD_ID.equals(id.getNamespace())) continue;
            List<Component> page = new ArrayList<>();
            String path = id.getPath();
            if (lang.has("desc.stormreach." + path)) {
                page.add(Component.translatable("desc.stormreach." + path));
            } else if (StormMobs.EGGS.contains(item) && path.startsWith("storm_") && path.endsWith("_spawn_egg")) {
                page.add(Component.translatable("desc.stormreach.storm_egg", Component.translatable("entity.minecraft." + path.substring(6, path.length() - 10))));
            } else if (item instanceof BlockItem && StormBlocks.ITEMS.contains(item) && path.startsWith("storm_")) {
                Item vanilla = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path.substring(6)));
                page.add(Component.translatable("desc.stormreach.storm_block",
                        vanilla == null ? Component.literal(path.substring(6)) : Component.translatable(vanilla.getDescriptionId())));
            }
            String base = item.getDescriptionId();
            for (int i = 1; i <= 9; i++) {
                String key = base + ".tooltip" + (i == 1 ? "" : String.valueOf(i));
                if (!lang.has(key)) break;
                if (!lang.getOrDefault(key).contains("%s")) page.add(Component.literal("• ").append(Component.translatable(key)));
            }
            if (!page.isEmpty()) r.addItemStackInfo(new ItemStack(item), page.toArray(Component[]::new));
        }
    }
}
