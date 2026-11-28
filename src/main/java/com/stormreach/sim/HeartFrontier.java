package com.stormreach.sim;

/**
 * 6.0: the arithmetic of an overcharged Stormheart's frontier, kept pure so it is unit-tested. The frontier is a ring
 * {@link #BAND} blocks wide at radius R. Each powered second samples some columns in the ring; R grows by one block
 * once the samples add up to the ring's area (so a wide ring takes longer, and growth slows as it spreads).
 */
public final class HeartFrontier {
    private HeartFrontier() {}

    public static final int BAND = 6;

    /** Columns sampled per powered second at a spread speed of 1 to 10. */
    public static int columnsPerCycle(int spreadSpeed) { return 24 + 12 * Math.max(1, Math.min(10, spreadSpeed)); }

    /** Samples needed before the frontier at {@code radius} moves out one block. */
    public static double samplesPerStep(int radius) { return Math.max(12, 2 * Math.PI * Math.max(1, radius) * 0.75); }

    /** The radius a sample at fraction {@code f} (0..1) of the band falls on: inside the ring [R - BAND, R]. */
    public static double sampleRadius(int radius, double f) { return Math.max(0, radius - BAND * f); }

    /** Whether the frontier may grow past {@code radius}, given the cap (0 = none). */
    public static boolean mayGrow(int radius, int cap) { return cap <= 0 || radius < cap; }
}
