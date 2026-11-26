package com.stormreach.realm;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Every block added in 4.0: the Tempest Spire, the Stormgate, and the Tempest Realm. */
public final class RealmBlocks {
    /** Blocks that get an item, in creative-tab order. */
    public static final List<Block> WITH_ITEMS = new ArrayList<>();

    public static final ResourceKey<Feature> GALEWOOD_TREE = ResourceKey.create(Registries.FEATURE, Stormreach.id("galewood"));
    public static final ResourceKey<Feature> TALL_GALEWOOD_TREE = ResourceKey.create(Registries.FEATURE, Stormreach.id("tall_galewood"));
    // 6.0: TreeGrower takes (name, single-sapling trees, 2x2 trees, trees near flowers, shortest). The trees were in the
    // 2x2 slot, so a lone sapling could never grow (naturally or with bone meal). Now: mostly Galewood, sometimes tall.
    public static final TreeGrower GALEWOOD_GROWER = new TreeGrower("stormreach_galewood",
            WeightedList.<ResourceKey<Feature>>builder().add(GALEWOOD_TREE, 4).add(TALL_GALEWOOD_TREE, 1).build(), WeightedList.of(), WeightedList.of(), GALEWOOD_TREE);

    // ---------- Tempest Spire (Overworld structure) ----------
    public static final Block TEMPEST_BRICKS = register("tempest_bricks", Block::new, stone(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f), true);
    public static final Block CRACKED_TEMPEST_BRICKS = register("cracked_tempest_bricks", Block::new, stone(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f), true);
    public static final Block CHISELED_TEMPEST_BRICKS = register("chiseled_tempest_bricks", Block::new,
            stone(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f).lightLevel(s -> 6), true);
    public static final Block TEMPEST_BRICK_STAIRS = stairs("tempest_brick_stairs", TEMPEST_BRICKS, stone(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f));
    public static final Block TEMPEST_BRICK_SLAB = register("tempest_brick_slab", SlabBlock::new, stone(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f), true);
    public static final Block TEMPEST_BRICK_WALL = register("tempest_brick_wall", WallBlock::new, stone(MapColor.COLOR_PURPLE).strength(2.5f, 9.0f), true);
    public static final Block SOVEREIGN_ALTAR = register("sovereign_altar", SovereignAltarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0f, 3600000.0f).noLootTable().randomTicks()
                    .sound(SoundType.LODESTONE).lightLevel(s -> s.getValue(SovereignAltarBlock.RITUAL) > 0 ? 15 : 8), true);

    // ---------- The Stormgate ----------
    public static final Block STORMGATE_FRAME = register("stormgate_frame", StormgateFrameBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(8.0f, 1200.0f)
                    .sound(SoundType.LODESTONE).lightLevel(s -> s.getValue(StormgateFrameBlock.EYE) ? 11 : 3), true);
    public static final Block STORMGATE = register("stormgate", StormgatePortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().strength(-1.0f, 3600000.0f).noLootTable()
                    .lightLevel(s -> 15).sound(SoundType.GLASS).pushReaction(PushReaction.IMMOVEABLE), false);

    // ---------- Skyshale (the realm's stone) ----------
    public static final Block SKYSHALE = register("skyshale", Block::new, stone(MapColor.COLOR_LIGHT_GRAY), true);
    public static final Block COBBLED_SKYSHALE = register("cobbled_skyshale", Block::new, stone(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f), true);
    public static final Block COBBLED_SKYSHALE_STAIRS = stairs("cobbled_skyshale_stairs", COBBLED_SKYSHALE, stone(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f));
    public static final Block COBBLED_SKYSHALE_SLAB = register("cobbled_skyshale_slab", SlabBlock::new, stone(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f), true);
    public static final Block COBBLED_SKYSHALE_WALL = register("cobbled_skyshale_wall", WallBlock::new, stone(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f), true);
    public static final Block POLISHED_SKYSHALE = register("polished_skyshale", Block::new, stone(MapColor.COLOR_LIGHT_GRAY), true);
    public static final Block SKYSHALE_BRICKS = register("skyshale_bricks", Block::new, stone(MapColor.COLOR_LIGHT_GRAY), true);
    public static final Block SKYSHALE_BRICK_STAIRS = stairs("skyshale_brick_stairs", SKYSHALE_BRICKS, stone(MapColor.COLOR_LIGHT_GRAY));
    public static final Block SKYSHALE_BRICK_SLAB = register("skyshale_brick_slab", SlabBlock::new, stone(MapColor.COLOR_LIGHT_GRAY), true);
    public static final Block SKYSHALE_BRICK_WALL = register("skyshale_brick_wall", WallBlock::new, stone(MapColor.COLOR_LIGHT_GRAY), true);
    public static final Block CHISELED_SKYSHALE = register("chiseled_skyshale", Block::new, stone(MapColor.COLOR_LIGHT_GRAY), true);

    // ---------- Soil and clouds ----------
    public static final Block SKYSOIL = register("skysoil", Block::new, copy(Blocks.DIRT).mapColor(MapColor.COLOR_LIGHT_BLUE), true);
    public static final Block STATIC_GRASS = register("static_grass", SkyGrassBlock::new,
            copy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_CYAN).randomTicks(), true);
    public static final Block NIMBUS_CLOUD = register("nimbus_cloud", p -> new CloudBlock(CloudBlock.Kind.NIMBUS, p), cloud(MapColor.SNOW), true);
    public static final Block THUNDERCLOUD = register("thundercloud", p -> new CloudBlock(CloudBlock.Kind.THUNDER, p), cloud(MapColor.COLOR_BLUE).lightLevel(s -> 4), true);
    public static final Block GALE_CLOUD = register("gale_cloud", p -> new CloudBlock(CloudBlock.Kind.GALE, p), cloud(MapColor.COLOR_LIGHT_GRAY), true);

    // ---------- Galewood ----------
    public static final Block GALEWOOD_LOG = register("galewood_log", RotatedPillarBlock::new, copy(Blocks.BIRCH_LOG).mapColor(MapColor.COLOR_LIGHT_GRAY), true);
    public static final Block STRIPPED_GALEWOOD_LOG = register("stripped_galewood_log", RotatedPillarBlock::new, copy(Blocks.STRIPPED_BIRCH_LOG), true);
    public static final Block GALEWOOD_PLANKS = register("galewood_planks", Block::new, copy(Blocks.BIRCH_PLANKS), true);
    public static final Block GALEWOOD_LEAVES = register("galewood_leaves", p -> new TintedParticleLeavesBlock(0.02f, p), copy(Blocks.BIRCH_LEAVES).lightLevel(s -> 2), true);
    public static final Block GALEWOOD_SAPLING = register("galewood_sapling", p -> new SaplingBlock(GALEWOOD_GROWER, p), copy(Blocks.BIRCH_SAPLING), true);
    public static final Block GALEWOOD_STAIRS = stairs("galewood_stairs", GALEWOOD_PLANKS, copy(Blocks.BIRCH_STAIRS));
    public static final Block GALEWOOD_SLAB = register("galewood_slab", SlabBlock::new, copy(Blocks.BIRCH_SLAB), true);
    public static final Block GALEWOOD_FENCE = register("galewood_fence", FenceBlock::new, copy(Blocks.BIRCH_FENCE), true);
    public static final Block GALEWOOD_FENCE_GATE = register("galewood_fence_gate", p -> new FenceGateBlock(WoodType.BIRCH, p), copy(Blocks.BIRCH_FENCE_GATE), true);
    public static final Block GALEWOOD_DOOR = register("galewood_door", p -> new DoorBlock(BlockSetType.BIRCH, p), copy(Blocks.BIRCH_DOOR), true);
    public static final Block GALEWOOD_TRAPDOOR = register("galewood_trapdoor", p -> new TrapDoorBlock(BlockSetType.BIRCH, p), copy(Blocks.BIRCH_TRAPDOOR), true);
    public static final Block GALEWOOD_BUTTON = register("galewood_button", p -> new ButtonBlock(BlockSetType.BIRCH, 30, p), copy(Blocks.BIRCH_BUTTON), true);
    public static final Block GALEWOOD_PRESSURE_PLATE = register("galewood_pressure_plate", p -> new PressurePlateBlock(BlockSetType.BIRCH, p), copy(Blocks.BIRCH_PRESSURE_PLATE), true);

    // ---------- Ores and their blocks ----------
    public static final Block VOLTITE_ORE = register("voltite_ore", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            stone(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 3.0f), true);
    public static final Block AURORITE_ORE = register("aurorite_ore", p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            stone(MapColor.COLOR_LIGHT_GRAY).strength(4.5f, 3.0f).lightLevel(s -> 5), true);
    public static final Block ION_ORE = register("ion_ore", p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            stone(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 3.0f).lightLevel(s -> 9), true);
    public static final Block VOLTITE_BLOCK = register("voltite_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().strength(5.0f, 6.0f).sound(SoundType.METAL), true);
    public static final Block AURORITE_BLOCK = register("aurorite_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).requiresCorrectToolForDrops().strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST).lightLevel(s -> 8), true);
    public static final Block ION_LAMP = register("ion_lamp", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.3f).sound(SoundType.GLASS).lightLevel(s -> 15), true);

    // ---------- Plants ----------
    public static final Block SKYBLOOM = register("skybloom", p -> new FlowerBlock(MobEffects.SLOW_FALLING, 8.0f, p), plant(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 3), true);
    public static final Block STORMCAP = register("stormcap", p -> new FlowerBlock(MobEffects.NIGHT_VISION, 6.0f, p), plant(MapColor.COLOR_PURPLE).lightLevel(s -> 6), true);
    public static final Block GALE_GRASS = register("gale_grass", TallGrassBlock::new, plant(MapColor.COLOR_CYAN).replaceable(), true);

    private RealmBlocks() {}

    private static BlockBehaviour.Properties copy(Block block) {
        return BlockBehaviour.Properties.ofFullCopy(block);
    }

    private static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5f, 6.0f).sound(SoundType.TUFF);
    }

    private static BlockBehaviour.Properties cloud(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.2f).sound(SoundType.WOOL).noOcclusion().noCollision()
                .isValidSpawn((s, l, pos, t) -> false).isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos, box) -> false);
    }

    private static BlockBehaviour.Properties plant(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollision().instabreak()
                .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED);
    }

    private static Block stairs(String name, Block base, BlockBehaviour.Properties props) {
        return register(name, p -> new StairBlock(base.defaultBlockState(), p) {}, props, true);
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                  BlockBehaviour.Properties props, boolean withItem) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
        if (withItem) WITH_ITEMS.add(block);
        return block;
    }

    public static void init() {}
}
