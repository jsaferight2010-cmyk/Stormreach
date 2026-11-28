package com.stormreach.forest;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModEffects;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Elderstorm Grove (Tempest Realm forest): giant Elderstorm trees, Stormfruit, Glimmerstags and Thistlehops. */
public final class ForestRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();
    public static final ResourceKey<Feature> ELDERSTORM_TREE = ResourceKey.create(Registries.FEATURE, Stormreach.id("elderstorm"));
    public static final ResourceKey<Feature> GIANT_ELDERSTORM_TREE = ResourceKey.create(Registries.FEATURE, Stormreach.id("giant_elderstorm"));
    public static final TreeGrower ELDERSTORM_GROWER = new TreeGrower("stormreach_elderstorm",
            WeightedList.of(ELDERSTORM_TREE), WeightedList.of(GIANT_ELDERSTORM_TREE), WeightedList.of(), ELDERSTORM_TREE);

    public static final Block ELDERSTORM_LOG = block("elderstorm_log", RotatedPillarBlock::new, copy(Blocks.DARK_OAK_LOG).mapColor(MapColor.COLOR_PURPLE));
    public static final Block STRIPPED_ELDERSTORM_LOG = block("stripped_elderstorm_log", RotatedPillarBlock::new, copy(Blocks.STRIPPED_DARK_OAK_LOG));
    public static final Block ELDERSTORM_PLANKS = block("elderstorm_planks", Block::new, copy(Blocks.DARK_OAK_PLANKS));
    public static final Block ELDERSTORM_LEAVES = block("elderstorm_leaves", p -> new TintedParticleLeavesBlock(0.02f, p), copy(Blocks.DARK_OAK_LEAVES).lightLevel(s -> 3));
    public static final Block ELDERSTORM_SAPLING = block("elderstorm_sapling", p -> new SaplingBlock(ELDERSTORM_GROWER, p), copy(Blocks.DARK_OAK_SAPLING));
    public static final Block ELDERSTORM_STAIRS = block("elderstorm_stairs", p -> new StairBlock(ELDERSTORM_PLANKS.defaultBlockState(), p) {}, copy(Blocks.DARK_OAK_STAIRS));
    public static final Block ELDERSTORM_SLAB = block("elderstorm_slab", SlabBlock::new, copy(Blocks.DARK_OAK_SLAB));
    public static final Block ELDERSTORM_FENCE = block("elderstorm_fence", FenceBlock::new, copy(Blocks.DARK_OAK_FENCE));
    public static final Block ELDERSTORM_FENCE_GATE = block("elderstorm_fence_gate", p -> new FenceGateBlock(WoodType.DARK_OAK, p), copy(Blocks.DARK_OAK_FENCE_GATE));
    public static final Block ELDERSTORM_DOOR = block("elderstorm_door", p -> new DoorBlock(BlockSetType.DARK_OAK, p), copy(Blocks.DARK_OAK_DOOR));
    public static final Block ELDERSTORM_TRAPDOOR = block("elderstorm_trapdoor", p -> new TrapDoorBlock(BlockSetType.DARK_OAK, p), copy(Blocks.DARK_OAK_TRAPDOOR));
    public static final Block ELDERSTORM_BUTTON = block("elderstorm_button", p -> new ButtonBlock(BlockSetType.DARK_OAK, 30, p), copy(Blocks.DARK_OAK_BUTTON));
    public static final Block ELDERSTORM_PRESSURE_PLATE = block("elderstorm_pressure_plate", p -> new PressurePlateBlock(BlockSetType.DARK_OAK, p), copy(Blocks.DARK_OAK_PRESSURE_PLATE));
    public static final Block STORMFRUIT_POD = block("stormfruit_pod", StormfruitPodBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
            .noCollision().strength(0.2f).sound(SoundType.CAVE_VINES).randomTicks().pushReaction(PushReaction.POPPED)
            .lightLevel(s -> 4 + s.getValue(StormfruitPodBlock.AGE) * 4));
    public static final Block THUNDERBLOOM = block("thunderbloom", p -> new FlowerBlock(MobEffects.HASTE, 8f, p), plant(MapColor.COLOR_YELLOW).lightLevel(s -> 6));
    public static final Block SPARKFERN = block("sparkfern", TallGrassBlock::new, plant(MapColor.COLOR_CYAN).replaceable());

    public static final Item STORMFRUIT = item("stormfruit", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                            new MobEffectInstance(MobEffects.SPEED, 400, 0), new MobEffectInstance(ModEffects.STATIC_CHARGE, 200, 0)))).build()));
    public static final Item GILDED_STORMFRUIT = item("gilded_stormfruit", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().rarity(Rarity.RARE)
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.2f).alwaysEdible().build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                            new MobEffectInstance(MobEffects.REGENERATION, 200, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1),
                            new MobEffectInstance(ModEffects.GROUNDED, 2400, 0)))).build()));
    public static final Item STORMFRUIT_PRESERVE = item("stormfruit_preserve", Item::new, new Item.Properties().stacksTo(16)
            .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build(),
                    Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 1800, 1))).build()));
    public static final Item THISTLE_DOWN = item("thistle_down", Item::new, new Item.Properties());
    public static final Item GLIMMER_ANTLER = item("glimmer_antler", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final EntityType<Glimmerstag> GLIMMERSTAG = entity("glimmerstag",
            EntityType.Builder.of(Glimmerstag::new, MobCategory.CREATURE).sized(0.9f, 1.6f).eyeHeight(1.45f).clientTrackingRange(10));
    public static final EntityType<Thistlehop> THISTLEHOP = entity("thistlehop",
            EntityType.Builder.of(Thistlehop::new, MobCategory.CREATURE).sized(0.5f, 0.5f).eyeHeight(0.35f).clientTrackingRange(8));
    public static final Item GLIMMERSTAG_SPAWN_EGG = item("glimmerstag_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(GLIMMERSTAG));
    public static final Item THISTLEHOP_SPAWN_EGG = item("thistlehop_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(THISTLEHOP));

    private ForestRegistry() {}

    private static BlockBehaviour.Properties copy(Block b) {
        return BlockBehaviour.Properties.ofFullCopy(b);
    }

    private static BlockBehaviour.Properties plant(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).noCollision().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED);
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        if (!name.equals("stormfruit_pod")) {
            ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
            Item.Properties ip = new Item.Properties().setId(ik).useBlockDescriptionPrefix();
            ITEMS.add(Registry.register(BuiltInRegistries.ITEM, ik, b instanceof DoorBlock ? new net.minecraft.world.item.DoubleHighBlockItem(b, ip) : new BlockItem(b, ip)));
        }
        return b;
    }

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties p) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
        ITEMS.add(it);
        return it;
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(GLIMMERSTAG, Glimmerstag.createAttributes());
        FabricDefaultAttributeRegistry.register(THISTLEHOP, Thistlehop.createAttributes());
        SpawnPlacements.register(GLIMMERSTAG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> StormreachConfig.realmMobSpawning && Animal.checkAnimalSpawnRules(t, l, r, p, rnd));
        SpawnPlacements.register(THISTLEHOP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> StormreachConfig.realmMobSpawning && Animal.checkAnimalSpawnRules(t, l, r, p, rnd));
    }
}
