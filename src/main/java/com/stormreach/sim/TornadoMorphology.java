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
        double base = 1.0 - taper * (1.0 - h);
        if (this == ROPE) {
            // ropes thin out quickly and stay thin
            base = 0.2 + 0.8 * Math.pow(h, 3);
        } else if (this == WEDGE || this == LARGE_WEDGE) {
            // flared top, broad base
            base = 0.85 + 0.15 * h;
        }
        return Math.max(0.05, base);
    }
}
