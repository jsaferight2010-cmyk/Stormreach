package com.stormreach.sim;

/**
 * Tornado lifecycle (after the classic stages described by Golden &amp; Purcell): organizing, mature,
 * shrinking and decaying ("roping out"). Multipliers are interpolated smoothly between keyframes.
 */
public enum TornadoStage {
    ORGANIZING, MATURE, SHRINKING, DECAYING;

    // lifetime fraction, intensity multiplier, width multiplier
    private static final double[][] KEYS = {
            {0.00, 0.00, 0.30},
            {0.15, 0.55, 0.60},
            {0.25, 1.00, 1.00},
            {0.70, 1.00, 1.00},
            {0.85, 0.70, 0.55},
            {1.00, 0.00, 0.20}};

    public static TornadoStage at(double f) {
        if (f < 0.20) return ORGANIZING;
        if (f < 0.70) return MATURE;
        if (f < 0.85) return SHRINKING;
        return DECAYING;
    }

    public static double intensityAt(double f) {
        return interp(f, 1);
    }

    public static double widthAt(double f) {
        return interp(f, 2);
    }

    private static double interp(double f, int col) {
        f = Math.max(0, Math.min(1, f));
        for (int i = 1; i < KEYS.length; i++) {
            if (f <= KEYS[i][0]) {
                double k = (f - KEYS[i - 1][0]) / (KEYS[i][0] - KEYS[i - 1][0]);
                return KEYS[i - 1][col] + (KEYS[i][col] - KEYS[i - 1][col]) * k;
            }
        }
        return KEYS[KEYS.length - 1][col];
    }
}
