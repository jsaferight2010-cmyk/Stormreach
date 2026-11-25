package com.stormreach.weather;

import com.stormreach.Stormreach;
import com.stormreach.chase.InterceptVehicle;
import com.stormreach.sim.EFScale;
import com.stormreach.sky.ChaserGear;
import com.stormreach.tornado.Instruments;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0: one small packet a second to every player with the local weather (wind direction and speed, temperature,
 * pressure) and, for players wearing Storm Chaser Goggles or driving a TIV, the nearest tornado. The client draws it
 * as a tidy panel in the top corner of the HUD (in the player's units) instead of an action-bar line, and uses the
 * wind to animate anemometers, vanes and turbines.
 */
public final class WeatherHud {
    private WeatherHud() {}

    public static final int MODE_NONE = 0, MODE_GOGGLES = 1, MODE_TIV = 2;

    public record Payload(int mode, float windDirX, float windDirZ, float windMph, float gustMph, float tempC, float pressureHpa,
                          boolean tornado, float distance, float bearingDeg, float headingDeg, float motionMph, float peakMph,
                          int efRating, float hereMph, float speedMph, boolean anchored) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Stormreach.id("weather_hud"));
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.ofMember((p, b) -> {
            b.writeVarInt(p.mode); b.writeFloat(p.windDirX); b.writeFloat(p.windDirZ); b.writeFloat(p.windMph); b.writeFloat(p.gustMph);
            b.writeFloat(p.tempC); b.writeFloat(p.pressureHpa); b.writeBoolean(p.tornado); b.writeFloat(p.distance); b.writeFloat(p.bearingDeg);
            b.writeFloat(p.headingDeg); b.writeFloat(p.motionMph); b.writeFloat(p.peakMph); b.writeVarInt(p.efRating + 1); b.writeFloat(p.hereMph);
            b.writeFloat(p.speedMph); b.writeBoolean(p.anchored);
        }, b -> new Payload(b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                b.readBoolean(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt() - 1, b.readFloat(),
                b.readFloat(), b.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(p, Payload.TYPE)) continue;
            ServerPlayNetworking.send(p, measure(p));
        }
    }

    /** Builds the reading for one player. Public for GameTests. */
    public static Payload measure(ServerPlayer p) {
        ServerLevel level = p.level();
        int mode = p.getVehicle() instanceof InterceptVehicle ? MODE_TIV : ChaserGear.wearsGoggles(p) ? MODE_GOGGLES : MODE_NONE;
        Vec3 w = StormWeather.windVector();
        double[] wind = Instruments.windMph(level, p.blockPosition());
        double t = Instruments.temperatureC(level, p.blockPosition()), hpa = Instruments.pressureHpa(level, p.blockPosition());
        TornadoEntity best = null;
        double bestD = mode == MODE_NONE ? 0 : 1024;
        if (mode != MODE_NONE) for (TornadoEntity tor : TornadoManager.live(level)) {
            double d = Math.hypot(tor.getX() - p.getX(), tor.getZ() - p.getZ());
            if (d < bestD) { bestD = d; best = tor; }
        }
        double speed = p.getVehicle() instanceof InterceptVehicle v ? v.speedMph() : 0;
        boolean anchored = p.getVehicle() instanceof InterceptVehicle v && v.isAnchored();
        if (best == null)
            return new Payload(mode, (float) w.x, (float) w.z, (float) wind[0], (float) wind[1], (float) t, (float) hpa, false, 0, 0, 0, 0, 0, -1, 0, (float) speed, anchored);
        double peak = best.currentPeakMph();
        EFScale ef = EFScale.fromGustMph(peak);
        double dx = best.getX() - p.getX(), dz = best.getZ() - p.getZ();
        var f = best.windField();
        return new Payload(mode, (float) w.x, (float) w.z, (float) wind[0], (float) wind[1], (float) t, (float) hpa, true, (float) bestD,
                (float) Math.toDegrees(Math.atan2(dx, -dz)), (float) Math.toDegrees(Math.atan2(f.translateX, -f.translateZ)),
                (float) EFScale.msToMph(Math.hypot(f.translateX, f.translateZ)), (float) peak, ef == null ? -1 : ef.ordinal(),
                (float) best.windMphAt(p.position().add(0, 2, 0)), (float) speed, anchored);
    }
}
