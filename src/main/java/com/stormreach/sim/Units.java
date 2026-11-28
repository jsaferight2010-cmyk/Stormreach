package com.stormreach.sim;

import java.util.Locale;

/**
 * Display units for every reading in the mod. US customary is the default (mph, degrees F, inches, inHg, feet and
 * miles); metric shows km/h, degrees C, millimetres, hPa and metres/kilometres. The simulation always works in SI
 * (m/s, Celsius, metres, hPa) and converts only for display. Pure Java so it is unit-tested.
 */
public final class Units {
    private Units() {}

    public enum System {
        IMPERIAL, METRIC;

        public static System parse(String s) {
            return s != null && s.trim().toLowerCase(Locale.ROOT).startsWith("m") ? METRIC : IMPERIAL;
        }
    }

    public static double cToF(double c) { return c * 9.0 / 5.0 + 32.0; }
    public static double fToC(double f) { return (f - 32.0) * 5.0 / 9.0; }
    public static double mphToKmh(double mph) { return mph * 1.609344; }
    public static double hpaToInHg(double hpa) { return hpa * 0.0295299830714; }
    public static double mmToIn(double mm) { return mm / 25.4; }
    public static double mToFt(double m) { return m * 3.28084; }

    public static String speed(double mph, System s) {
        return s == System.METRIC ? Math.round(mphToKmh(mph)) + " km/h" : Math.round(mph) + " mph";
    }

    public static String temperature(double celsius, System s) {
        return s == System.METRIC ? Math.round(celsius) + "°C" : Math.round(cToF(celsius)) + "°F";
    }

    public static String pressure(double hpa, System s) {
        return s == System.METRIC ? String.format(Locale.ROOT, "%.0f hPa", hpa) : String.format(Locale.ROOT, "%.2f inHg", hpaToInHg(hpa));
    }

    /** Small lengths: hailstone size, rainfall. */
    public static String smallLength(double mm, System s) {
        return s == System.METRIC ? String.format(Locale.ROOT, "%.0f mm", mm) : String.format(Locale.ROOT, "%.2f in", mmToIn(mm));
    }

    /** Distances in blocks (1 block = 1 m): feet below a tenth of a mile, then miles; metres then kilometres. */
    public static String distance(double metres, System s) {
        if (s == System.METRIC) return metres < 1000 ? Math.round(metres) + " m" : String.format(Locale.ROOT, "%.1f km", metres / 1000);
        double ft = mToFt(metres);
        return ft < 528 ? Math.round(ft) + " ft" : String.format(Locale.ROOT, "%.1f mi", ft / 5280);
    }

    /** Short unit label for a speed axis. */
    public static String speedUnit(System s) { return s == System.METRIC ? "km/h" : "mph"; }

    /** A speed value converted to the display unit (no label). */
    public static double speedValue(double mph, System s) { return s == System.METRIC ? mphToKmh(mph) : mph; }
}
