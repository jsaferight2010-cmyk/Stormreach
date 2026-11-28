package com.stormreach.story;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * 6.0: the Cyrrus intro as a film. The server stays in charge of the story (the boss is invulnerable until the fight,
 * lines and actions play on its timeline); these packets only tell clients to run the camera: {@link Start} when the
 * intro begins (with the altar, the boss's entity id and the tick it's already at, so a player who arrives late joins
 * in sync), {@link End} when the fight starts. A player can skip: their camera returns at once, and when every player
 * watching has skipped, the intro fast-forwards straight to the fight (in single-player that is just you).
 */
public final class CinematicSync {
    private CinematicSync() {}

    public record Start(int entityId, BlockPos altar, int atTick, int length) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(Stormreach.id("cinematic_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Start> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.entityId); b.writeBlockPos(p.altar); b.writeVarInt(p.atTick); b.writeVarInt(p.length); },
                b -> new Start(b.readVarInt(), b.readBlockPos(), b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record End(int entityId) implements CustomPacketPayload {
        public static final Type<End> TYPE = new Type<>(Stormreach.id("cinematic_end"));
        public static final StreamCodec<RegistryFriendlyByteBuf, End> CODEC = StreamCodec.of((b, p) -> b.writeVarInt(p.entityId), b -> new End(b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Skip(int entityId) implements CustomPacketPayload {
        public static final Type<Skip> TYPE = new Type<>(Stormreach.id("cinematic_skip"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Skip> CODEC = StreamCodec.of((b, p) -> b.writeVarInt(p.entityId), b -> new Skip(b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(Start.TYPE, Start.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(End.TYPE, End.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Skip.TYPE, Skip.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Skip.TYPE, (p, ctx) -> {
            if (ctx.player().level().getEntity(p.entityId()) instanceof CyrrusBoss boss) boss.skipRequested(ctx.player());
        });
    }

    /** GameTests: the same as that player pressing skip. */
    public static void skipForTest(CyrrusBoss boss, ServerPlayer p) { boss.skipRequested(p); }

    static void send(ServerPlayer p, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(p, payload.type())) ServerPlayNetworking.send(p, payload);
    }
}
