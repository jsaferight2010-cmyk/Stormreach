package com.stormreach.chase;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Storm chasing (5.2): the intercept vehicle, weather balloons, the rain gauge, the tornado probe and the logbook. */
public final class ChaseRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();

    public static final Block RAIN_GAUGE = block("rain_gauge", RainGaugeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
            .strength(0.5f).sound(SoundType.GLASS).noOcclusion());
    public static final Block TORNADO_PROBE = block("tornado_probe", TornadoProbeBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.COLOR_ORANGE).strength(4.0f, 1200f).noOcclusion().lightLevel(s -> s.getValue(TornadoProbeBlock.RECORDED) ? 7 : 3));
    public static final BlockEntityType<TornadoProbeBlockEntity> PROBE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Stormreach.id("tornado_probe"), new BlockEntityType<>(TornadoProbeBlockEntity::new, Set.of(TORNADO_PROBE)));

    public static final EntityType<InterceptVehicle> INTERCEPT_VEHICLE_ENTITY = entity("intercept_vehicle",
            EntityType.Builder.of(InterceptVehicle::new, MobCategory.MISC).sized(2.4f, 2.6f).eyeHeight(1.6f).passengerAttachments(0.25f)   /* 6.0: the driver sits inside the armoured cab, eyes at the slit window */
                    .fireImmune().clientTrackingRange(12));
    public static final EntityType<WeatherBalloon> WEATHER_BALLOON_ENTITY = entity("weather_balloon",
            EntityType.Builder.of(WeatherBalloon::new, MobCategory.MISC).sized(0.9f, 1.8f).eyeHeight(1.0f).clientTrackingRange(16));

    public static final Item INTERCEPT_VEHICLE = item("intercept_vehicle", ChaseItems.VehicleItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item WEATHER_BALLOON = item("weather_balloon", ChaseItems.BalloonItem::new, new Item.Properties().stacksTo(16));
    public static final Item CHASERS_LOGBOOK = item("chasers_logbook", ChaseItems.LogbookItem::new, new Item.Properties().stacksTo(1));

    private ChaseRegistry() {}

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

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(INTERCEPT_VEHICLE_ENTITY, InterceptVehicle.createAttributes());
        FabricDefaultAttributeRegistry.register(WEATHER_BALLOON_ENTITY, WeatherBalloon.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(ChaserLog::tick);
    }
}
