package com.stormreach.client;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.Units;

/** Formats readings on the client in the player's chosen units (config/stormreach.properties: units=imperial|metric). */
public final class ClientUnits {
    private ClientUnits() {}

    private static Units.System sent;
    private static Object sentTo;

    /** 6.0: tells the server this player's units on joining and whenever the setting changes (for chat readings). */
    public static void init() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.getConnection() == null) { sentTo = null; return; }
            if (sentTo != mc.getConnection() || sent != system()) {
                sentTo = mc.getConnection();     // a new connection (join, server switch) sends again
                sent = system();
                try {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.stormreach.units.PlayerUnits.Payload(sent == Units.System.METRIC));
                } catch (RuntimeException ignored) {
                    // a server without Stormreach: chat readings there aren't ours anyway
                }
            }
        });
    }

    public static Units.System system() { return StormreachConfig.units; }

    public static String speed(double mph) { return Units.speed(mph, system()); }
    public static String temperature(double c) { return Units.temperature(c, system()); }
    public static String pressure(double hpa) { return Units.pressure(hpa, system()); }
    public static String distance(double m) { return Units.distance(m, system()); }
    public static String smallLength(double mm) { return Units.smallLength(mm, system()); }
}
