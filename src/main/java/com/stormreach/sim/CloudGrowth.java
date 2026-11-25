package com.stormreach.sim;

/**
 * How a storm cloud develops over its life (0..1): a flat cumulus base swells into a towering cumulus, the tower
 * shoots up to the tropopause and spreads into an anvil (a mature cumulonimbus), then the updraft dies, the tower
 * slumps and the cloud thins out. The renderer draws the cloud from these numbers, so you can watch it build.
 */
public final class CloudGrowth {
    private CloudGrowth() {}

    /** Fraction of its full height the tower has reached. */
    public static double tower(double f) {
        f = clamp(f, 0, 1);
        double up = smooth(f / 0.45);
        double slump = f > 0.8 ? 0.25 * smooth((f - 0.8) / 0.2) : 0;
        return Math.max(0.08, up - slump);
    }

    /** How far the anvil has spread, 0..1 of its full reach. */
    public static double anvil(double f) {
        return smooth((clamp(f, 0, 1) - 0.35) / 0.3);
    }

    /** Width of the base, 0..1. */
    public static double base(double f) {
        return 0.4 + 0.6 * smooth(clamp(f, 0, 1) / 0.3);
    }

    /** Opacity: fades in at birth and out as it dissipates. */
    public static double opacity(double f) {
        f = clamp(f, 0, 1);
        if (f < 0.05) return f / 0.05;
        if (f > 0.85) return Math.max(0, (1 - f) / 0.15);
        return 1;
    }

    static double smooth(double x) {
        x = clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
