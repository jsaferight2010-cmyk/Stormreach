package com.stormreach.labyrinth;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** The two locker packets: the server tells a player they are (or no longer are) hiding; the player asks to step out. */
public final class LockerNet {
    private LockerNet() {}

    /** Server → client. {@code facing} is the locker's open side (a horizontal Direction's 2D id). */
    public record HidePayload(boolean hidden, BlockPos locker, int facing) implements CustomPacketPayload {
        public static final Type<HidePayload> TYPE = new Type<>(Stormreach.id("locker_hide"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HidePayload> CODEC = StreamCodec.ofMember(
                (p, buf) -> { buf.writeBoolean(p.hidden()); buf.writeBlockPos(p.locker()); buf.writeVarInt(p.facing()); },
                buf -> new HidePayload(buf.readBoolean(), buf.readBlockPos(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: the hiding player pressed Use to step out. */
    public record ExitPayload() implements CustomPacketPayload {
        public static final Type<ExitPayload> TYPE = new Type<>(Stormreach.id("locker_exit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExitPayload> CODEC = StreamCodec.unit(new ExitPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(HidePayload.TYPE, HidePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExitPayload.TYPE, ExitPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ExitPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            ctx.server().execute(() -> { if (HidingTracker.isHidden(p)) HidingTracker.leave(p, true); });
        });
    }

    static void send(ServerPlayer p, boolean hidden, BlockPos locker, int facing) {
        if (ServerPlayNetworking.canSend(p, HidePayload.TYPE)) ServerPlayNetworking.send(p, new HidePayload(hidden, locker, facing));
    }
}
