package com.stormreach.front;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Thunderfront (5.4): signs and tools of the coming storm. Rainfrogs (a croaking chorus before the rain), Storm Petrels
 * (seabirds that gather over the sea ahead of a storm), Dust Devils (whirlwinds of hot, clear days), the Tesla Coil
 * (stores lightning and zaps hostile mobs), Wind Chimes, the Umbrella, the Omen Feather and the Bottled Dust Devil.
 */
public final class FrontRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();

    // ---- blocks
    public static final Block TESLA_COIL = block("tesla_coil", TeslaCoilBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(4.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion()
            .lightLevel(s -> s.getValue(TeslaCoilBlock.CHARGE) > 0 ? 3 + s.getValue(TeslaCoilBlock.CHARGE) * 2 : 0));
    public static final Block WIND_CHIMES = block("wind_chimes", WindChimesBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(0.5f).sound(SoundType.CHAIN).noOcclusion().noCollision());
    public static final BlockEntityType<TeslaCoilBlockEntity> TESLA_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Stormreach.id("tesla_coil"), new BlockEntityType<>(TeslaCoilBlockEntity::new, Set.of(TESLA_COIL)));

    // ---- items
    public static final Item UMBRELLA = item("umbrella", UmbrellaItem::new, new Item.Properties().stacksTo(1).durability(250));
    public static final Item OMEN_FEATHER = item("omen_feather", OmenFeatherItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final Item BOTTLED_DUST_DEVIL = item("bottled_dust_devil", BottledDustDevilItem::new, new Item.Properties().stacksTo(16));

    // ---- creatures
    public static final EntityType<Rainfrog> RAINFROG = entity("rainfrog",
            EntityType.Builder.of(Rainfrog::new, MobCategory.CREATURE).sized(0.5f, 0.4f).eyeHeight(0.3f).clientTrackingRange(8));
    public static final EntityType<StormPetrel> STORM_PETREL = entity("storm_petrel",
            EntityType.Builder.of(StormPetrel::new, MobCategory.AMBIENT).sized(0.5f, 0.45f).eyeHeight(0.3f).clientTrackingRange(10));
    public static final EntityType<DustDevil> DUST_DEVIL = entity("dust_devil",
            EntityType.Builder.of(DustDevil::new, MobCategory.AMBIENT).sized(1.2f, 3.2f).eyeHeight(1.6f).fireImmune().clientTrackingRange(10));
    public static final EntityType<BottledDustDevilItem.Thrown> BOTTLED_DUST_DEVIL_ENTITY = entity("bottled_dust_devil",
            EntityType.Builder.<BottledDustDevilItem.Thrown>of(BottledDustDevilItem.Thrown::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));

    public static final Item RAINFROG_SPAWN_EGG = egg("rainfrog", RAINFROG);
    public static final Item STORM_PETREL_SPAWN_EGG = egg("storm_petrel", STORM_PETREL);
    public static final Item DUST_DEVIL_SPAWN_EGG = egg("dust_devil", DUST_DEVIL);

    private FrontRegistry() {}

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
        FabricDefaultAttributeRegistry.register(RAINFROG, Rainfrog.createAttributes());
        FabricDefaultAttributeRegistry.register(STORM_PETREL, StormPetrel.createAttributes());
        FabricDefaultAttributeRegistry.register(DUST_DEVIL, DustDevil.createAttributes());
        SpawnPlacements.register(RAINFROG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && Rainfrog.checkSpawn(l, p));
        SpawnPlacements.register(STORM_PETREL, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, (t, l, r, p, rnd) -> false);
        SpawnPlacements.register(DUST_DEVIL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, l, r, p, rnd) -> false);
        ServerTickEvents.END_SERVER_TICK.register(FrontArrivals::tick);
    }
}
