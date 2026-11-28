package com.stormreach.sim;

/**
 * Visual appearance of the condensation funnel. Morphology describes what a tornado <i>looks like</i>;
 * it says nothing by itself about intensity (a rope can be violent, a wedge can be weak), though some
 * appearances are statistically more common at certain intensities. Width values are typical funnel/damage
 * core diameters in metres (1 block = 1 m) before the gameplay size scale is applied.
 */
public enum TornadoMorphology {
    /** Very thin, often sinuous; typical of the decaying ("roping out") stage or weak tornadoes. */
    ROPE(8, 30, 0.95),
    /** Extremely narrow, straight, needle-like funnel. */
    NEEDLE(5, 20, 0.97),
    /** Narrow, tapered, sometimes twisted funnel ("drill bit"). */
    DRILL_BIT(15, 50, 0.85),
    /** Classic funnel wide at cloud base, narrowing to the ground. */
    CONE(40, 200, 0.55),
    /** Roughly cylindrical funnel of nearly constant width. */
    STOVEPIPE(80, 300, 0.15),
    /** Appears wider than it is tall from ground level; usually large, intense tornadoes. */
    WEDGE(300, 1000, 0.05),
    /** Exceptionally wide wedge (over ~1 km). */
    LARGE_WEDGE(1000, 2600, 0.0);

    public final int minWidth;
    public final int maxWidth;
    /** How much the funnel tapers toward the ground (0 = cylinder, 1 = point). */
    public final double taper;

    TornadoMorphology(int minWidth, int maxWidth, double taper) {
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.taper = taper;
    }

    /**
     * Relative funnel radius at a fraction of the way from the ground (0) to cloud base (1).
     * Returns 1 at cloud base for all shapes.
     */
    public double radiusProfile(double heightFraction) {
        double h = Math.max(0, Math.min(1, heightFraction));
        // 6.0: every type has its own silhouette instead of one cone stretched to different sizes
        double base = switch (this) {
            case ROPE -> 0.16 + 0.84 * Math.pow(h, 4);                          // a thin cord that only widens right under the cloud
            case NEEDLE -> 0.12 + 0.25 * h + 0.63 * Math.pow(h, 6);              // straight and narrow all the way down
            case DRILL_BIT -> 0.14 + 0.86 * Math.pow(h, 0.8);                   // tapered spike
            case CONE -> 0.2 + 0.8 * Math.pow(h, 1.35);                         // classic inverted cone, sides bowing outward
            case STOVEPIPE -> 0.9 + 0.1 * h;                                    // a column of nearly constant width
            case WEDGE -> 0.92 + 0.08 * h + 0.12 * Math.max(0, 0.15 - h) / 0.15; // broader than tall, flaring at the ground
            case LARGE_WEDGE -> 1.0 + 0.04 * h + 0.18 * Math.max(0, 0.2 - h) / 0.2; // a wall of cloud reaching the ground
        };
        return Math.max(0.05, base);
    }
}
