package com.stormreach.sky;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModSfx;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Skyward (5.3): the Squall Serpent (a sky serpent of the Tempest Realm), Hailstone Golems (which come with
 * thunderstorms and melt when they pass), Aurora Moths, and Stormflies (which swarm before a storm). Storm gear:
 * the Weather Radio, Bottled Lightning, Cloud and Supercell Seeders, the Wind Turbine, the Storm Shelter Hatch,
 * the Stormchaser suit and the Gale Blade.
 */
public final class SkyRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();

    public static final TagKey<Item> CHASER_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("repairs_stormchaser"));
    public static final TagKey<Item> GALE_BLADE_REPAIRS = TagKey.create(Registries.ITEM, Stormreach.id("repairs_gale_blade"));
    public static final ResourceKey<EquipmentAsset> CHASER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Stormreach.id("stormchaser"));
    public static final ArmorMaterial CHASER_ARMOR = new ArmorMaterial(22,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 6),
            14, ModSfx.holder(ModSfx.ITEM_EQUIP_PACK), 1.0f, 0.1f, CHASER_REPAIRS, CHASER_ASSET);
    public static final ToolMaterial GALE_MATERIAL = new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1400, 8.0f, 3.0f, 16, GALE_BLADE_REPAIRS);

    // ---- blocks
    public static final Block WIND_TURBINE = block("wind_turbine", WindTurbineBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.METAL).strength(3f, 6f).noOcclusion());
    public static final Block STORM_SHELTER_HATCH = block("storm_shelter_hatch", p -> new TrapDoorBlock(BlockSetType.IRON, p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR).strength(8f, 1200f));
    public static final Block STORMFLY_JAR = block("stormfly_jar", LanternBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
            .sound(SoundType.GLASS).lightLevel(s -> 12).mapColor(MapColor.COLOR_LIGHT_BLUE));
    public static final Block AURORA_GLASS = block("aurora_glass", TransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
            .lightLevel(s -> 7).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final Block SERPENTSCALE_BLOCK = block("serpentscale_block", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PRISMARINE)
            .mapColor(MapColor.COLOR_PURPLE).strength(3f, 9f));
    public static final Block GLACIAL_LAMP = block("glacial_lamp", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN)
            .lightLevel(s -> 15).mapColor(MapColor.ICE));
    public static final BlockEntityType<WindTurbineBlockEntity> TURBINE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Stormreach.id("wind_turbine"), new BlockEntityType<>(WindTurbineBlockEntity::new, Set.of(WIND_TURBINE)));

    // ---- items
    public static final Item SERPENT_SCALE = item("serpent_scale", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item SQUALL_HEART = item("squall_heart", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().rarity(Rarity.EPIC).stacksTo(16));
    public static final Item GLACIAL_CORE = item("glacial_core", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item AURORA_DUST = item("aurora_dust", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties());
    public static final Item BOTTLED_STORMFLY = item("bottled_stormfly", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().stacksTo(16));
    public static final Item BOTTLED_LIGHTNING = item("bottled_lightning", BottledLightningItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final Item CLOUD_SEEDER = item("cloud_seeder", p -> new CloudSeederItem(p, false), new Item.Properties().stacksTo(16));
    public static final Item SUPERCELL_SEEDER = item("supercell_seeder", p -> new CloudSeederItem(p, true), new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final Item GALE_BLADE = item("gale_blade", GaleBladeItem::new, new Item.Properties().sword(GALE_MATERIAL, 3.0f, -2.2f).rarity(Rarity.EPIC));
    public static final Item CHASERS_COFFEE = item("chasers_coffee", Item::new, new Item.Properties().stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build(),
                    Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                            new MobEffectInstance(MobEffects.SPEED, 2400, 0), new MobEffectInstance(MobEffects.HASTE, 2400, 0),
                            new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0)))).build()));
    public static final Item SNOW_CONE = item("snow_cone", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0))).build()));
    public static final Item CHASER_GOGGLES = item("stormchaser_goggles", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().humanoidArmor(CHASER_ARMOR, ArmorType.HELMET));
    public static final Item CHASER_JACKET = item("stormchaser_jacket", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().humanoidArmor(CHASER_ARMOR, ArmorType.CHESTPLATE));
    public static final Item CHASER_TROUSERS = item("stormchaser_trousers", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().humanoidArmor(CHASER_ARMOR, ArmorType.LEGGINGS));
    public static final Item CHASER_BOOTS = item("stormchaser_boots", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().humanoidArmor(CHASER_ARMOR, ArmorType.BOOTS));

    // ---- creatures
    public static final EntityType<SquallSerpent> SQUALL_SERPENT = entity("squall_serpent",
            EntityType.Builder.of(SquallSerpent::new, MobCategory.MONSTER).sized(1.8f, 1.4f).eyeHeight(0.8f).fireImmune().clientTrackingRange(16));
    public static final EntityType<HailstoneGolem> HAILSTONE_GOLEM = entity("hailstone_golem",
            EntityType.Builder.of(HailstoneGolem::new, MobCategory.MONSTER).sized(1.3f, 2.5f).eyeHeight(2.1f).clientTrackingRange(10));
    public static final EntityType<AuroraMoth> AURORA_MOTH = entity("aurora_moth",
            EntityType.Builder.of(AuroraMoth::new, MobCategory.AMBIENT).sized(0.6f, 0.4f).eyeHeight(0.2f).clientTrackingRange(8));
    public static final EntityType<Stormfly> STORMFLY = entity("stormfly",
            EntityType.Builder.of(Stormfly::new, MobCategory.AMBIENT).sized(0.25f, 0.25f).eyeHeight(0.12f).clientTrackingRange(6));
    public static final EntityType<BottledLightningItem.Thrown> BOTTLED_LIGHTNING_ENTITY = entity("bottled_lightning",
            EntityType.Builder.<BottledLightningItem.Thrown>of(BottledLightningItem.Thrown::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));

    public static final Item SQUALL_SERPENT_SPAWN_EGG = egg("squall_serpent", SQUALL_SERPENT);
    public static final Item HAILSTONE_GOLEM_SPAWN_EGG = egg("hailstone_golem", HAILSTONE_GOLEM);
    public static final Item AURORA_MOTH_SPAWN_EGG = egg("aurora_moth", AURORA_MOTH);
    public static final Item STORMFLY_SPAWN_EGG = egg("stormfly", STORMFLY);

    private SkyRegistry() {}

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        ITEMS.add(Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(b, new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
        return b;
    }

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties p) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
        ITEMS.add(it);
        return it;
    }

    private static Item egg(String mob, EntityType<?> type) {
        return item(mob + "_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(type));
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SQUALL_SERPENT, SquallSerpent.createAttributes());
        FabricDefaultAttributeRegistry.register(HAILSTONE_GOLEM, HailstoneGolem.createAttributes());
        FabricDefaultAttributeRegistry.register(AURORA_MOTH, AuroraMoth.createAttributes());
        FabricDefaultAttributeRegistry.register(STORMFLY, Stormfly.createAttributes());
        SpawnPlacements.register(SQUALL_SERPENT, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && StormreachConfig.realmMobSpawning && p.getY() > l.getMinY() + 50 && rnd.nextInt(20) == 0
                        && l.getEntitiesOfClass(SquallSerpent.class, new net.minecraft.world.phys.AABB(p).inflate(128)).isEmpty());
        SpawnPlacements.register(AURORA_MOTH, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && StormreachConfig.realmMobSpawning && l.getSkyDarken() > 4 && p.getY() > l.getMinY() + 40);
        SpawnPlacements.register(HAILSTONE_GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, l, r, p, rnd) -> false);
        SpawnPlacements.register(STORMFLY, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, (t, l, r, p, rnd) -> false);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            StormArrivals.tick(server);
            WeatherRadio.tick(server);
            ChaserGear.tick(server);
            com.stormreach.weather.WeatherHud.tick(server);
        });
    }
}
