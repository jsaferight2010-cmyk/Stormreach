package com.stormreach.client;

import com.stormreach.weather.WeatherHud;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The latest local-weather reading from the server (once a second), for the HUD and for animating instruments. */
public final class ClientWeather {
    /** 6.0: forget the last session (called when the connection changes). */
    public static void resetSession() {
        last = null;
        receivedAt = 0;
    }

    private ClientWeather() {}

    private static WeatherHud.Payload last;
    private static long receivedAt;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(WeatherHud.Payload.TYPE, (payload, ctx) -> { last = payload; receivedAt = System.currentTimeMillis(); });
    }

    public static WeatherHud.Payload last() {
        return last != null && System.currentTimeMillis() - receivedAt < 5000 ? last : null;
    }

    /** Wind blowing toward (x, z), unit length; north-easterly breeze when nothing has arrived yet. */
    public static float[] windDir() {
        WeatherHud.Payload p = last();
        return p == null ? new float[]{-0.7f, 0.7f} : new float[]{p.windDirX(), p.windDirZ()};
    }

    public static float windMph() {
        WeatherHud.Payload p = last();
        return p == null ? 6 : p.windMph();
    }
}
