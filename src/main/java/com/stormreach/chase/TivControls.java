package com.stormreach.chase;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 6.0: the TIV anchor key. The client sends one packet per key press; the server checks the sender is the driver. */
public final class TivControls {
    private TivControls() {}

    public record AnchorPayload() implements CustomPacketPayload {
        public static final Type<AnchorPayload> TYPE = new Type<>(Stormreach.id("tiv_anchor"));
        public static final StreamCodec<FriendlyByteBuf, AnchorPayload> CODEC = StreamCodec.unit(new AnchorPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(AnchorPayload.TYPE, AnchorPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AnchorPayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> {
            if (ctx.player().getVehicle() instanceof InterceptVehicle v) v.toggleAnchors(ctx.player());
        }));
    }
}
