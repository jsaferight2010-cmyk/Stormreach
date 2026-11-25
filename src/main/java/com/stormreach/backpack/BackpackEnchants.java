package com.stormreach.backpack;

import com.stormreach.Stormreach;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Backpack enchantments (defined as data in data/stormreach/enchantment, effects implemented here):
 * Capacity I–III (+9 slots per level), Magnetism I–II (pulls nearby drops into the pack), Organization
 * (auto-sorts on close and adds a Sort button), Soulbound (the pack stays with you when you die) and
 * Resilience (the dropped pack can't be destroyed by fire, lava, cactus, explosions or the void).
 */
public final class BackpackEnchants {
    public static final ResourceKey<Enchantment> CAPACITY = key("capacity");
    public static final ResourceKey<Enchantment> MAGNETISM = key("magnetism");
    public static final ResourceKey<Enchantment> ORGANIZATION = key("organization");
    public static final ResourceKey<Enchantment> SOULBOUND = key("soulbound");
    public static final ResourceKey<Enchantment> RESILIENCE = key("resilience");

    private BackpackEnchants() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Stormreach.id(name));
    }

    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        if (level == null || stack.isEmpty()) return 0;
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel((Holder<Enchantment>) h, stack)).orElse(0);
    }
}
