package com.stormreach.sim;

/**
 * 6.0 stage 10, pure rules for the "signs of the storm": the Weatherglass forecast, freezing rain and St. Elmo's fire.
 * No Minecraft types, so they are unit-tested.
 */
public final class StormSigns {
    private StormSigns() {}

    /** How far ahead (ticks) the Weatherglass can feel weather: 20 in-game minutes. */
    public static final int GLASS_HORIZON = 24_000;
    /** "Soon": 5 minutes. */
    public static final int GLASS_SOON = 6_000;

    /**
     * The Weatherglass reading, 0-15, from the vanilla weather timers. 0 = settled weather; 1-9 = rain is on its way
     * (rising as it nears); 10-14 = raining (higher if thunder is close); 15 = thunderstorm. While {@code clearTime}
     * is set (forced clear weather), nothing is coming yet.
     */
    public static int glassLevel(boolean raining, boolean thundering, int rainTime, int thunderTime, int clearTime) {
        if (raining && thundering) return 15;
        boolean thunderSoon = !thundering && thunderTime > 0 && thunderTime <= GLASS_SOON;
        if (raining) {
            if (!thunderSoon) return 10;
            return 10 + Math.max(1, Math.min(4, (int) Math.ceil(4.0 * (GLASS_SOON - thunderTime) / GLASS_SOON)));
        }
        if (clearTime > 0 || rainTime <= 0 || rainTime > GLASS_HORIZON) return 0;
        return Math.max(1, Math.min(9, (int) Math.ceil(9.0 * (GLASS_HORIZON - rainTime) / GLASS_HORIZON)));
    }

    /** The look of the glass for a reading: 0 clear, 1 a few feathers, 2 cloudy crystals, 3 a full storm of crystals. */
    public static int glassCrystals(int level) {
        return level >= 15 ? 3 : level >= 5 ? 2 : level >= 1 ? 1 : 0;
    }

    /**
     * Freezing rain: rain (not snow) falling where the biome is only just above freezing, at night or in a
     * Thunderhead. {@code temperature} is the biome's temperature at that spot (vanilla snows below 0.15).
     */
    public static boolean freezingRain(boolean rainingHere, float temperature, boolean night, boolean thundering) {
        return rainingHere && temperature >= 0.15f && temperature < 0.35f && (night || thundering);
    }

    /** Glaze melts in daylight once the freezing rain stops, or at once in a warm spot. */
    public static boolean glazeMelts(boolean freezingRainHere, boolean day, float temperature, int blockLight) {
        if (temperature >= 0.35f || blockLight > 11) return true;
        return !freezingRainHere && day;
    }

    /** Ticks of St. Elmo's charge before the warning, and before the strike. */
    public static final int ELMO_WARN = 200, ELMO_STRIKE = 600;

    /**
     * One second (20 ticks) of St. Elmo's fire. A player out in a Thunderhead holding metal high up builds charge;
     * going under cover, putting the metal away or standing near a lightning rod bleeds it off.
     *
     * @return the new charge in ticks (0 to ELMO_STRIKE)
     */
    public static int elmoStep(int charge, boolean thundering, boolean exposed, boolean holdingMetal, int y, boolean rodNearby) {
        boolean builds = thundering && exposed && holdingMetal && y >= 90 && !rodNearby;
        int next = builds ? charge + 20 : charge - 60;
        return Math.max(0, Math.min(ELMO_STRIKE, next));
    }
}
