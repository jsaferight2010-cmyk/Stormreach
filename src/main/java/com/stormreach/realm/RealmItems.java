package com.stormreach.realm;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Every item added in 4.0. */
public final class RealmItems {
    public static final List<Item> TAB_ORDER = new ArrayList<>();
    /** Block items that get a tooltip, with how many lines. */
    private static final Map<Block, Integer> BLOCK_TOOLTIPS = Map.of(
            RealmBlocks.SOVEREIGN_ALTAR, 2, RealmBlocks.STORMGATE_FRAME, 3, RealmBlocks.NIMBUS_CLOUD, 1,
            RealmBlocks.THUNDERCLOUD, 1, RealmBlocks.GALE_CLOUD, 1);

    static {
        for (Block block : RealmBlocks.WITH_ITEMS) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
            Item.Properties props = new Item.Properties().setId(key).useBlockDescriptionPrefix();
            if (block == RealmBlocks.SOVEREIGN_ALTAR) props.rarity(Rarity.EPIC);
            if (block == RealmBlocks.STORMGATE_FRAME) props.rarity(Rarity.RARE);
            Integer lines = BLOCK_TOOLTIPS.get(block);
            Item item = Registry.register(BuiltInRegistries.ITEM, key, lines == null ? new BlockItem(block, props) : new TooltipItem.Placed(block, props, lines));
            TAB_ORDER.add(item);
        }
    }

    public static final TagKey<Item> VOLTITE_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("voltite_tool_materials"));
    public static final TagKey<Item> AURORITE_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("aurorite_tool_materials"));
    public static final TagKey<Item> CLOUDSTEP_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("cloudstep_repair_materials"));

    /** Voltite: the realm's working metal, a step above iron. */
    public static final ToolMaterial VOLTITE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1700, 8.5f, 3.5f, 16, VOLTITE_REPAIRS);   // 6.0: diamond tier and a bit more
    /** Aurorite: storm-gem that outclasses diamond. */
    public static final ToolMaterial AURORITE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2400, 10.0f, 4.5f, 18, AURORITE_REPAIRS);   // 6.0: past netherite

    public static final ResourceKey<EquipmentAsset> VOLTITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("voltite"));
    public static final ResourceKey<EquipmentAsset> AURORITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("aurorite"));
    public static final ResourceKey<EquipmentAsset> CLOUDSTEP_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("cloudstep"));
    public static final ArmorMaterial VOLTITE_ARMOR = new ArmorMaterial(33,      // 6.0: matches diamond, plus its set bonus
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
            14, com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_METAL), 2.0f, 0.0f, VOLTITE_REPAIRS, VOLTITE_ASSET);
    public static final ArmorMaterial AURORITE_ARMOR = new ArmorMaterial(40,      // 6.0: a little past netherite
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 12),
            18, com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_METAL), 3.5f, 0.1f, AURORITE_REPAIRS, AURORITE_ASSET);
    public static final ArmorMaterial CLOUDSTEP_ARMOR = new ArmorMaterial(15,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 3, ArmorType.CHESTPLATE, 4, ArmorType.HELMET, 2, ArmorType.BODY, 4),
            20, com.stormreach.registry.ModSfx.holder(com.stormreach.registry.ModSfx.ITEM_EQUIP_PACK), 0.0f, 0.0f, CLOUDSTEP_REPAIRS, CLOUDSTEP_ASSET);

    // ---------- The road to the realm ----------
    /** Creative-only shortcut to the realm and back (no recipe, no loot). */
    public static final Item REALM_PASS = register("realm_pass", RealmPassItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final Item TEMPEST_SIGIL = register("tempest_sigil", p -> new TooltipItem.Plain(p, 3),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final Item TEMPEST_EYE = register("tempest_eye", p -> new TooltipItem.Plain(p, 3),
            new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // ---------- Materials ----------
    public static final Item RAW_VOLTITE = register("raw_voltite", Item::new, new Item.Properties());
    public static final Item VOLTITE_INGOT = register("voltite_ingot", Item::new, new Item.Properties());
    public static final Item AURORITE_GEM = register("aurorite", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item STORMHAWK_FEATHER = register("stormhawk_feather", Item::new, new Item.Properties());
    public static final Item ION_SHARD = register("ion_shard", p -> new TooltipItem.Plain(p, 1), new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).alwaysEdible().build(),
                    Consumables.defaultFood().consumeSeconds(0.8f).sound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(com.stormreach.registry.ModSfx.MAGIC_CHIME))
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 0))).build()));

    // ---------- Food ----------
    public static final Item CLOUDBERRIES = register("cloudberries", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build(),
                    withEffect(Consumables.defaultFood(), new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0))));
    public static final Item CLOUDBERRY_TART = register("cloudberry_tart", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6f).build(),
                    withEffect(Consumables.defaultFood(), new MobEffectInstance(MobEffects.JUMP_BOOST, 600, 1))));
    public static final Item RAW_SKYRAY = register("raw_skyray", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Item COOKED_SKYRAY = register("cooked_skyray", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));
    public static final Item STORM_NECTAR = register("storm_nectar", Item::new, new Item.Properties().stacksTo(16)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).alwaysEdible().build(),
                    Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                            new MobEffectInstance(MobEffects.REGENERATION, 200, 0),
                            new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0)))).build()));
    public static final Item GALE_BREAD = register("gale_bread", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build()));

    // ---------- Voltite gear ----------
    public static final Item VOLTITE_SWORD = register("voltite_sword", p -> new VoltiteSwordItem(p, 3.0f), com.stormreach.item.StormSwords.sword(new Item.Properties(), VOLTITE, 3.0f, -2.4f));
    public static final Item VOLTITE_PICKAXE = register("voltite_pickaxe", Item::new, new Item.Properties().pickaxe(VOLTITE, 1.0f, -2.8f));
    public static final Item VOLTITE_AXE = register("voltite_axe", Item::new, new Item.Properties().axe(VOLTITE, 6.0f, -3.1f));
    public static final Item VOLTITE_SHOVEL = register("voltite_shovel", Item::new, new Item.Properties().shovel(VOLTITE, 1.5f, -3.0f));
    public static final Item VOLTITE_HOE = register("voltite_hoe", Item::new, new Item.Properties().hoe(VOLTITE, -2.0f, -1.0f));
    public static final Item VOLTITE_HELMET = register("voltite_helmet", Item::new, new Item.Properties().humanoidArmor(VOLTITE_ARMOR, ArmorType.HELMET));
    public static final Item VOLTITE_CHESTPLATE = register("voltite_chestplate", Item::new, new Item.Properties().humanoidArmor(VOLTITE_ARMOR, ArmorType.CHESTPLATE));
    public static final Item VOLTITE_LEGGINGS = register("voltite_leggings", Item::new, new Item.Properties().humanoidArmor(VOLTITE_ARMOR, ArmorType.LEGGINGS));
    public static final Item VOLTITE_BOOTS = register("voltite_boots", Item::new, new Item.Properties().humanoidArmor(VOLTITE_ARMOR, ArmorType.BOOTS));

    // ---------- Aurorite gear ----------
    public static final Item AURORITE_SWORD = register("aurorite_sword", p -> new VoltiteSwordItem(p, 6.0f), com.stormreach.item.StormSwords.sword(new Item.Properties(), AURORITE, 3.0f, -2.4f).fireResistant());
    public static final Item AURORITE_PICKAXE = register("aurorite_pickaxe", Item::new, new Item.Properties().pickaxe(AURORITE, 1.0f, -2.8f).fireResistant());
    public static final Item AURORITE_AXE = register("aurorite_axe", Item::new, new Item.Properties().axe(AURORITE, 5.0f, -3.0f).fireResistant());
    public static final Item AURORITE_SHOVEL = register("aurorite_shovel", Item::new, new Item.Properties().shovel(AURORITE, 1.5f, -3.0f).fireResistant());
    public static final Item AURORITE_HOE = register("aurorite_hoe", Item::new, new Item.Properties().hoe(AURORITE, -3.0f, 0.0f).fireResistant());
    public static final Item AURORITE_HELMET = register("aurorite_helmet", Item::new, new Item.Properties().humanoidArmor(AURORITE_ARMOR, ArmorType.HELMET).fireResistant());
    public static final Item AURORITE_CHESTPLATE = register("aurorite_chestplate", Item::new, new Item.Properties().humanoidArmor(AURORITE_ARMOR, ArmorType.CHESTPLATE).fireResistant());
    public static final Item AURORITE_LEGGINGS = register("aurorite_leggings", Item::new, new Item.Properties().humanoidArmor(AURORITE_ARMOR, ArmorType.LEGGINGS).fireResistant());
    public static final Item AURORITE_BOOTS = register("aurorite_boots", Item::new, new Item.Properties().humanoidArmor(AURORITE_ARMOR, ArmorType.BOOTS).fireResistant());
    public static final Item CLOUDSTEP_BOOTS = register("cloudstep_boots", p -> new TooltipItem.Plain(p, 1),
            new Item.Properties().humanoidArmor(CLOUDSTEP_ARMOR, ArmorType.BOOTS).rarity(Rarity.RARE));

    // ---------- Spawn eggs ----------
    public static final Item TEMPEST_SOVEREIGN_SPAWN_EGG = egg("tempest_sovereign_spawn_egg", RealmEntities.TEMPEST_SOVEREIGN);
    public static final Item SKYRAY_SPAWN_EGG = egg("skyray_spawn_egg", RealmEntities.SKYRAY);
    public static final Item PUFFLING_SPAWN_EGG = egg("puffling_spawn_egg", RealmEntities.PUFFLING);
    public static final Item STATIC_WISP_SPAWN_EGG = egg("static_wisp_spawn_egg", RealmEntities.STATIC_WISP);
    public static final Item CYCLONE_SPAWN_EGG = egg("cyclone_spawn_egg", RealmEntities.CYCLONE);
    public static final Item STORMHAWK_SPAWN_EGG = egg("stormhawk_spawn_egg", RealmEntities.STORMHAWK);

    public static final Set<Item> AURORITE_SET = Set.of(AURORITE_HELMET, AURORITE_CHESTPLATE, AURORITE_LEGGINGS, AURORITE_BOOTS);

    private RealmItems() {}

    private static Consumable withEffect(Consumable.Builder builder, MobEffectInstance effect) {
        return builder.onConsume(new ApplyStatusEffectsConsumeEffect(effect)).build();
    }

    private static Item egg(String name, net.minecraft.world.entity.EntityType<?> type) {
        return register(name, SpawnEggItem::new, new Item.Properties().spawnEgg(type));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
        TAB_ORDER.add(item);
        return item;
    }

    public static void init() {}
}
