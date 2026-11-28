package com.stormreach.item;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 6.0: every Stormreach sword is a vanilla sword (same mining rules: cobwebs fast, bamboo and the like instantly,
 * leaves and plants a little faster, everything else at hand speed, 2 durability a block), except that it can also
 * break blocks in Creative. Vanilla swords can't; the player asked for Stormreach's to.
 */
public final class StormSwords {
    private StormSwords() {}

    public static Item.Properties sword(Item.Properties properties, ToolMaterial material, float attackDamage, float attackSpeed) {
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return properties.sword(material, attackDamage, attackSpeed).component(DataComponents.TOOL, new Tool(List.of(
                Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0f),
                Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
                Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5f)),
                1.0f, 2, true));
    }
}
