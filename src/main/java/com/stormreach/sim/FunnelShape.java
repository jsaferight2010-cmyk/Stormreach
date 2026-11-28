package com.stormreach.sim;

/**
 * The visible condensation funnel over a tornado's life. It forms as a bulge under the cloud base, lowers itself
 * until it reaches the ground (touchdown), stays down through the mature stage, and lifts back into the cloud as
 * it ropes out. Damage and wind effects only happen while it is down (see {@link #touchedDown}).
 */
public final class FunnelShape {
    private FunnelShape() {}

    /** Lifetime fraction by which the funnel has lowered all the way down. */
    public static final double DESCENT_END = 0.14;
    /** Lifetime fraction at which the funnel starts lifting off. */
    public static final double LIFT_START = 0.955;

    /** How far the funnel reaches down from the cloud base: 0 = still in the cloud, 1 = on the ground. */
    public static double reach(double f) {
        f = clamp(f, 0, 1);
        if (f < DESCENT_END) return smooth(f / DESCENT_END);
        if (f > LIFT_START) return 1 - smooth((f - LIFT_START) / (1 - LIFT_START));
        return 1;
    }

    /** The funnel is on the ground: block damage, debris and wind forces are active. */
    public static boolean touchedDown(double reach) {
        return reach >= 0.985;
    }

    /** The circulation is spinning at the ground under a descending funnel: show the dust whirl. */
    public static boolean dustWhirl(double reach) {
        return reach > 0.35;
    }

    /** Height above the ground of the funnel's lowest point, blocks. */
    public static double tipHeight(double cloudBase, double reach) {
        return cloudBase * (1 - clamp(reach, 0, 1));
    }

    /**
     * Funnel radius at height {@code y} above the ground, in blocks.
     * {@code halfWidth} is half the current visual width. Returns 0 below the tip.
     */
    public static double radius(TornadoMorphology m, double halfWidth, double y, double cloudBase, double reach) {
        if (cloudBase <= 0) return 0;
        double tip = tipHeight(cloudBase, reach);
        if (y < tip) return 0;
        double h = clamp(y / cloudBase, 0, 1);
        double r = halfWidth * m.radiusProfile(h);
        if (!touchedDown(reach)) {
            // a descending funnel ends in a rounded, pendant tip instead of a flat cut
            double taper = Math.max(3, cloudBase * 0.2);
            double k = clamp((y - tip) / taper, 0, 1);
            r *= Math.sqrt(k);
        }
        // where it meets the cloud base the funnel flares into the lowered wall cloud
        if (h > 0.88) r *= 1 + Math.pow((h - 0.88) / 0.12, 2) * 1.4;
        return Math.max(0, r);
    }

    static double smooth(double x) {
        x = clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
