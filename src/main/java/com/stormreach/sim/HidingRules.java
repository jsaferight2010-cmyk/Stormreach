package com.stormreach.sim;

/**
 * Rules for the Labyrinth guardian and hiding lockers. Pure functions so they can be tested.
 */
public final class HidingRules {
    private HidingRules() {}

    /** Ticks after the guardian last saw you during which it remembers you ducked into a locker. */
    public static final int WITNESS_WINDOW = 60;

    /**
     * Chance per search check (once a second) that the guardian finds a hidden player.
     *
     * @param distance          guardian to locker distance, blocks
     * @param movedWhileHidden  the player moved, attacked or used an item since hiding
     * @param ticksSinceSeen    ticks since the guardian last had line of sight to the player before they hid
     */
    public static double discoverChance(double distance, boolean movedWhileHidden, int ticksSinceSeen) {
        if (movedWhileHidden && distance < 6) return 1.0;
        if (distance > 3.5) return 0.0;
        if (ticksSinceSeen <= WITNESS_WINDOW) return 0.35; // it saw you go in and is checking the lockers
        return 0.03; // just searching nearby
    }

    /** A guardian outside its maze must be pulled back. */
    public static boolean inside(double x, double y, double z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return x >= minX && x <= maxX + 1 && y >= minY && y <= maxY + 1 && z >= minZ && z <= maxZ + 1;
    }

    /** Hunting speed multiplier: fast when it can see you, steady when tracking by sound. */
    public static double huntSpeed(boolean canSee, double distance) {
        if (canSee) return distance < 8 ? 1.25 : 1.1;
        return 0.85;
    }

    /** Music intensity from guardian distance: 0 calm, 1 tense, 2 chase, with soft blend values in between. */
    public static double musicIntensity(double guardianDistance, boolean guardianSeesYou) {
        if (guardianSeesYou) return 2.0;
        if (guardianDistance >= 48) return 0.0;
        if (guardianDistance <= 12) return 1.6 + (12 - guardianDistance) / 12 * 0.4;
        return (48 - guardianDistance) / 36.0 * 1.6;
    }
}
