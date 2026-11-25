package com.stormreach;

import com.stormreach.backpack.BackpackItem;
import com.stormreach.backpack.Backpacks;
import com.stormreach.registry.ModItems;
import com.stormreach.storm.StormBlocks;
import com.stormreach.storm.StormMobs;
import com.stormreach.tornado.WeatherItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Sorts every Stormreach item into themed creative tabs instead of one big list. */
public final class TabOrganizer {
    public enum Tab { BUILDING, NATURE, EQUIPMENT, FOOD, MATERIALS, SPAWN_EGGS, WEATHER_EVENTS, METEOROLOGY }

    private TabOrganizer() {}

    private static Map<Tab, List<Item>> sorted() {
        Map<Tab, List<Item>> out = new EnumMap<>(Tab.class);
        for (Tab t : Tab.values()) out.put(t, new ArrayList<>());
        Set<Item> skip = new HashSet<>(StormBlocks.ITEMS);
        skip.addAll(StormMobs.EGGS);
        Set<Item> events = new HashSet<>(WeatherItems.WEATHER_EVENTS);
        events.add(ModItems.BOTTLED_RAIN); events.add(ModItems.BOTTLED_SUNSHINE); events.add(ModItems.BOTTLED_THUNDER);
        Set<Item> meteo = new HashSet<>(WeatherItems.METEOROLOGY);
        meteo.add(com.stormreach.registry.ModBlocks.STORM_BAROMETER.asItem());
        meteo.add(com.stormreach.registry.ModBlocks.WIND_VANE.asItem());
        meteo.add(com.stormreach.registry.ModBlocks.STORM_COLLECTOR.asItem());
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Stormreach.MOD_ID) || skip.contains(item)) continue;
            out.get(classify(item, events, meteo)).add(item);
        }
        return out;
    }

    static Tab classify(Item item, Set<Item> events, Set<Item> meteo) {
        if (events.contains(item)) return Tab.WEATHER_EVENTS;
        if (meteo.contains(item)) return Tab.METEOROLOGY;
        if (item instanceof SpawnEggItem) return Tab.SPAWN_EGGS;
        ItemStack s = new ItemStack(item);
        if (s.has(DataComponents.FOOD)) return Tab.FOOD;
        if (item instanceof BackpackItem || Backpacks.UPGRADES.containsValue(item) || s.has(DataComponents.TOOL) || s.has(DataComponents.WEAPON)
                || s.has(DataComponents.EQUIPPABLE) || s.has(DataComponents.GLIDER) || s.isDamageableItem()) return Tab.EQUIPMENT;
        if (item instanceof BucketItem) return Tab.NATURE;
        if (item instanceof BlockItem bi) {
            BlockState b = bi.getBlock().defaultBlockState();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (b.is(BlockTags.LOGS) || b.is(BlockTags.LEAVES) || b.is(BlockTags.SAPLINGS) || b.is(BlockTags.FLOWERS) || b.is(BlockTags.DIRT)
                    || path.endsWith("_ore") || path.contains("cloud") || path.contains("grass") || path.contains("fern") || path.contains("crystal")
                    || path.equals("skyshale") || path.equals("stormstone") || path.contains("salt") || path.equals("fulgurite") || path.contains("turf")
                    || bi.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock) return Tab.NATURE;
            return Tab.BUILDING;
        }
        return Tab.MATERIALS;
    }

    public static void register() {
        Map<Tab, Supplier<ItemStack>> icons = Map.of(
                Tab.BUILDING, () -> new ItemStack(com.stormreach.registry.ModBlocks.STORMSTONE_BRICKS),
                Tab.NATURE, () -> new ItemStack(com.stormreach.realm.RealmBlocks.GALEWOOD_SAPLING),
                Tab.EQUIPMENT, () -> new ItemStack(Backpacks.BY_TIER.get(com.stormreach.sim.BackpackTier.DIAMOND)),
                Tab.FOOD, () -> new ItemStack(com.stormreach.forest.ForestRegistry.STORMFRUIT),
                Tab.MATERIALS, () -> new ItemStack(com.stormreach.realm.RealmItems.TEMPEST_EYE),
                Tab.SPAWN_EGGS, () -> new ItemStack(ModItems.STORMWARDEN_SPAWN_EGG),
                Tab.WEATHER_EVENTS, () -> new ItemStack(WeatherItems.WEATHER_EVENTS.get(4)),
                Tab.METEOROLOGY, () -> new ItemStack(WeatherItems.WEATHER_STATION));
        for (Tab t : Tab.values()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Stormreach.id(t.name().toLowerCase()),
                    FabricCreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.stormreach." + t.name().toLowerCase()))
                            .icon(icons.get(t))
                            .displayItems((params, output) -> {
                                for (Item item : sorted().get(t)) output.accept(item);
                            })
                            .build());
        }
    }
}
