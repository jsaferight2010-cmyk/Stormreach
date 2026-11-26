package com.stormreach.sim;

/**
 * 6.0: how a Gale Cloud throws you, from the way you met it. The gust carries you on through the cloud in the
 * direction you were moving (or, standing still, away from the side you came in at), and always with some lift:
 * <ul>
 *   <li>entering a side: flung onward across it, with a lift;</li>
 *   <li>falling in from above: bounced back up, carried a little the way you face;</li>
 *   <li>jumping or flying up into it from below: thrown higher, keeping your sideways motion.</li>
 * </ul>
 * Inputs are the entity's velocity (blocks/tick), its centre relative to the cloud's centre, and its facing.
 * Returns the new velocity. Pure so it is unit-tested.
 */
public final class GaleLaunch {
    private GaleLaunch() {}

    public static final double ONWARD = 1.15, LIFT = 0.45, BOUNCE = 0.95, UPDRAFT = 0.9;

    public static double[] vector(double vx, double vy, double vz, double offX, double offY, double offZ, double lookX, double lookZ) {
        double hx = vx, hz = vz, h = Math.hypot(hx, hz);
        boolean fromAbove = vy < -0.2 && h < Math.abs(vy) * 1.5 || h < 0.03 && offY > 0.25;
        boolean fromBelow = vy > 0.1 && h < Math.abs(vy) * 1.5 || h < 0.03 && offY < -0.25;
        if (h < 0.03) {
            // barely moving sideways: push out through the far side from where the entity is in the cloud
            hx = -offX; hz = -offZ; h = Math.hypot(hx, hz);
            if (h < 0.05 || fromAbove || fromBelow) { hx = lookX; hz = lookZ; h = Math.hypot(hx, hz); }
        }
        double ux = h < 1e-6 ? 0 : hx / h, uz = h < 1e-6 ? 0 : hz / h;
        if (fromAbove) return new double[]{ux * ONWARD * 0.35, BOUNCE, uz * ONWARD * 0.35};
        if (fromBelow) return new double[]{vx + ux * 0.25, Math.max(vy, 0) + UPDRAFT, vz + uz * 0.25};
        return new double[]{ux * ONWARD, LIFT, uz * ONWARD};
    }
}
