package com.stormreach;

import com.stormreach.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;

/** Adds the Stormfin to the fishing "fish" table, but it only bites while it is raining. */
public final class StormreachFishing {
    private StormreachFishing() {}

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!StormreachConfig.stormfinFishing || !source.isBuiltin() || !key.equals(BuiltInLootTables.FISHING_FISH)) return;
            ((FabricLootTableBuilder) tableBuilder).modifyPools(pool -> pool.add(LootItem.lootTableItem(ModItems.STORMFIN).setWeight(20)
                    .when(WeatherCheck.weather().setRaining(true))));
        });
    }
}
