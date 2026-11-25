package com.stormreach.client;

import com.stormreach.Stormreach;
import com.stormreach.client.mixin.EntityRenderersAccessor;
import com.stormreach.registry.ModFluids;
import com.stormreach.storm.StormBlocks;
import com.stormreach.storm.StormMobs;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/** Client side of the 3.0 content: storm block tint, storm mob renderers, and the two storm fluids. */
public final class StormClientContent {
    public static final int STORM_TINT = 0xFF9FB4D8;

    private StormClientContent() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void init() {
        // Storm blocks reuse vanilla textures; their models carry a tint index that we colour storm-blue.
        Block[] blocks = StormBlocks.STORM_OF.values().toArray(new Block[0]);
        if (blocks.length > 0) BlockColorRegistry.register(List.of(BlockTintSources.constant(STORM_TINT), BlockTintSources.constant(STORM_TINT)), blocks);

        Map<EntityType<?>, EntityRendererProvider<?>> providers = EntityRenderersAccessor.stormreach$providers();
        int missing = 0;
        for (Map.Entry<EntityType<?>, EntityType<?>> e : StormMobs.STORM_OF.entrySet()) {
            EntityRendererProvider provider = providers.get(e.getKey());
            if (provider == null) { missing++; continue; }
            EntityRendererRegistry.register((EntityType) e.getValue(), provider);
        }
        if (missing > 0) Stormreach.LOGGER.warn("Stormreach: {} storm mobs had no vanilla renderer to copy", missing);

        FluidRenderingRegistry.register(ModFluids.STORMWATER, ModFluids.FLOWING_STORMWATER, new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/water_still")),
                new Material(Identifier.withDefaultNamespace("block/water_flow")),
                new Material(Identifier.withDefaultNamespace("block/water_overlay")),
                state -> 0x3A6FB8));
        FluidRenderingRegistry.register(ModFluids.STORMLAVA, ModFluids.FLOWING_STORMLAVA, new FluidModel.Unbaked(
                new Material(Stormreach.id("block/stormlava_still")),
                new Material(Stormreach.id("block/stormlava_flow")),
                null,
                null));
    }
}
