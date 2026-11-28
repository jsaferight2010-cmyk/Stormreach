package com.stormreach.weather.precip;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Hail, hail on the ground, and rain puddles. */
public final class PrecipContent {
    private PrecipContent() {}

    public static final Block HAIL_LAYER = block("hail_layer", HailLayerBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.ICE).replaceable().forceSolidOff().randomTicks().strength(0.1f).sound(SoundType.GLASS)
            .friction(0.9f).pushReaction(PushReaction.POPPED).noLootTable());
    public static final Block PUDDLE = block("puddle", PuddleBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER).replaceable().noCollision().noOcclusion().randomTicks().instabreak().sound(SoundType.EMPTY)
            .pushReaction(PushReaction.POPPED).noLootTable());
    public static final Item HAIL_LAYER_ITEM = item("hail_layer", HAIL_LAYER);
    /** 6.0: so creative players can pick, place and clear puddles. */
    public static final Item PUDDLE_ITEM = item("puddle", PUDDLE);

    public static final EntityType<HailstoneEntity> HAILSTONE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("hailstone")),
            EntityType.Builder.<HailstoneEntity>of(HailstoneEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).noSave()
                    .clientTrackingRange(6).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("hailstone"))));

    public static final EntityType<com.stormreach.weather.cloud.StormCellEntity> STORM_CELL = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("storm_cell")),
            EntityType.Builder.<com.stormreach.weather.cloud.StormCellEntity>of(com.stormreach.weather.cloud.StormCellEntity::new, MobCategory.MISC)
                    .sized(4f, 4f).fireImmune().clientTrackingRange(48).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("storm_cell"))));

    public static void init() {}

    private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
    }

    private static Item item(String name, Block b) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(b, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        com.stormreach.tornado.WeatherItems.WEATHER_EVENTS.add(it);
        return it;
    }
}
