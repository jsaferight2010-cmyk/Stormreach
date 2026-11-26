package com.stormreach.registry;

import com.stormreach.Stormreach;
import com.stormreach.item.StormbladeItem;
import com.stormreach.item.TempestStaffItem;
import com.stormreach.item.ThunderheadHornItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import com.stormreach.item.SquallBottleItem;
import com.stormreach.item.StormbreakerItem;
import java.util.Map;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ModItems {
    /** Everything shown in the Stormreach creative tab, in order. */
    public static final List<Item> TAB_ORDER = new ArrayList<>();

    static {
        registerBlockItems(); // blocks lead the creative tab
    }

    public static final TagKey<Item> STORM_SHARD_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("storm_shard_tool_materials"));

    /** Storm Shard tools: between iron and diamond, and they love bad weather. */
    public static final ToolMaterial STORM_SHARD_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 1100, 7.5f, 3.0f, 18, STORM_SHARD_REPAIRS);   // 6.0: a clear step past iron

    public static final ResourceKey<EquipmentAsset> STORMSHARD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("stormshard"));
    public static final ResourceKey<EquipmentAsset> GALE_WINGS_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("gale_wings"));
    /** Stormshard armor: a clear step past iron (6.0: 19 armour, toughness 1.5). A full set makes you immune to lightning. */
    public static final ArmorMaterial STORMSHARD_ARMOR = new ArmorMaterial(26,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 7, ArmorType.HELMET, 3, ArmorType.BODY, 9),
            16, com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_METAL), 1.5f, 0.05f, STORM_SHARD_REPAIRS, STORMSHARD_ASSET);

    // Materials
    public static final Item STORM_SHARD = register("storm_shard", Item::new, new Item.Properties());
    public static final Item GULL_FEATHER = register("gull_feather", Item::new, new Item.Properties());
    public static final Item SALT = register("salt", Item::new, new Item.Properties());
    public static final Item FULGURITE_SHARD = register("fulgurite_shard", Item::new, new Item.Properties());
    public static final Item STORM_CELL = register("storm_cell", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final Item TEMPEST_CORE = register("tempest_core", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    // Food (everything here comes from the Stormreach coast)
    public static final Item CRAB_CLAW = register("crab_claw", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).build()));
    public static final Item COOKED_CRAB_CLAW = register("cooked_crab_claw", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build()));
    public static final Item STORMBERRIES = register("stormberries",
            p -> new BlockItem(ModBlocks.STORMBERRY_SHRUB, p), new Item.Properties().useItemDescriptionPrefix()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.SPEED, 120, 0))));
    public static final Item STORMBERRY_PIE = register("stormberry_pie", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.3f).build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.SPEED, 400, 0))));
    public static final Item STORM_CHOWDER = register("storm_chowder", Item::new, new Item.Properties().stacksTo(1)
            .usingConvertsTo(Items.BOWL)
            .food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.9f).build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.RESISTANCE, 600, 0))));
    public static final Item THRIFT_TEA = register("thrift_tea", Item::new, new Item.Properties().stacksTo(16)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.5f).alwaysEdible().build(),
                    withEffects(Consumables.defaultDrink(), new MobEffectInstance(MobEffects.REGENERATION, 160, 0))));

    public static final Item GULL_EGG = register("gull_egg", Item::new, new Item.Properties().stacksTo(16));
    public static final Item FRIED_GULL_EGG = register("fried_gull_egg", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final Item CRAB_CAKES = register("crab_cakes", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));
    public static final Item STORMBERRY_JAM = register("stormberry_jam", Item::new, new Item.Properties().stacksTo(16)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.4f).build(),
                    withEffects(Consumables.defaultDrink(), new MobEffectInstance(MobEffects.SPEED, 600, 1))));
    public static final Item STORMBERRY_JUICE = register("stormberry_juice", Item::new, new Item.Properties().stacksTo(16)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).alwaysEdible().build(),
                    withEffects(Consumables.defaultDrink(), new MobEffectInstance(MobEffects.JUMP_BOOST, 600, 0))));
    public static final Item SEAWEED_SALAD = register("seaweed_salad", Item::new, new Item.Properties().stacksTo(1)
            .usingConvertsTo(Items.BOWL)
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.WATER_BREATHING, 900, 0))));
    public static final Item SALTED_COD = register("salted_cod", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.9f).build()));
    public static final Item SAILORS_STEW = register("sailors_stew", Item::new, new Item.Properties().stacksTo(1)
            .usingConvertsTo(Items.BOWL)
            .food(new FoodProperties.Builder().nutrition(12).saturationModifier(1.0f).build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.REGENERATION, 200, 0))));
    public static final Item STORMFIN = register("stormfin", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).build()));
    public static final Item COOKED_STORMFIN = register("cooked_stormfin", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.HASTE, 600, 0))));
    public static final Item TEMPEST_TONIC = register("tempest_tonic", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.5f).alwaysEdible().build(),
                    Consumables.defaultDrink()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(MobEffects.STRENGTH, 1200, 0),
                                    new MobEffectInstance(MobEffects.SPEED, 1200, 1),
                                    new MobEffectInstance(ModEffects.STATIC_CHARGE, 1200, 0)))).build()));
    public static final Item ROCK_CANDY = register("rock_candy", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).alwaysEdible().build(),
                    withEffects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.HASTE, 300, 1))));

    // Storm gear
    public static final Item TEMPEST_STAFF = register("tempest_staff", TempestStaffItem::new,
            new Item.Properties().durability(128).rarity(Rarity.UNCOMMON).repairable(STORM_SHARD_REPAIRS));
    public static final Item THUNDERHEAD_HORN = register("thunderhead_horn", ThunderheadHornItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item STORMSHARD_SWORD = register("stormshard_sword", StormbladeItem::new,
            new Item.Properties().sword(STORM_SHARD_MATERIAL, 3.0f, -2.4f));
    public static final Item STORMSHARD_PICKAXE = register("stormshard_pickaxe", Item::new,
            new Item.Properties().pickaxe(STORM_SHARD_MATERIAL, 1.0f, -2.8f));
    public static final Item STORMSHARD_AXE = register("stormshard_axe", Item::new,
            new Item.Properties().axe(STORM_SHARD_MATERIAL, 6.0f, -3.1f));
    public static final Item STORMSHARD_SHOVEL = register("stormshard_shovel", Item::new,
            new Item.Properties().shovel(STORM_SHARD_MATERIAL, 1.5f, -3.0f));

    public static final Item STORMSHARD_HELMET = register("stormshard_helmet", Item::new,
            new Item.Properties().humanoidArmor(STORMSHARD_ARMOR, ArmorType.HELMET));
    public static final Item STORMSHARD_CHESTPLATE = register("stormshard_chestplate", Item::new,
            new Item.Properties().humanoidArmor(STORMSHARD_ARMOR, ArmorType.CHESTPLATE));
    public static final Item STORMSHARD_LEGGINGS = register("stormshard_leggings", Item::new,
            new Item.Properties().humanoidArmor(STORMSHARD_ARMOR, ArmorType.LEGGINGS));
    public static final Item STORMSHARD_BOOTS = register("stormshard_boots", Item::new,
            new Item.Properties().humanoidArmor(STORMSHARD_ARMOR, ArmorType.BOOTS));
    /** A glider like the elytra, made from gull feathers. Storm gusts give it a boost. */
    public static final Item GALE_WINGS = register("gale_wings", Item::new, new Item.Properties().durability(300).rarity(Rarity.RARE)
            .component(DataComponents.GLIDER, Unit.INSTANCE)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                    .setEquipSound(com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_PACK)).setAsset(GALE_WINGS_ASSET).setDamageOnHurt(false).build())
            .repairable(GULL_FEATHER));
    public static final Item SQUALL_IN_A_BOTTLE = register("squall_in_a_bottle", SquallBottleItem::new,
            new Item.Properties().stacksTo(16).useCooldown(0.5f));
    public static final Item BOTTLED_SUNSHINE = register("bottled_sunshine",
            p -> new com.stormreach.item.WeatherBottleItem(com.stormreach.item.WeatherBottleItem.Weather.CLEAR, p), new Item.Properties().stacksTo(16));
    public static final Item BOTTLED_RAIN = register("bottled_rain",
            p -> new com.stormreach.item.WeatherBottleItem(com.stormreach.item.WeatherBottleItem.Weather.RAIN, p), new Item.Properties().stacksTo(16));
    public static final Item BOTTLED_THUNDER = register("bottled_thunder",
            p -> new com.stormreach.item.WeatherBottleItem(com.stormreach.item.WeatherBottleItem.Weather.THUNDER, p),
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final Item STORMBREAKER = register("stormbreaker", StormbreakerItem::new,
            new Item.Properties().durability(900).rarity(Rarity.EPIC).fireResistant()
                    .sword(STORM_SHARD_MATERIAL, 7.0f, -3.2f).repairable(TEMPEST_CORE));

    // Spawn eggs
    public static final Item CLIFF_CRAB_SPAWN_EGG = register("cliff_crab_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.CLIFF_CRAB));
    public static final Item STORM_GULL_SPAWN_EGG = register("storm_gull_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.STORM_GULL));
    public static final Item SQUALL_SPAWN_EGG = register("squall_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.SQUALL));

    public static final Item THUNDER_CRAB_SPAWN_EGG = register("thunder_crab_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.THUNDER_CRAB));
    public static final Item HARBOR_SEAL_SPAWN_EGG = register("harbor_seal_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.HARBOR_SEAL));
    public static final Item BRINE_WRAITH_SPAWN_EGG = register("brine_wraith_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.BRINE_WRAITH));
    public static final Item BALL_LIGHTNING_SPAWN_EGG = register("ball_lightning_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.BALL_LIGHTNING));
    public static final Item STORMWARDEN_SPAWN_EGG = register("stormwarden_spawn_egg", SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.STORMWARDEN));

    private ModItems() {}

    private static Consumable withEffects(Consumable.Builder builder, MobEffectInstance effect) {
        return builder.onConsume(new ApplyStatusEffectsConsumeEffect(effect)).build();
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
        TAB_ORDER.add(item);
        return item;
    }

    private static void registerBlockItems() {
        for (Block block : ModBlocks.WITH_ITEMS) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
            Item item = Registry.register(BuiltInRegistries.ITEM, key,
                    new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
            TAB_ORDER.add(item);
        }
    }

    public static void init() {
        // Loading this class registers everything above.
    }
}
