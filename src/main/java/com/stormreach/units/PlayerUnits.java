package com.stormreach.units;

import com.stormreach.sim.Units;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 6.0: each player's own display units on a server. The client tells the server its {@code units} setting when it
 * joins (and whenever the config is saved), so chat readings from instruments, the radio and tornado surveys come out
 * in that player's units. A player who never sent one (a vanilla client, a GameTest mock) gets the server's setting.
 */
public final class PlayerUnits {
    private PlayerUnits() {}

    private static final Map<UUID, Units.System> CHOSEN = new ConcurrentHashMap<>();

    public record Payload(boolean metric) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Stormreach.id("units"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (b, p) -> b.writeBoolean(p.metric()), b -> new Payload(b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, ctx) ->
                CHOSEN.put(ctx.player().getUUID(), payload.metric() ? Units.System.METRIC : Units.System.IMPERIAL));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CHOSEN.remove(handler.player.getUUID()));
    }

    public static Units.System of(Player p) {
        return p == null ? StormreachConfig.units : CHOSEN.getOrDefault(p.getUUID(), StormreachConfig.units);
    }

    public static void set(Player p, Units.System s) { CHOSEN.put(p.getUUID(), s); }

    public static String speed(Player p, double mph) { return Units.speed(mph, of(p)); }
    public static String temperature(Player p, double c) { return Units.temperature(c, of(p)); }
    public static String pressure(Player p, double hpa) { return Units.pressure(hpa, of(p)); }
    public static String distance(Player p, double m) { return Units.distance(m, of(p)); }
    public static String smallLength(Player p, double mm) { return Units.smallLength(mm, of(p)); }
}
