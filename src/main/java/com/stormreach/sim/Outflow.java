package com.stormreach.sim;

/**
 * A downburst's outflow: cold air hits the ground and spreads out as a ring (the gust front) at about 15 m/s.
 * Winds are strongest just behind the front, and the whole event peaks a few seconds after impact, then weakens.
 * A wet downburst comes with a shaft of heavy rain; a dry one falls from virga (rain that evaporates before
 * reaching the ground) and shows only as a ring of dust.
 */
public final class Outflow {
    private Outflow() {}

    /** Radius of the gust front after {@code ticks}, blocks. */
    public static double frontRadius(int ticks, double maxRadius) {
        return Math.min(maxRadius, 2 + ticks * 0.75);
    }

    /** Outward wind, mph, at distance r from the impact point. */
    public static double windMph(double r, int ticks, int duration, double peakMph, double maxRadius) {
        double front = frontRadius(ticks, maxRadius);
        if (r > front + 3) return 0;                              // the air ahead of the front is still calm
        double profile = r < front * 0.35 ? r / (front * 0.35) * 0.6 : 0.6 + 0.4 * Math.min(1, (r - front * 0.35) / (front * 0.55));
        if (r > front) profile *= Math.max(0, 1 - (r - front) / 3);
        double life = ticks < 20 ? ticks / 20.0 : ticks > duration - 40 ? Math.max(0, (duration - ticks) / 40.0) : 1;
        double spread = Math.max(0.35, 1 - front / (maxRadius * 2.2)); // weakens as it spreads out
        return peakMph * profile * life * spread;
    }
}
