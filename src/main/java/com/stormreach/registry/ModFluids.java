package com.stormreach.registry;

import com.stormreach.Stormreach;
import com.stormreach.fluid.StormlavaBlock;
import com.stormreach.fluid.StormlavaFluid;
import com.stormreach.fluid.StormwaterFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/** Stormwater and Stormlava: the Stormblight's versions of water and lava. */
public final class ModFluids {
    public static final FlowingFluid STORMWATER = Registry.register(BuiltInRegistries.FLUID, Stormreach.id("stormwater"), new StormwaterFluid.Source());
    public static final FlowingFluid FLOWING_STORMWATER = Registry.register(BuiltInRegistries.FLUID, Stormreach.id("flowing_stormwater"), new StormwaterFluid.Flowing());
    public static final FlowingFluid STORMLAVA = Registry.register(BuiltInRegistries.FLUID, Stormreach.id("stormlava"), new StormlavaFluid.Source());
    public static final FlowingFluid FLOWING_STORMLAVA = Registry.register(BuiltInRegistries.FLUID, Stormreach.id("flowing_stormlava"), new StormlavaFluid.Flowing());

    public static final Block STORMWATER_BLOCK = block("stormwater",
            p -> new LiquidBlock(STORMWATER, p) {}, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(s -> 6));
    public static final Block STORMLAVA_BLOCK = block("stormlava",
            p -> new StormlavaBlock(STORMLAVA, p), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).lightLevel(s -> 15));

    public static final Item STORMWATER_BUCKET = bucket("stormwater_bucket", STORMWATER);
    public static final Item STORMLAVA_BUCKET = bucket("stormlava_bucket", STORMLAVA);

    private ModFluids() {}

    private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
    }

    private static Item bucket(String name, FlowingFluid fluid) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key,
                new BucketItem(fluid, new Item.Properties().setId(key).craftRemainder(Items.BUCKET).stacksTo(1)));
        ModItems.TAB_ORDER.add(item);
        return item;
    }

    public static void init() {
        // Loading this class registers everything above.
    }
}
