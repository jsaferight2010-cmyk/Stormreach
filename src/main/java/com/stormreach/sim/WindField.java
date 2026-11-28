package com.stormreach.sim;

/**
 * Tornado wind field: a modified Rankine vortex with near-surface inflow, an updraft annulus,
 * optional orbiting subvortices, and the tornado's translation added to give ground-relative winds.
 *
 * <p>Units: metres, seconds, m/s. Coordinates are relative to the tornado centre; +x east, +z south
 * (Minecraft), so a cyclonic (counter-clockwise seen from above, Northern Hemisphere) vortex rotates
 * with {@code rotationSign = +1}.
 */
public final class WindField {
    /** Air density near the surface, kg/m^3. */
    public static final double RHO = 1.2;
    /** Exponent of the decay of tangential wind outside the core (observed roughly 0.5–0.7). */
    public static final double ALPHA = 0.6;

    public final double coreRadius;      // radius of maximum rotational wind, m
    public final double vMax;            // peak rotational (tangential) wind of the parent vortex, m/s
    public final double translateX;      // tornado motion, m/s
    public final double translateZ;
    public final int rotationSign;       // +1 cyclonic, -1 anticyclonic
    public final int subvortices;        // 0 for single-vortex
    public final double subvortexFraction; // subvortex peak wind as a fraction of vMax

    public WindField(double coreRadius, double vMax, double translateX, double translateZ, int rotationSign,
                     int subvortices, double subvortexFraction) {
        this.coreRadius = Math.max(1.0, coreRadius);
        this.vMax = Math.max(0, vMax);
        this.translateX = translateX;
        this.translateZ = translateZ;
        this.rotationSign = rotationSign >= 0 ? 1 : -1;
        this.subvortices = Math.max(0, subvortices);
        this.subvortexFraction = subvortexFraction;
    }

    private static final double[] RATIO_CACHE = new double[16];

    /**
     * Ratio between the peak horizontal wind (rotation + inflow + subvortices, no translation) and the parent
     * vMax. The field is scale-invariant in radius, so this depends only on the number of subvortices.
     */
    static synchronized double rotationRatio(int subvortices) {
        int k = Math.min(15, Math.max(0, subvortices));
        if (RATIO_CACHE[k] == 0) {
            WindField probe = new WindField(50, 10, 0, 0, 1, k, k > 0 ? 0.3 : 0.0);
            RATIO_CACHE[k] = probe.scanPeakGust(72) / 10.0;
        }
        return RATIO_CACHE[k];
    }

    /** Builds a field whose peak <i>ground-relative</i> gust equals {@code peakGustMs}. */
    public static WindField forPeakGust(double peakGustMs, double coreRadius, double translateX, double translateZ,
                                        int rotationSign, int subvortices) {
        double t = Math.hypot(translateX, translateZ);
        double rot = Math.max(5, peakGustMs - t);
        double parent = rot / rotationRatio(subvortices);
        return new WindField(coreRadius, parent, translateX, translateZ, rotationSign, subvortices, subvortices > 0 ? 0.3 : 0.0);
    }

    /** Tangential wind of a Rankine-type vortex at radius r. */
    public static double tangential(double vMax, double rc, double r) {
        if (r <= rc) return vMax * (r / rc);
        return vMax * Math.pow(rc / r, ALPHA);
    }

    /** Height factor: winds peak in the lowest tens of metres (the "corner flow"), weaker right at the surface. */
    public static double heightFactor(double z) {
        if (z < 0) z = 0;
        if (z < 10) return 0.8 + 0.02 * z;
        if (z < 120) return 1.0;
        return Math.max(0.6, 1.0 - (z - 120) / 400.0);
    }

    public record Sample(double tangential, double radial, double vertical, double u, double w, double horizontal,
                         double pressureDeficitPa) {
        public double horizontalMph() {
            return EFScale.msToMph(horizontal);
        }
    }

    /**
     * Ground-relative wind at offset (dx, dz) from the centre and height z above ground, at time t seconds
     * (t moves the subvortices around their orbit).
     */
    public Sample sample(double dx, double dz, double z, double t) {
        double r = Math.max(0.01, Math.hypot(dx, dz));
        double hf = heightFactor(z);
        double vt = tangential(vMax, coreRadius, r) * hf;
        // unit vectors: radial outward and tangential (counter-clockwise seen from above with +z south means
        // the tangential direction for cyclonic flow is (dz, -dx)/r in x/z coordinates)
        double ex = dx / r, ez = dz / r;
        double tx = ez * rotationSign, tz = -ex * rotationSign;
        // near-surface inflow outside the core, fading with height
        double inflowShare = r > coreRadius * 0.5 ? 0.45 * Math.max(0, 1.0 - z / 100.0) : 0.1;
        double vr = -inflowShare * tangential(vMax, coreRadius, r);
        // updraft annulus just inside the radius of maximum wind
        double q = r / coreRadius - 0.8;
        double w = 0.6 * vMax * Math.exp(-q * q * 4) * Math.min(1, z / 30.0 + 0.2);
        double u = vt * tx + vr * ex;
        double v = vt * tz + vr * ez;
        // subvortices orbit at ~0.75 core radius
        if (subvortices > 0) {
            double orbit = 0.75 * coreRadius;
            double rs = Math.max(2.0, 0.15 * coreRadius);
            double omega = rotationSign * (vMax * 0.7) / orbit; // orbit with part of the parent flow
            for (int i = 0; i < subvortices; i++) {
                double a = omega * t + i * 2 * Math.PI / subvortices;
                double sx = dx - orbit * Math.cos(a), sz = dz + orbit * Math.sin(a);
                double sr = Math.max(0.01, Math.hypot(sx, sz));
                double sv = tangential(vMax * subvortexFraction, rs, sr) * hf;
                u += sv * (sz / sr) * rotationSign;
                v += sv * (-sx / sr) * rotationSign;
            }
        }
        u += translateX;
        v += translateZ;
        return new Sample(vt, vr, w, u, v, Math.hypot(u, v), pressureDeficit(r));
    }

    /** Pressure deficit from cyclostrophic balance (Rankine vortex), Pa. Centre value = rho * vMax^2. */
    public double pressureDeficit(double r) {
        double v2 = vMax * vMax;
        if (r <= coreRadius) return RHO * v2 * (1.0 - (r * r) / (2 * coreRadius * coreRadius));
        double k = coreRadius / r;
        return RHO * v2 * 0.5 * k * k;
    }

    /** Largest ground-relative horizontal gust found by scanning the field near the surface. */
    public double scanPeakGust(int steps) {
        double best = 0;
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            for (double rf = 0.5; rf <= 1.3; rf += 0.05) {
                for (double t = 0; t < 6; t += 0.5) {
                    Sample s = sample(Math.cos(a) * coreRadius * rf, Math.sin(a) * coreRadius * rf, 15, t);
                    best = Math.max(best, s.horizontal());
                }
            }
        }
        return best;
    }
}
