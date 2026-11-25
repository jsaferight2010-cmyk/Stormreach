package com.stormreach.registry;

import com.stormreach.Stormreach;
import com.stormreach.block.StormBarometerBlock;
import com.stormreach.block.StormCollectorBlock;
import com.stormreach.block.StormCrystalBlock;
import com.stormreach.block.StormberryShrubBlock;
import com.stormreach.block.WindVaneBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
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

public final class ModBlocks {
    /** Blocks that get a normal BlockItem, in creative-tab order. */
    public static final List<Block> WITH_ITEMS = new ArrayList<>();

    public static final ResourceKey<Feature> STORMPINE_TREE = ResourceKey.create(Registries.FEATURE, Stormreach.id("stormpine"));
    public static final TreeGrower STORMPINE_GROWER = new TreeGrower("stormreach_stormpine",
            WeightedList.of(), WeightedList.of(STORMPINE_TREE), WeightedList.of(), STORMPINE_TREE);

    // ---------- Stormstone family (cliff stone of the coast) ----------
    public static final Block STORMSTONE = register("stormstone", Block::new, stone(), true);
    public static final Block STORMSTONE_STAIRS = stairs("stormstone_stairs", STORMSTONE, stone());
    public static final Block STORMSTONE_SLAB = register("stormstone_slab", SlabBlock::new, stone(), true);
    public static final Block STORMSTONE_WALL = register("stormstone_wall", WallBlock::new, stone(), true);
    public static final Block CHARGED_STORMSTONE = register("charged_stormstone", Block::new, stone().lightLevel(s -> 7), true);
    public static final Block POLISHED_STORMSTONE = register("polished_stormstone", Block::new, stone(), true);
    public static final Block POLISHED_STORMSTONE_STAIRS = stairs("polished_stormstone_stairs", POLISHED_STORMSTONE, stone());
    public static final Block POLISHED_STORMSTONE_SLAB = register("polished_stormstone_slab", SlabBlock::new, stone(), true);
    public static final Block POLISHED_STORMSTONE_WALL = register("polished_stormstone_wall", WallBlock::new, stone(), true);
    public static final Block STORMSTONE_BRICKS = register("stormstone_bricks", Block::new, stone(), true);
    public static final Block CRACKED_STORMSTONE_BRICKS = register("cracked_stormstone_bricks", Block::new, stone(), true);
    public static final Block CHISELED_STORMSTONE = register("chiseled_stormstone", Block::new, stone(), true);
    public static final Block STORMSTONE_BRICK_STAIRS = stairs("stormstone_brick_stairs", STORMSTONE_BRICKS, stone());
    public static final Block STORMSTONE_BRICK_SLAB = register("stormstone_brick_slab", SlabBlock::new, stone(), true);
    public static final Block STORMSTONE_BRICK_WALL = register("stormstone_brick_wall", WallBlock::new, stone(), true);

    // ---------- Driftwood (washed-up wood) ----------
    public static final Block DRIFTWOOD_LOG = register("driftwood_log", RotatedPillarBlock::new, wood().strength(2.0f), true);
    public static final Block DRIFTWOOD_PLANKS = register("driftwood_planks", Block::new, wood().strength(2.0f, 3.0f), true);
    public static final Block DRIFTWOOD_STAIRS = stairs("driftwood_stairs", DRIFTWOOD_PLANKS, wood().strength(2.0f, 3.0f));
    public static final Block DRIFTWOOD_SLAB = register("driftwood_slab", SlabBlock::new, wood().strength(2.0f, 3.0f), true);
    public static final Block DRIFTWOOD_FENCE = register("driftwood_fence", FenceBlock::new, copy(Blocks.OAK_FENCE), true);
    public static final Block DRIFTWOOD_FENCE_GATE = register("driftwood_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), copy(Blocks.OAK_FENCE_GATE), true);
    public static final Block DRIFTWOOD_DOOR = register("driftwood_door", p -> new DoorBlock(BlockSetType.OAK, p), copy(Blocks.OAK_DOOR), true);
    public static final Block DRIFTWOOD_TRAPDOOR = register("driftwood_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), copy(Blocks.OAK_TRAPDOOR), true);
    public static final Block DRIFTWOOD_BUTTON = register("driftwood_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), copy(Blocks.OAK_BUTTON), true);
    public static final Block DRIFTWOOD_PRESSURE_PLATE = register("driftwood_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), copy(Blocks.OAK_PRESSURE_PLATE), true);

    // ---------- Stormpine (wind-bent conifer of the highlands) ----------
    public static final Block STORMPINE_LOG = register("stormpine_log", RotatedPillarBlock::new, copy(Blocks.SPRUCE_LOG), true);
    public static final Block STRIPPED_STORMPINE_LOG = register("stripped_stormpine_log", RotatedPillarBlock::new, copy(Blocks.STRIPPED_SPRUCE_LOG), true);
    public static final Block STORMPINE_PLANKS = register("stormpine_planks", Block::new, copy(Blocks.SPRUCE_PLANKS), true);
    public static final Block STORMPINE_LEAVES = register("stormpine_leaves", p -> new TintedParticleLeavesBlock(0.01f, p), copy(Blocks.SPRUCE_LEAVES), true);
    public static final Block STORMPINE_SAPLING = register("stormpine_sapling", p -> new SaplingBlock(STORMPINE_GROWER, p), copy(Blocks.SPRUCE_SAPLING), true);
    public static final Block STORMPINE_STAIRS = stairs("stormpine_stairs", STORMPINE_PLANKS, copy(Blocks.SPRUCE_STAIRS));
    public static final Block STORMPINE_SLAB = register("stormpine_slab", SlabBlock::new, copy(Blocks.SPRUCE_SLAB), true);
    public static final Block STORMPINE_FENCE = register("stormpine_fence", FenceBlock::new, copy(Blocks.SPRUCE_FENCE), true);
    public static final Block STORMPINE_FENCE_GATE = register("stormpine_fence_gate", p -> new FenceGateBlock(WoodType.SPRUCE, p), copy(Blocks.SPRUCE_FENCE_GATE), true);
    public static final Block STORMPINE_DOOR = register("stormpine_door", p -> new DoorBlock(BlockSetType.SPRUCE, p), copy(Blocks.SPRUCE_DOOR), true);
    public static final Block STORMPINE_TRAPDOOR = register("stormpine_trapdoor", p -> new TrapDoorBlock(BlockSetType.SPRUCE, p), copy(Blocks.SPRUCE_TRAPDOOR), true);
    public static final Block STORMPINE_BUTTON = register("stormpine_button", p -> new ButtonBlock(BlockSetType.SPRUCE, 30, p), copy(Blocks.SPRUCE_BUTTON), true);
    public static final Block STORMPINE_PRESSURE_PLATE = register("stormpine_pressure_plate", p -> new PressurePlateBlock(BlockSetType.SPRUCE, p), copy(Blocks.SPRUCE_PRESSURE_PLATE), true);

    // ---------- Storm crystals, glass and lightning-made blocks ----------
    public static final Block STORM_CRYSTAL_ORE = register("storm_crystal_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p), stone().strength(3.0f, 3.0f).lightLevel(s -> 3), true);
    public static final Block STORM_CRYSTAL_BLOCK = register("storm_crystal_block", StormCrystalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f).sound(SoundType.AMETHYST).lightLevel(s -> 10), true);
    public static final Block STORMGLASS = register("stormglass", p -> new TransparentBlock(p) {}, glass(), true);
    public static final Block STORMGLASS_PANE = register("stormglass_pane", IronBarsBlock::new, glass(), true);
    public static final Block FULGURITE = register("fulgurite", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0f).sound(SoundType.GLASS).lightLevel(s -> 4), true);
    public static final Block FULGURITE_LANTERN = register("fulgurite_lantern", LanternBlock::new,
            copy(Blocks.LANTERN).lightLevel(s -> 15), true);
    public static final Block STORM_LANTERN = register("storm_lantern", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f).sound(SoundType.GLASS).lightLevel(s -> 15), true);

    // ---------- Salt (Brinewater Marsh) ----------
    public static final Block SALT_BLOCK = register("salt_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.CALCITE), true);
    public static final Block SALT_CRYSTAL = register("salt_crystal", p -> new AmethystClusterBlock(5.0f, 3.0f, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noOcclusion().strength(0.6f).sound(SoundType.SMALL_AMETHYST_BUD)
                    .lightLevel(s -> 2).pushReaction(PushReaction.POPPED), true);

    // ---------- Stormblight ----------
    public static final Block STORMHEART = register("stormheart", com.stormreach.block.StormheartBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(6.0f, 1200.0f)
                    .sound(SoundType.AMETHYST).lightLevel(s -> s.getValue(com.stormreach.block.StormheartBlock.ACTIVE) ? 15 : 6), true);
    public static final Block STORMSCAR_TURF = register("stormscar_turf", com.stormreach.block.StormscarTurfBlock::new,
            copy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_LIGHT_BLUE), true);

    // ---------- Weather machines ----------
    public static final Block STORM_BAROMETER = register("storm_barometer", StormBarometerBlock::new, stone().strength(1.5f, 6.0f), true);
    public static final Block WIND_VANE = register("wind_vane", WindVaneBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.COPPER).noOcclusion(), true);
    public static final Block STORM_COLLECTOR = register("storm_collector", StormCollectorBlock::new,
            stone().strength(3.0f, 6.0f).sound(SoundType.COPPER).lightLevel(s -> s.getValue(StormCollectorBlock.CHARGE) * 3), true);

    // ---------- Coastal plants ----------
    public static final Block SEA_THRIFT = register("sea_thrift", p -> new FlowerBlock(MobEffects.SPEED, 6.0f, p), plant(MapColor.COLOR_PINK), true);
    public static final Block SEA_ROCKET = register("sea_rocket", p -> new FlowerBlock(MobEffects.JUMP_BOOST, 6.0f, p), plant(MapColor.COLOR_PURPLE), true);
    public static final Block SALTGRASS = register("saltgrass", p -> new FlowerBlock(MobEffects.WATER_BREATHING, 5.0f, p), plant(MapColor.PLANT).replaceable(), true);
    /** Placed by planting Stormberries (like sweet berries), so it has no item of its own. */
    public static final Block STORMBERRY_SHRUB = register("stormberry_shrub", StormberryShrubBlock::new, plant(MapColor.COLOR_PURPLE).randomTicks(), false);

    private ModBlocks() {}

    private static BlockBehaviour.Properties copy(Block block) {
        return BlockBehaviour.Properties.ofFullCopy(block);
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5f, 6.0f).sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).instrument(NoteBlockInstrument.BASS)
                .sound(SoundType.WOOD).ignitedByLava();
    }

    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.HAT)
                .strength(0.4f).sound(SoundType.GLASS).noOcclusion()
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

    public static void init() {
        // Loading this class registers everything above.
    }
}
