package com.stormreach.power;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Live Wire (5.5): storm electricity and captured water.
 * Power (Storm Units, SU): Wind Turbines and Water Wheels make it, Copper Cable carries it, Storm Capacitors store it
 * (and soak up lightning), and Arc Lamps, Shock Plates, Electric Pumps, Sprinklers and Tesla Coils use it.
 * Water: Rain Barrels catch rain (Stormwater in a thunderstorm) and drain into Cisterns; pumps lift water into them;
 * sprinklers spend it on crops.
 */
public final class PowerRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER);
    }

    public static final Block COPPER_CABLE = block("copper_cable", CopperCableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
            .strength(0.4f).sound(SoundType.COPPER).noOcclusion());
    public static final Block STORM_CAPACITOR = block("storm_capacitor", StormCapacitorBlock::new, metal().noOcclusion()
            .lightLevel(s -> s.getValue(StormCapacitorBlock.LEVEL) * 3));
    public static final Block WATER_WHEEL = block("water_wheel", WaterWheelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final Block ARC_LAMP = block("arc_lamp", ArcLampBlock::new, metal().strength(1.5f).sound(SoundType.LANTERN).noOcclusion()
            .lightLevel(s -> s.getValue(ArcLampBlock.LIT) ? 15 : 0));
    public static final Block SHOCK_PLATE = block("shock_plate", ShockPlateBlock::new, metal().strength(1.5f).noCollision().noOcclusion());
    public static final Block ELECTRIC_PUMP = block("electric_pump", ElectricPumpBlock::new, metal());
    public static final Block SPRINKLER = block("sprinkler", SprinklerBlock::new, metal().strength(1.5f).noOcclusion());
    public static final Block RAIN_BARREL = block("rain_barrel", p -> new TankBlock(p, 4000, true, Block.box(1, 0, 1, 15, 16, 15)),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final Block CISTERN = block("cistern", p -> new TankBlock(p, 32000, false, Block.box(0, 0, 0, 16, 16, 16)),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final BlockEntityType<StormCapacitorBlock.Entity> CAPACITOR_ENTITY = be("storm_capacitor", StormCapacitorBlock.Entity::new, STORM_CAPACITOR);
    public static final BlockEntityType<WaterWheelBlock.Entity> WATER_WHEEL_ENTITY = be("water_wheel", WaterWheelBlock.Entity::new, WATER_WHEEL);
    public static final BlockEntityType<ArcLampBlock.Entity> ARC_LAMP_ENTITY = be("arc_lamp", ArcLampBlock.Entity::new, ARC_LAMP);
    public static final BlockEntityType<ShockPlateBlock.Plate> SHOCK_PLATE_ENTITY = be("shock_plate", ShockPlateBlock.Plate::new, SHOCK_PLATE);
    public static final BlockEntityType<ElectricPumpBlock.Entity> PUMP_ENTITY = be("electric_pump", ElectricPumpBlock.Entity::new, ELECTRIC_PUMP);
    public static final BlockEntityType<SprinklerBlock.Entity> SPRINKLER_ENTITY = be("sprinkler", SprinklerBlock.Entity::new, SPRINKLER);
    public static final BlockEntityType<TankBlock.Entity> TANK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Stormreach.id("water_tank"),
            new BlockEntityType<>(TankBlock.Entity::new, Set.of(RAIN_BARREL, CISTERN)));

    public static final Item MULTIMETER = item("multimeter", MultimeterItem::new, new Item.Properties().stacksTo(1));

    private PowerRegistry() {}

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> be(String name, BlockEntityType.BlockEntitySupplier<T> f, Block b) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Stormreach.id(name), new BlockEntityType<>(f, Set.of(b)));
    }

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

    public static void init() {}
}
