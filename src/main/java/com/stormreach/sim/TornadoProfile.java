package com.stormreach.sim;

import java.util.Random;

/**
 * Everything that describes one tornado when it forms. Values are sampled from approximate observed
 * distributions (US tornado climatology); see {@link #roll}.
 */
public record TornadoProfile(TornadoFormation formation, EFScale potentialRating, double peakGustMph,
                             TornadoMorphology morphology, VortexStructure structure, int subvortices,
                             double width, double translationMs, double headingDeg, int rotationSign,
                             int lifetimeTicks, TornadoRelation relation) {

    /**
     * Approximate share of rated US tornadoes by EF rating (rounded from SPC/NWS records since 2007):
     * EF0 about 55%, EF1 about 32%, EF2 about 9.5%, EF3 about 2.6%, EF4 about 0.8%, EF5 about 0.1%.
     */
    public static final double[] SUPERCELL_CLIMATOLOGY = {0.550, 0.320, 0.095, 0.026, 0.008, 0.001};
    public static final double[] LANDSPOUT_CLIMATOLOGY = {0.80, 0.18, 0.02, 0, 0, 0};
    public static final double[] WATERSPOUT_CLIMATOLOGY = {0.90, 0.10, 0, 0, 0, 0};

    /** Typical lifetime ranges in minutes by rating (most tornadoes last under 10 minutes). */
    static final double[][] LIFETIME_MIN = {{1, 5}, {2, 8}, {4, 15}, {8, 25}, {15, 45}, {20, 60}};

    static final double[] MULTIVORTEX_CHANCE = {0.02, 0.05, 0.15, 0.30, 0.50, 0.60};

    /** Morphology weights per rating, order: ROPE, NEEDLE, DRILL_BIT, CONE, STOVEPIPE, WEDGE, LARGE_WEDGE. */
    static final int[][] MORPH_WEIGHTS = {
            {30, 15, 15, 35, 5, 0, 0},
            {20, 8, 15, 45, 12, 0, 0},
            {10, 0, 8, 45, 25, 12, 0},
            {5, 0, 0, 35, 30, 28, 2},
            {0, 0, 0, 20, 25, 45, 10},
            {0, 0, 0, 10, 25, 50, 15}};
    static final int[] LANDSPOUT_MORPH = {60, 25, 15, 0, 0, 0, 0};
    static final int[] WATERSPOUT_MORPH = {50, 30, 0, 20, 0, 0, 0};

    /**
     * @param intensityBias 1.0 = climatology; higher values shift the distribution toward stronger
     *                      tornadoes (each step multiplies a rating's weight by bias^index).
     * @param sizeScale     gameplay scale applied to widths (1.0 = real size, 1 block = 1 m)
     * @param durationScale gameplay scale applied to lifetimes
     * @param baseHeadingDeg heading of the steering wind (degrees, 0 = +x/east, 90 = +z/south)
     */
    public static TornadoProfile roll(Random random, TornadoFormation formation, double intensityBias,
                                      double sizeScale, double durationScale, double baseHeadingDeg) {
        double[] weights = switch (formation) {
            case SUPERCELL -> SUPERCELL_CLIMATOLOGY.clone();
            case LANDSPOUT -> LANDSPOUT_CLIMATOLOGY.clone();
            case WATERSPOUT -> WATERSPOUT_CLIMATOLOGY.clone();
        };
        for (int i = 0; i < weights.length; i++) weights[i] *= Math.pow(Math.max(0.01, intensityBias), i);
        EFScale rating = EFScale.values()[pick(random, weights)];
        if (rating.ordinal() > formation.maxRating.ordinal()) rating = formation.maxRating;
        return rollWith(random, formation, rating, null, sizeScale, durationScale, baseHeadingDeg);
    }

    /** Builds a profile with a chosen rating and (optionally) a forced morphology. */
    public static TornadoProfile rollWith(Random random, TornadoFormation formation, EFScale rating,
                                          TornadoMorphology forcedMorph, double sizeScale, double durationScale,
                                          double baseHeadingDeg) {
        int ef = rating.ordinal();
        // peak estimated gust: anywhere in the rating's range, skewed toward its lower end (stronger winds are rarer)
        double span = (rating.openEnded() ? 235 : rating.maxMph) - rating.minMph;
        double gust = rating.minMph + span * Math.pow(random.nextDouble(), 1.4);

        int[] mw = switch (formation) {
            case SUPERCELL -> MORPH_WEIGHTS[ef];
            case LANDSPOUT -> LANDSPOUT_MORPH;
            case WATERSPOUT -> WATERSPOUT_MORPH;
        };
        TornadoMorphology morph = forcedMorph != null ? forcedMorph : TornadoMorphology.values()[pick(random, toDouble(mw))];

        // width: within the morphology's range, larger for stronger ratings
        double k = Math.min(1, (ef + random.nextDouble()) / 6.0);
        double width = (morph.minWidth + (morph.maxWidth - morph.minWidth) * (0.3 * random.nextDouble() + 0.7 * k)) * sizeScale;
        width = Math.max(3, width);

        VortexStructure structure = VortexStructure.SINGLE;
        int subs = 0;
        if (formation == TornadoFormation.SUPERCELL) {
            double mv = MULTIVORTEX_CHANCE[ef] * ((morph == TornadoMorphology.WEDGE || morph == TornadoMorphology.LARGE_WEDGE) ? 1.5 : 1.0);
            if (morph == TornadoMorphology.ROPE || morph == TornadoMorphology.NEEDLE) mv = 0;
            if (random.nextDouble() < mv) {
                structure = VortexStructure.MULTIPLE_VORTEX;
                subs = 2 + random.nextInt(5);
            }
        }

        // translation speed: supercell tornadoes average ~13 m/s (30 mph), most between 5 and 25 m/s;
        // non-supercell vortices drift slowly with the low-level wind
        double trans = formation == TornadoFormation.SUPERCELL
                ? clamp(13 + random.nextGaussian() * 5.5 + ef * 0.8, 0.5, 31)
                : clamp(3 + random.nextGaussian() * 2, 0, 9);
        double heading = baseHeadingDeg + random.nextGaussian() * 20;
        int rotation = formation == TornadoFormation.SUPERCELL ? (random.nextDouble() < 0.98 ? 1 : -1) : (random.nextDouble() < 0.7 ? 1 : -1);

        double[] life = LIFETIME_MIN[ef];
        double minutes = life[0] + (life[1] - life[0]) * Math.pow(random.nextDouble(), 1.6);
        int ticks = (int) Math.max(400, minutes * 60 * 20 * durationScale);

        return new TornadoProfile(formation, rating, gust, morph, structure, subs, width, trans, heading, rotation, ticks, TornadoRelation.NONE);
    }

    public TornadoProfile withRelation(TornadoRelation r) {
        return new TornadoProfile(formation, potentialRating, peakGustMph, morphology, structure, subvortices, width,
                translationMs, headingDeg, rotationSign, lifetimeTicks, r);
    }

    /** Core (radius of maximum wind) is taken as half the funnel/damage width. */
    public double coreRadius() {
        return width / 2.0;
    }

    public WindField windField(double lifeFraction) {
        double i = TornadoStage.intensityAt(lifeFraction);
        double rad = Math.toRadians(headingDeg);
        double gustMs = EFScale.mphToMs(peakGustMph) * i;
        return WindField.forPeakGust(gustMs, coreRadius() * TornadoStage.widthAt(lifeFraction),
                Math.cos(rad) * translationMs, Math.sin(rad) * translationMs, rotationSign,
                structure == VortexStructure.MULTIPLE_VORTEX ? subvortices : 0);
    }

    static int pick(Random random, double[] weights) {
        double sum = 0;
        for (double w : weights) sum += w;
        double x = random.nextDouble() * sum;
        for (int i = 0; i < weights.length; i++) {
            x -= weights[i];
            if (x < 0) return i;
        }
        for (int i = weights.length - 1; i >= 0; i--) if (weights[i] > 0) return i;
        return 0;
    }

    private static double[] toDouble(int[] a) {
        double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) d[i] = a[i];
        return d;
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
