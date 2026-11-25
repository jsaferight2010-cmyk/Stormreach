package com.stormreach.story;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The story: the Stranger, journal pages, the Hollow Shrine and Cyrrus, plus achievement rewards. */
public final class StoryRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();
    public static final List<Item> REWARDS = new ArrayList<>();

    public static final EntityType<Stranger> STRANGER = entity("stranger",
            EntityType.Builder.of(Stranger::new, MobCategory.MISC).sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(16));
    public static final EntityType<CyrrusBoss> CYRRUS = entity("cyrrus",
            EntityType.Builder.of(CyrrusBoss::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.62f).fireImmune().clientTrackingRange(16));

    public static final Block RELIC_PEDESTAL = block("relic_pedestal", ShrineBlocks.Pedestal::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable().sound(SoundType.LODESTONE)
                    .lightLevel(s -> s.getValue(ShrineBlocks.Pedestal.LIT) ? 12 : 2), ITEMS);
    public static final Block STORM_CONDUIT = block("storm_conduit", ShrineBlocks.Conduit::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1f, 3600000f).noLootTable().sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(s -> s.getValue(ShrineBlocks.Conduit.STRUCK) ? 15 : 0), ITEMS);
    public static final Block HOLLOW_ALTAR = block("hollow_altar", ShrineBlocks.Altar::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1f, 3600000f).noLootTable().sound(SoundType.LODESTONE).lightLevel(s -> 5), ITEMS);
    public static final Block SOVEREIGN_TROPHY = trophy("sovereign_trophy");
    public static final Block KEEPER_TROPHY = trophy("keeper_trophy");
    public static final Block CYRRUS_TROPHY = trophy("cyrrus_trophy");
    public static final Block TORNADO_TROPHY = trophy("tornado_trophy");

    public static final ResourceKey<EquipmentAsset> STORMCROWN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("stormcrown"));
    public static final ResourceKey<EquipmentAsset> CHASER_CAP_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("storm_chaser_cap"));
    public static final TagKey<Item> MAELSTROM_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("maelstrom_repair_materials"));
    public static final ArmorMaterial STORMCROWN_ARMOR = new ArmorMaterial(40, Map.of(ArmorType.HELMET, 4, ArmorType.BOOTS, 4,
            ArmorType.LEGGINGS, 4, ArmorType.CHESTPLATE, 4, ArmorType.BODY, 4), 20, com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_METAL), 3.5f, 0.1f, MAELSTROM_REPAIRS, STORMCROWN_ASSET);
    public static final ArmorMaterial CHASER_CAP_ARMOR = new ArmorMaterial(10, Map.of(ArmorType.HELMET, 1, ArmorType.BOOTS, 1,
            ArmorType.LEGGINGS, 1, ArmorType.CHESTPLATE, 1, ArmorType.BODY, 1), 15, com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_PACK), 0f, 0f, MAELSTROM_REPAIRS, CHASER_CAP_ASSET);
    /** Maelstrom: the strongest tool tier, from Cyrrus's core. */
    public static final ToolMaterial MAELSTROM = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 10f, 5f, 18, MAELSTROM_REPAIRS);

    public static final Item JOURNAL_PAGE_1 = item("journal_page_1", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), ITEMS);
    public static final Item JOURNAL_PAGE_2 = item("journal_page_2", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), ITEMS);
    public static final Item JOURNAL_PAGE_3 = item("journal_page_3", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), ITEMS);
    public static final Item JOURNAL_PAGE_4 = item("journal_page_4", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), ITEMS);
    public static final Item JOURNAL_PAGE_5 = item("journal_page_5", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), ITEMS);
    public static final Item WEATHERED_JOURNAL = item("weathered_journal", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), ITEMS);
    public static final Item HOLLOW_REMNANT = item("hollow_remnant", p -> new RewardItems.Lore(p, 2), new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ITEMS);
    public static final Item MAELSTROM_CORE = item("maelstrom_core", p -> new RewardItems.Lore(p, 1), new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant(), ITEMS);
    public static final Item STORMCROWN = item("stormcrown", p -> new RewardItems.Lore(p, 1), new Item.Properties().humanoidArmor(STORMCROWN_ARMOR, ArmorType.HELMET).rarity(Rarity.EPIC).fireResistant(), ITEMS);
    public static final Item WEATHERVANE_SCEPTER = item("weathervane_scepter", RewardItems.Scepter::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).useCooldown(60f).fireResistant(), ITEMS);
    public static final Item WEATHERBREAKER = item("weatherbreaker", RewardItems.Weatherbreaker::new, new Item.Properties().sword(MAELSTROM, 4f, -2.4f).rarity(Rarity.EPIC).fireResistant(), ITEMS);
    public static final Item STRANGER_SPAWN_EGG = item("stranger_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(STRANGER), ITEMS);
    public static final Item CYRRUS_SPAWN_EGG = item("cyrrus_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(CYRRUS), ITEMS);

    // achievement rewards
    public static final Item LIGHTNING_CHARM = item("lightning_charm", RewardItems.Charm::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), REWARDS);
    public static final Item STORM_CHASER_CAP = item("storm_chaser_cap", p -> new RewardItems.Lore(p, 1), new Item.Properties().humanoidArmor(CHASER_CAP_ARMOR, ArmorType.HELMET).rarity(Rarity.RARE), REWARDS);
    public static final Item CHASERS_BADGE = item("chasers_badge", p -> new RewardItems.Lore(p, 1), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), REWARDS);

    private StoryRegistry() {}

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p, List<Item> list) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        list.add(Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(b, new Item.Properties().setId(ik).useBlockDescriptionPrefix().rarity(Rarity.EPIC))));
        return b;
    }

    private static Block trophy(String name) {
        return block(name, Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 6f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 7), REWARDS);
    }

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties p, List<Item> list) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
        list.add(it);
        return it;
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(STRANGER, Stranger.createAttributes());
        FabricDefaultAttributeRegistry.register(CYRRUS, CyrrusBoss.createAttributes());
        SpawnPlacements.register(STRANGER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, l, r, p, rnd) -> false);
        SpawnPlacements.register(CYRRUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, l, r, p, rnd) -> false);
        StrangerDirector.init();
    }
}
