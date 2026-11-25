package com.stormreach.client;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.Units;

/** Formats readings on the client in the player's chosen units (config/stormreach.properties: units=imperial|metric). */
public final class ClientUnits {
    private ClientUnits() {}

    public static Units.System system() { return StormreachConfig.units; }

    public static String speed(double mph) { return Units.speed(mph, system()); }
    public static String temperature(double c) { return Units.temperature(c, system()); }
    public static String pressure(double hpa) { return Units.pressure(hpa, system()); }
    public static String distance(double m) { return Units.distance(m, system()); }
    public static String smallLength(double mm) { return Units.smallLength(mm, system()); }
}
