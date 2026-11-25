package com.stormreach.sim;

/** Rules for the Stranger's appearances. */
public final class ShadowRules {
    private ShadowRules() {}

    /** Cosine of the angle within which "looking straight at it" makes it vanish (about 9 degrees). */
    public static final double STARE_COS = 0.988;

    /** True when the player's look vector points almost exactly at the shadow. */
    public static boolean beingStaredAt(double lookX, double lookY, double lookZ, double toX, double toY, double toZ) {
        double l = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        double t = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
        if (l == 0 || t == 0) return false;
        return (lookX * toX + lookY * toY + lookZ * toZ) / (l * t) >= STARE_COS;
    }

    /**
     * Chance per check (one check every 30 seconds per player) that the Stranger appears.
     * Atmosphere multiplies the base rate: forests, fog/rain, night and deep shade make it likelier.
     */
    public static double appearanceChance(double baseRate, boolean forest, boolean raining, boolean night, boolean dark, int sightingsSoFar) {
        double c = baseRate;
        if (forest) c *= 1.8;
        if (raining) c *= 1.5;
        if (night) c *= 1.4;
        if (dark) c *= 1.3;
        // it grows bolder as the story advances, but stays rare
        c *= 1.0 + Math.min(10, sightingsSoFar) * 0.05;
        return Math.min(0.25, c);
    }
}
