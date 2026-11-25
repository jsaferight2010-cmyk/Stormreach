package com.stormreach.sim;

/**
 * Rain that builds up: puddles appear faster in heavier rain and the longer it has been raining, grow while it keeps
 * raining, and dry up after it stops.
 */
public final class Rainfall {
    private Rainfall() {}

    /** Rain intensity: 0 dry, 0.5 rain, 1 thunderstorm downpour. */
    public static double intensity(boolean raining, boolean thundering) {
        return !raining ? 0 : thundering ? 1.0 : 0.5;
    }

    /** How "soaked" the ground is, 0..1, from the intensity and the ticks it has rained without a break. */
    public static double wetness(double intensity, long rainTicks) {
        if (intensity <= 0) return 0;
        // the first two minutes soak in; the ground saturates after about 15 minutes of rain (sooner in a downpour)
        double soak = Math.max(0, rainTicks - 2400) / (18000.0 / (0.5 + intensity));
        return Math.min(1, soak) * intensity;
    }

    /** Puddle attempts per player per tick (a fraction is a chance). */
    public static double puddleAttempts(double wetness) {
        return wetness * 0.6;
    }

    /** Largest puddle size (1..3) the ground can hold at this wetness. */
    public static int maxPuddleSize(double wetness) {
        return wetness > 0.75 ? 3 : wetness > 0.35 ? 2 : 1;
    }

    /** Chance per random tick that a puddle shrinks: never while it rains on it, faster in hot, dry places. */
    public static double evaporation(boolean rainingHere, float temperature) {
        if (rainingHere) return 0;
        return Math.max(0.05, Math.min(0.9, 0.15 + temperature * 0.3));
    }
}
