package com.stormreach.sim;

/**
 * The Enhanced Fujita scale (NWS/SPC, operational since 2007).
 *
 * <p>EF ratings are <b>damage-based</b>: a surveyor matches observed damage to a damage indicator and
 * degree of damage, and the rating carries an <i>estimated</i> 3-second gust range. The scale is not a
 * wind-speed measurement. In this mod a tornado has an actual simulated wind field; its rating is assigned
 * afterwards from the strongest wind that actually caused block damage (the "damage survey"), and a
 * forecaster-style "estimated" rating is shown while it is active.
 */
public enum EFScale {
    EF0(65, 85), EF1(86, 110), EF2(111, 135), EF3(136, 165), EF4(166, 200), EF5(201, 250);

    /** Lower bound of the estimated 3-second gust, mph (inclusive). */
    public final int minMph;
    /**
     * Upper bound of the estimated 3-second gust, mph (inclusive). EF5 is open-ended ("over 200 mph");
     * 250 mph is used only as a sampling ceiling for simulated tornadoes (near the highest radar-measured
     * tornadic winds, about 300 mph at El Reno in 1999, which were measured aloft, not at the surface).
     */
    public final int maxMph;

    EFScale(int minMph, int maxMph) {
        this.minMph = minMph;
        this.maxMph = maxMph;
    }

    public boolean openEnded() {
        return this == EF5;
    }

    /** Rating whose estimated-gust range contains the given gust. Below 65 mph there is no EF rating (null). */
    public static EFScale fromGustMph(double mph) {
        if (mph < EF0.minMph) return null;
        EFScale result = EF0;
        for (EFScale ef : values()) if (mph >= ef.minMph) result = ef;
        return result;
    }

    public static double mphToMs(double mph) {
        return mph * 0.44704;
    }

    public static double msToMph(double ms) {
        return ms / 0.44704;
    }

    public static double mphToKmh(double mph) {
        return mph * 1.609344;
    }

    public String range() {
        return openEnded() ? "over 200 mph" : minMph + "–" + maxMph + " mph";
    }
}
