package com.stormreach.sim;

/**
 * Pure Doppler-radar geometry and colour scales, shared by the server scan and the client scope (and unit-tested).
 *
 * A scan is a polar grid: {@link #AZIMUTHS} azimuth bins (0 = north, clockwise, like a real PPI) by {@link #GATES}
 * range gates. Each gate stores reflectivity in dBZ (0..80, or {@link #NO_ECHO}) and radial velocity in m/s
 * (-127..127, positive = moving away from the radar, or {@link #NO_DATA} where there is nothing to bounce off).
 */
public final class RadarMath {
    private RadarMath() {}

    public static final int AZIMUTHS = 90, GATES = 40;
    public static final byte NO_DATA = Byte.MIN_VALUE;
    public static final byte NO_ECHO = 0;

    public static int index(int az, int gate) { return az * GATES + gate; }

    /** Compass bearing in degrees (0 north, 90 east) of offset (dx, dz) in Minecraft axes (+z is south). */
    public static double bearing(double dx, double dz) {
        double b = Math.toDegrees(Math.atan2(dx, -dz));
        return b < 0 ? b + 360 : b;
    }

    /** Centre of an azimuth bin in degrees. */
    public static double azimuthCentre(int az) { return (az + 0.5) * 360.0 / AZIMUTHS; }

    /** Offset (dx, dz) of the centre of a gate for a scan of the given range (blocks = metres). */
    public static double[] gateOffset(int az, int gate, double range) {
        double a = Math.toRadians(azimuthCentre(az));
        double r = (gate + 0.5) * range / GATES;
        return new double[]{Math.sin(a) * r, -Math.cos(a) * r};
    }

    /** Which gate a point falls in, or -1 outside the scan. Returns {az, gate}. */
    public static int[] gateOf(double dx, double dz, double range) {
        double r = Math.hypot(dx, dz);
        if (r >= range) return null;
        int az = (int) (bearing(dx, dz) / 360.0 * AZIMUTHS) % AZIMUTHS;
        return new int[]{az, (int) (r / range * GATES)};
    }

    /**
     * Radial velocity seen by a radar: the component of the wind (u east, v south, m/s) along the beam from the radar
     * to a target at offset (dx, dz). Positive means the air moves away from the radar (outbound), negative toward it.
     */
    public static double radial(double u, double v, double dx, double dz) {
        double r = Math.hypot(dx, dz);
        if (r < 1e-6) return 0;
        return (u * dx + v * dz) / r;
    }

    /** Quantises a velocity to the stored byte (1 m/s steps, clamped so it never collides with NO_DATA). */
    public static byte packVelocity(double ms) {
        return (byte) Math.max(-127, Math.min(127, Math.round(ms)));
    }

    public static byte packDbz(double dbz) {
        return (byte) Math.max(0, Math.min(80, Math.round(dbz)));
    }

    /** NWS-style reflectivity colours (ARGB), transparent below 5 dBZ. */
    public static int dbzColour(int dbz) {
        if (dbz < 5) return 0;
        int[][] scale = {{5, 0xFF04E9E7}, {10, 0xFF019FF4}, {15, 0xFF0300F4}, {20, 0xFF02FD02}, {25, 0xFF01C501}, {30, 0xFF008E00},
                {35, 0xFFFDF802}, {40, 0xFFE5BC00}, {45, 0xFFFD9500}, {50, 0xFFFD0000}, {55, 0xFFD40000}, {60, 0xFFBC0000},
                {65, 0xFFF800FD}, {70, 0xFF9854C6}, {75, 0xFFFDFDFD}};
        int c = scale[0][1];
        for (int[] s : scale) if (dbz >= s[0]) c = s[1];
        return c;
    }

    /**
     * Velocity colours: greens toward the radar, reds away, darker for weak motion, near-white at the extremes; a
     * dull grey band for |v| < 1 m/s (range folding aside, that is how a real velocity scope reads).
     */
    public static int velocityColour(int ms) {
        int a = Math.abs(ms);
        if (a < 1) return 0xFF6A6A6A;
        double t = Math.min(1.0, a / 60.0);
        int lo, hi;
        if (ms < 0) { lo = 0xFF0B4A12; hi = 0xFF00FF66; } else { lo = 0xFF5A0A0A; hi = 0xFFFF2A2A; }
        if (a > 55) return ms < 0 ? 0xFFB8FFD8 : 0xFFFFD0E8;
        return lerp(lo, hi, t);
    }

    static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Reflectivity contribution of a precipitation core: dBZ peak at the centre, Gaussian falloff with radius. */
    public static double coreDbz(double peakDbz, double radius, double dist) {
        if (radius <= 0) return 0;
        double q = dist / radius;
        return peakDbz * Math.exp(-q * q * 1.6);
    }

    /** dBZ values combine as powers (Z = 10^(dBZ/10)), not by adding decibels. */
    public static double combineDbz(double a, double b) {
        if (a <= 0) return b;
        if (b <= 0) return a;
        return 10 * Math.log10(Math.pow(10, a / 10) + Math.pow(10, b / 10));
    }
}
