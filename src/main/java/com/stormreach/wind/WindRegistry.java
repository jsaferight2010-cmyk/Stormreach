package com.stormreach.wind;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

/** 6.0: the Creative Wind Generator and its settings packet. */
public final class WindRegistry {
    private WindRegistry() {}

    public static final Block WIND_GENERATOR;
    public static final Item WIND_GENERATOR_ITEM;

    static {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id("wind_generator"));
        WIND_GENERATOR = Registry.register(BuiltInRegistries.BLOCK, key, new WindGeneratorBlock(BlockBehaviour.Properties.of().setId(key)
                .strength(-1.0f, 3600000.0f).sound(SoundType.METAL).noLootTable().pushReaction(PushReaction.IMMOVEABLE)));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id("wind_generator"));
        WIND_GENERATOR_ITEM = Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(WIND_GENERATOR,
                new Item.Properties().setId(ik).rarity(Rarity.EPIC).useBlockDescriptionPrefix()));
    }

    public static final BlockEntityType<WindGeneratorBlockEntity> GENERATOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Stormreach.id("wind_generator"), new BlockEntityType<>(WindGeneratorBlockEntity::new, java.util.Set.of(WIND_GENERATOR)));

    /** Client to server: new settings for the generator at {@code pos}. Only creative players near it are obeyed. */
    public record SettingsPayload(BlockPos pos, int mph, int radius, float gustiness, boolean on, boolean damage) implements CustomPacketPayload {
        public static final Type<SettingsPayload> TYPE = new Type<>(Stormreach.id("wind_generator"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.of((b, p) -> {
            b.writeBlockPos(p.pos()); b.writeVarInt(p.mph()); b.writeVarInt(p.radius()); b.writeFloat(p.gustiness());
            b.writeBoolean(p.on()); b.writeBoolean(p.damage());
        }, b -> new SettingsPayload(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readBoolean(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (p, ctx) -> {
            var player = ctx.player();
            if (!player.isCreative() || player.distanceToSqr(Vec3.atCenterOf(p.pos())) > 16 * 16) return;
            if (player.level().getBlockEntity(p.pos()) instanceof WindGeneratorBlockEntity be)
                be.configure(p.mph(), p.radius(), p.gustiness(), p.on(), p.damage());
        });
    }
}
