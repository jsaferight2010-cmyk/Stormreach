package com.stormreach.sim;

/**
 * What a tornado's wind does to a body (a player, a mob, an item, a block of debris), per game tick.
 *
 * <p>Units: velocities in blocks per tick (1 block = 1 m, 20 ticks = 1 s, so m/s × 0.05), accelerations in blocks
 * per tick². Minecraft's gravity is 0.08 blocks/tick² for living entities and 0.04 for falling blocks and items.
 *
 * <p>The model is drag toward the local wind: each tick a body closes a fraction {@code k} of the gap between its
 * velocity and the wind's, where {@code k} shrinks with mass (inertia) and with ground friction. Lift is a separate
 * vertical term that only beats gravity in violent winds: roughly EF3 skids you along, EF4 lifts you, EF5 carries
 * you up to the {@link #ceiling} and throws you out.
 */
public final class VortexForces {
    private VortexForces() {}

    /** Fastest a body is allowed to move, blocks/tick (keeps the server from tunnelling entities through chunks). */
    public static final double MAX_SPEED = 2.5;
    public static final double LIVING_GRAVITY = 0.08;

    /** A mass proxy from the hitbox: 1.0 for a player-sized body, bigger for golems, smaller for items. */
    public static double mass(double bbWidth, double bbHeight, int armor) {
        double vol = bbWidth * bbWidth * bbHeight;
        return clamp(vol / 0.648, 0.15, 8.0) * (1 + armor * 0.02);
    }

    /** Share of the wind/velocity gap closed each tick. */
    public static double dragFactor(double mass, boolean onGround, boolean bracing) {
        double k = 0.07 / Math.sqrt(mass);
        if (onGround) k *= 0.55;      // friction and grip
        if (bracing) k *= 0.6;        // a sneaking player braces against the wind
        return clamp(k, 0.005, 0.35);
    }

    /** Wind speed at which a body of this mass starts to be lifted, mph. */
    public static double liftThresholdMph(double mass) {
        return 100 + 20 * Math.sqrt(mass);
    }

    /** Upward acceleration from the wind (blocks/tick²). It beats living gravity (0.08) from about 170 mph for a player (EF4). */
    public static double lift(double mph, double mass, double heightAboveGround, double ceiling) {
        double thr = liftThresholdMph(mass);
        if (mph <= thr) return 0;
        double a = Math.min(0.2, (mph - thr) / 600.0) / Math.sqrt(Math.max(0.3, mass));
        // fade out under the ceiling, so bodies don't climb forever
        double room = clamp((ceiling - heightAboveGround) / 6.0, 0, 1);
        return a * room;
    }

    /** How high (blocks above the ground) a tornado of this peak wind can carry things before throwing them. */
    public static double ceiling(double peakMph) {
        return clamp(4 + (peakMph - 100) * 0.32, 4, 48);
    }

    /** Damage per second to a living body from the wind alone (sandblasting, impacts with small debris). */
    public static double windDamagePerSecond(double mph) {
        return mph < 110 ? 0 : (mph - 110) / 18.0;
    }

    /**
     * New velocity for a body. {@code vel} and {@code wind} are {x, y, z} in blocks/tick; returns a new array.
     * {@code radialOut} is the unit vector from the tornado's axis to the body (x, z), used to throw it outward.
     */
    public static double[] step(double[] vel, double[] wind, double mph, double mass, boolean onGround, boolean bracing,
                                double heightAboveGround, double ceiling, double[] radialOut) {
        double k = dragFactor(mass, onGround, bracing);
        double vx = vel[0] + (wind[0] - vel[0]) * k;
        double vz = vel[2] + (wind[2] - vel[2]) * k;
        double vy = vel[1] + (wind[1] - vel[1]) * k * 0.5 + lift(mph, mass, heightAboveGround, ceiling);
        // near the ceiling, the outflow throws bodies out of the funnel
        if (heightAboveGround > ceiling * 0.8 && mph > liftThresholdMph(mass)) {
            double fling = 0.06 * Math.min(1, (mph - liftThresholdMph(mass)) / 60.0);
            vx += radialOut[0] * fling;
            vz += radialOut[1] * fling;
        }
        double s = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (s > MAX_SPEED) { vx *= MAX_SPEED / s; vy *= MAX_SPEED / s; vz *= MAX_SPEED / s; }
        return new double[]{vx, vy, vz};
    }

    /**
     * Debris in the vortex: it spirals around the core at {@code orbitRadius}, rising, then is thrown out tangentially.
     * Positions are relative to the tornado axis; {@code tangentialBpt} is the orbit speed in blocks/tick.
     * Returns the velocity {x, y, z} to set, before the tornado's own motion is added.
     */
    public static double[] carry(double dx, double dz, double heightAboveGround, double orbitRadius, double tangentialBpt,
                                 double riseBpt, double ceiling, int rotationSign, double fallingGravity) {
        double r = Math.max(0.01, Math.hypot(dx, dz));
        double ex = dx / r, ez = dz / r;
        double tx = ez * rotationSign, tz = -ex * rotationSign;  // same handedness as WindField
        double radial = clamp((orbitRadius - r) * 0.12, -0.8, 0.8);
        double vy = heightAboveGround < ceiling ? riseBpt + fallingGravity : fallingGravity;
        double vt = Math.min(MAX_SPEED, tangentialBpt);
        return new double[]{tx * vt + ex * radial, vy, tz * vt + ez * radial};
    }

    /** Velocity when debris is thrown out of the funnel: along its orbit, outward, and a little up. */
    public static double[] eject(double dx, double dz, double tangentialBpt, int rotationSign) {
        double r = Math.max(0.01, Math.hypot(dx, dz));
        double ex = dx / r, ez = dz / r;
        double tx = ez * rotationSign, tz = -ex * rotationSign;
        double vt = Math.min(MAX_SPEED, tangentialBpt * 1.1);
        return new double[]{tx * vt + ex * 0.5, 0.25, tz * vt + ez * 0.5};
    }

    /** How long debris rides the vortex before it is thrown, ticks. Stronger tornadoes carry things longer. */
    public static int carryTicks(double peakMph, double randomUnit) {
        return (int) Math.round(clamp(20 + (peakMph - 90) * 0.9, 15, 160) * (0.6 + 0.8 * randomUnit));
    }

    /**
     * How many block checks a tornado gets this tick, from the configured base. A weak tornado uses the base; an EF5
     * at full strength uses up to four times as many, so its damage swath is far more complete.
     */
    public static int blockChecks(int base, double currentPeakMph, double width) {
        double strength = clamp((currentPeakMph - 65) / 45.0, 0, 3);
        double size = clamp(width / 60.0, 0.5, 2.0);
        return (int) Math.round(base * Math.min(4.0, (1 + strength) * Math.sqrt(size)));
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
