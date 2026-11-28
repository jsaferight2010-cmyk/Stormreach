package com.stormreach.sim;

/**
 * 6.0: the shape of a Wind Generator's wind: a cone that widens downwind (half-width 1.5 blocks at the fan, growing
 * 0.35 a block), strongest on its axis and fading toward the reach. Returns the fraction (0..1) of the fan's speed
 * felt at a point relative to the fan's mouth. Pure Java so it is unit-tested.
 */
public final class FanField {
    private FanField() {}

    public static double strength(double relX, double relY, double relZ, double dirX, double dirZ, int radius) {
        double along = relX * dirX + relZ * dirZ;
        if (along < -1.5 || along > radius) return 0;
        double latX = relX - along * dirX, latZ = relZ - along * dirZ;
        double half = 1.5 + Math.max(0, along) * 0.35;
        double lat = Math.sqrt(latX * latX + latZ * latZ + relY * relY);
        if (lat > half) return 0;
        double reach = Math.pow(1 - Math.max(0, along) / radius, 0.7);
        double core = 1 - (lat / half) * (lat / half);
        return reach * (0.35 + 0.65 * core);
    }
}
