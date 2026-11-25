package com.stormreach.sim;

import java.util.Random;

/**
 * Hailstone sizes (the reference objects storm reports use) and the physics that decides how much each one hurts.
 * A hailstone's terminal speed grows with the square root of its diameter and its mass with the cube, so its impact
 * energy grows roughly with the 3.5th power of size: a golf ball is far worse than a pea, not just a bit worse.
 */
public enum HailSize {
    PEA(0.64), MARBLE(1.27), QUARTER(2.54), GOLF_BALL(4.45), BASEBALL(7.0), SOFTBALL(10.2);

    /** Diameter, cm. */
    public final double diameterCm;

    HailSize(double diameterCm) {
        this.diameterCm = diameterCm;
    }

    /** Terminal fall speed, m/s (about 9 m/s for a 1 cm stone, 30 m/s for a baseball). */
    public double terminalMs() {
        return 11.2 * Math.sqrt(diameterCm);
    }

    /** Mass, kg (solid ice, 900 kg/m³). */
    public double massKg() {
        double r = diameterCm / 200.0;
        return 900 * 4.0 / 3.0 * Math.PI * r * r * r;
    }

    /** Impact energy at terminal speed, joules. */
    public double impactJoules() {
        double v = terminalMs();
        return 0.5 * massKg() * v * v;
    }

    /** Damage to an unarmoured body (2 = one heart): a pea stings, a golf ball hurts a lot, a softball can kill. */
    public float damage() {
        return (float) (0.5 + 1.6 * (Math.log(1 + impactJoules()) / Math.log(2)));
    }

    /** Size drawn in the world, blocks across (exaggerated: real hail would be nearly invisible at block scale). */
    public float visualSize() {
        return (float) (0.07 + diameterCm * 0.032);
    }

    /** Chance one stone adds a layer to the hail on the ground. */
    public double accumulation() {
        return Math.min(1.0, 0.04 * diameterCm * diameterCm);
    }

    public boolean breaksCrops() { return this.ordinal() >= QUARTER.ordinal(); }

    public boolean breaksPanes() { return this.ordinal() >= GOLF_BALL.ordinal(); }

    public boolean breaksGlassBlocks() { return this.ordinal() >= BASEBALL.ordinal(); }

    public boolean shredsLeaves() { return this.ordinal() >= BASEBALL.ordinal(); }

    /**
     * Picks a stone's size. {@code severity} 0..1 is how strong the storm's updraft is: weak storms drop peas and
     * marbles; only the most severe storms produce the rare baseball- and softball-sized stones.
     */
    public static HailSize roll(Random r, double severity) {
        double s = Math.max(0, Math.min(1, severity));
        double[] w = {
                1.0,
                0.8,
                0.15 + 0.6 * s,
                Math.max(0, s - 0.25) * 0.8,
                Math.max(0, s - 0.55) * 0.5,
                Math.max(0, s - 0.8) * 0.4};
        double total = 0;
        for (double x : w) total += x;
        double pick = r.nextDouble() * total;
        for (int i = 0; i < w.length; i++) {
            pick -= w[i];
            if (pick <= 0) return values()[i];
        }
        return PEA;
    }

    /** The largest size a storm of this severity can produce. */
    public static HailSize maxFor(double severity) {
        if (severity > 0.8) return SOFTBALL;
        if (severity > 0.55) return BASEBALL;
        if (severity > 0.25) return GOLF_BALL;
        return QUARTER;
    }
}
