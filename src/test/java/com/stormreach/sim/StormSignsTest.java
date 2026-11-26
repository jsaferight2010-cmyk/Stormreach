package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 6.0 stage 10: the Weatherglass, freezing rain and St. Elmo's fire rules. */
class StormSignsTest {
    @Test
    void glassRisesAsRainNears() {
        assertEquals(0, StormSigns.glassLevel(false, false, 100_000, 50_000, 0), "a storm far off does not show");
        int far = StormSigns.glassLevel(false, false, 20_000, 50_000, 0);
        int near = StormSigns.glassLevel(false, false, 1_000, 50_000, 0);
        assertTrue(far >= 1 && near > far && near <= 9, "far " + far + " near " + near);
        assertEquals(0, StormSigns.glassLevel(false, false, 1_000, 50_000, 5_000), "forced clear weather shows nothing coming");
    }

    @Test
    void glassShowsRainAndThunder() {
        assertEquals(10, StormSigns.glassLevel(true, false, 8_000, 40_000, 0));
        int close = StormSigns.glassLevel(true, false, 8_000, 500, 0);
        assertTrue(close > 10 && close < 15, "thunder close while raining: " + close);
        assertEquals(15, StormSigns.glassLevel(true, true, 8_000, 3_000, 0));
        assertEquals(10, StormSigns.glassLevel(true, false, 8_000, 0, 0));
        assertEquals(3, StormSigns.glassCrystals(15));
        assertEquals(0, StormSigns.glassCrystals(0));
    }

    @Test
    void freezingRainOnlyJustAboveFreezing() {
        assertTrue(StormSigns.freezingRain(true, 0.25f, true, false), "a taiga night");
        assertTrue(StormSigns.freezingRain(true, 0.2f, false, true), "a Thunderhead by day on a stony shore");
        assertFalse(StormSigns.freezingRain(true, 0.1f, true, true), "it snows there instead");
        assertFalse(StormSigns.freezingRain(true, 0.8f, true, true), "too warm");
        assertFalse(StormSigns.freezingRain(true, 0.25f, false, false), "a plain rainy day");
        assertFalse(StormSigns.freezingRain(false, 0.25f, true, true), "no rain");
        assertTrue(StormSigns.glazeMelts(false, true, 0.25f, 0));
        assertFalse(StormSigns.glazeMelts(false, false, 0.25f, 0), "it stays through the night");
        assertTrue(StormSigns.glazeMelts(true, false, 0.25f, 14), "a torch melts it");
    }

    @Test
    void elmoBuildsOnlyWhenExposedWithMetalUpHigh() {
        int c = 0;
        for (int s = 0; s < 10; s++) c = StormSigns.elmoStep(c, true, true, true, 120, false);
        assertEquals(200, c);
        assertTrue(c >= StormSigns.ELMO_WARN);
        assertEquals(140, StormSigns.elmoStep(c, true, true, false, 120, false), "putting the metal away bleeds it off");
        assertEquals(140, StormSigns.elmoStep(c, true, true, true, 120, true), "a lightning rod takes it");
        assertEquals(140, StormSigns.elmoStep(c, true, true, true, 70, false), "low ground is safe");
        assertEquals(0, StormSigns.elmoStep(10, false, true, true, 120, false));
        int full = StormSigns.ELMO_STRIKE;
        assertEquals(full, StormSigns.elmoStep(full, true, true, true, 120, false), "capped");
    }
}
