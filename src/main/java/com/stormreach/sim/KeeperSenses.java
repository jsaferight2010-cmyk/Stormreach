package com.stormreach.sim;

/**
 * How the Hollow Keeper hears. Every player makes a noise radius from what they're doing: standing still makes
 * none, sneaking very little, walking some, sprinting a lot, and fighting or breaking blocks the most. The Keeper
 * hears you only inside that radius, and its idea of where you are gets sharper the closer you are.
 */
public final class KeeperSenses {
    private KeeperSenses() {}

    /** Noise radius in blocks. {@code loud} = attacked, broke or placed a block in the last few seconds. */
    public static double noiseRadius(boolean moving, boolean sneaking, boolean sprinting, boolean loud) {
        if (loud) return 32;
        if (!moving) return 0;
        if (sprinting) return 28;
        if (sneaking) return 4;
        return 14;
    }

    public static boolean hears(double distance, double noiseRadius, boolean enraged) {
        return distance <= noiseRadius * (enraged ? 1.4 : 1.0);
    }

    /** How far off (blocks) its guess of your position can be. */
    public static double guessError(double distance) {
        return Math.min(8, 1 + distance * 0.25);
    }

    /** Ticks between listens: it listens harder once wounded. */
    public static int listenInterval(float healthFraction) {
        return healthFraction < 0.5f ? 12 : 20;
    }

    /** Whether to leap at a visible target this tick (distance in blocks, cooldown remaining in ticks). */
    public static boolean lunge(double distance, int cooldown) {
        return cooldown <= 0 && distance >= 4 && distance <= 9;
    }
}
