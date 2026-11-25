package com.stormreach.labyrinth;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class LabyrinthRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();
    public static final ResourceKey<Structure> STRUCTURE_KEY = ResourceKey.create(Registries.STRUCTURE, Stormreach.id("hollow_labyrinth"));
    public static final StructureType<LabyrinthStructure> STRUCTURE_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
            Stormreach.id("labyrinth"), () -> LabyrinthStructure.CODEC);
    public static final StructurePieceType PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
            Stormreach.id("labyrinth_piece"), (StructurePieceType.ContextlessType) LabyrinthPiece::new);

    public static final Block LOCKER = block("rusted_locker", LockerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
            .strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
            .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p, a) -> false), true);
    public static final Block SEAL = block("labyrinth_seal", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
            .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE).lightLevel(s -> 3), false);
    public static final Block KEYHOLE = block("labyrinth_keyhole", KeyholeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
            .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE).lightLevel(s -> s.getValue(KeyholeBlock.FILLED) ? 12 : 5), false);
    /** The Vault Door's plating: unbreakable, opened only by its three locks. */
    public static final Block VAULT_DOOR = block("vault_door", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.IMMOVEABLE), false);
    /** The treasure chamber's walls, floor and ceiling: unbreakable, so the vault can't be dug into. */
    public static final Block VAULT_WALL = block("vault_wall", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
            .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_TILES).pushReaction(PushReaction.IMMOVEABLE), false);
    public static final Block HOLLOW_LAMP = block("hollow_lamp", HollowLampBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(1.5f, 6.0f).sound(SoundType.GLASS).randomTicks().lightLevel(s -> s.getValue(HollowLampBlock.LIGHT)), true);

    public static final EntityType<HollowKeeper> HOLLOW_KEEPER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("hollow_keeper")),
            EntityType.Builder.of(HollowKeeper::new, MobCategory.MONSTER).sized(0.9f, 3.1f).eyeHeight(2.8f).fireImmune().clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("hollow_keeper"))));

    public static final Item[] KEYS = {
            item("labyrinth_key_gale", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().stacksTo(1).rarity(Rarity.RARE)),
            item("labyrinth_key_thunder", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().stacksTo(1).rarity(Rarity.RARE)),
            item("labyrinth_key_hollow", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().stacksTo(1).rarity(Rarity.RARE))};
    /** Before 5.1 the three keys were one item; kept so old keys aren't lost (no lock takes it now). */
    public static final Item LABYRINTH_KEY = item("labyrinth_key", p -> new com.stormreach.realm.TooltipItem.Plain(p, 1), new Item.Properties().stacksTo(3).rarity(Rarity.RARE));
    public static final Item HEART_OF_THE_LABYRINTH = item("heart_of_the_labyrinth", p -> new com.stormreach.realm.TooltipItem.Plain(p, 2),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final Item HOLLOW_KEEPER_SPAWN_EGG = item("hollow_keeper_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(HOLLOW_KEEPER));

    private LabyrinthRegistry() {}

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p, boolean withItem) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item.Properties ip = new Item.Properties().setId(ik).useBlockDescriptionPrefix();
        Item it = Registry.register(BuiltInRegistries.ITEM, ik, b instanceof LockerBlock ? new DoubleHighBlockItem(b, ip) : new BlockItem(b, ip));
        if (withItem) ITEMS.add(it); else ITEMS.add(it);
        return b;
    }

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties p) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
        ITEMS.add(it);
        return it;
    }

    public static void init() {
        LockerNet.init();
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.JOIN.register(HidingTracker::onJoin);
        FabricDefaultAttributeRegistry.register(HOLLOW_KEEPER, HollowKeeper.createAttributes());
        SpawnPlacements.register(HOLLOW_KEEPER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, l, r, p, rnd) -> false);
    }
}
