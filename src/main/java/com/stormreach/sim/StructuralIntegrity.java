package com.stormreach.sim;

/**
 * Wind resistance of blocks. Base thresholds are the ground-relative 3-second gusts (mph) at which a
 * material class typically fails, loosely following the EF scale's damage indicators: tree branches and
 * foliage go in EF0 winds, windows and light roofing in EF0–EF1, wood framing in EF1–EF2, whole trees and
 * heavy timber in EF2, unreinforced masonry in EF3, well-built stone in EF4, and only EF5 winds scour soil
 * or wreck metal. Support, connections and shelter raise the threshold; exposure and thin shapes lower it.
 */
public final class StructuralIntegrity {
    public enum MaterialClass {
        FOLIAGE(45, true), GLASS(75, true), FABRIC(85, true), LIGHT_WOOD(105, true), HEAVY_WOOD(125, true),
        MASONRY(150, false), STONE(170, false), LOOSE_EARTH(185, false), METAL(205, false),
        UNBREAKABLE(Double.POSITIVE_INFINITY, false);

        public final double baseMph;
        /** Light enough to be lifted and carried as debris rather than just breaking apart. */
        public final boolean liftable;

        MaterialClass(double baseMph, boolean liftable) {
            this.baseMph = baseMph;
            this.liftable = liftable;
        }
    }

    private StructuralIntegrity() {}

    /**
     * @param solidNeighbors number of the 6 neighbours that are solid (connections)
     * @param supportedBelow whether the block rests on a solid block
     * @param exposed        whether at least one face touches open air (wind can reach it)
     * @param thinShape      slabs, stairs, panes, fences, doors and similar
     * @param positionHash   any per-position hash, used for a deterministic +/-8% construction variance
     */
    public static double failureGustMph(MaterialClass material, int solidNeighbors, boolean supportedBelow,
                                        boolean exposed, boolean thinShape, long positionHash) {
        if (material == MaterialClass.UNBREAKABLE) return Double.POSITIVE_INFINITY;
        double t = material.baseMph;
        t *= 1.0 + 0.04 * Math.min(6, Math.max(0, solidNeighbors));
        t *= supportedBelow ? 1.08 : 0.85;
        if (!exposed) t *= 1.6;
        if (thinShape) t *= 0.9;
        double variance = ((positionHash * 0x9E3779B97F4A7C15L >>> 40) % 1000) / 1000.0; // 0..1
        t *= 0.92 + 0.16 * variance;
        return t;
    }

    public static boolean fails(double localGustMph, double thresholdMph) {
        return localGustMph >= thresholdMph;
    }
}
