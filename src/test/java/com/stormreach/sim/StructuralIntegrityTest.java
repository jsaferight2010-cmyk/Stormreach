package com.stormreach.sim;

import com.stormreach.sim.StructuralIntegrity.MaterialClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructuralIntegrityTest {
    private static double t(MaterialClass m, int n, boolean below, boolean exposed, boolean thin) {
        return StructuralIntegrity.failureGustMph(m, n, below, exposed, thin, 12345L);
    }

    @Test
    void materialsAreOrderedByStrength() {
        double prev = 0;
        for (MaterialClass m : MaterialClass.values()) {
            double v = t(m, 2, true, true, false);
            assertTrue(v > prev, m + " should be stronger than the previous class");
            prev = v;
        }
    }

    @Test
    void shelterAndSupportHelp() {
        assertTrue(t(MaterialClass.LIGHT_WOOD, 2, true, false, false) > t(MaterialClass.LIGHT_WOOD, 2, true, true, false));
        assertTrue(t(MaterialClass.LIGHT_WOOD, 2, true, true, false) > t(MaterialClass.LIGHT_WOOD, 2, false, true, false));
        assertTrue(t(MaterialClass.STONE, 6, true, true, false) > t(MaterialClass.STONE, 0, true, true, false));
        assertTrue(t(MaterialClass.GLASS, 2, true, true, true) < t(MaterialClass.GLASS, 2, true, true, false));
    }

    @Test
    void damageLevelsLineUpWithTheEfScale() {
        // foliage goes in EF0 winds, wood framing by EF2, stone needs at least EF3/EF4
        assertTrue(t(MaterialClass.FOLIAGE, 1, true, true, false) < EFScale.EF0.maxMph);
        assertTrue(t(MaterialClass.LIGHT_WOOD, 2, true, true, false) < EFScale.EF2.maxMph);
        assertTrue(t(MaterialClass.STONE, 2, true, true, false) > EFScale.EF3.minMph);
        assertFalse(StructuralIntegrity.fails(1000, t(MaterialClass.UNBREAKABLE, 0, false, true, true)));
    }

    @Test
    void varianceIsBoundedAndDeterministic() {
        for (long h = 0; h < 2000; h++) {
            double v = StructuralIntegrity.failureGustMph(MaterialClass.MASONRY, 0, true, true, false, h);
            assertTrue(v >= 150 * 1.08 * 0.92 - 1e-6 && v <= 150 * 1.08 * 1.08 + 1e-6);
            assertEquals(v, StructuralIntegrity.failureGustMph(MaterialClass.MASONRY, 0, true, true, false, h), 0);
        }
    }

    @Test
    void debrisBudgetNeverExceedsItsLimits() {
        DebrisBudget b = new DebrisBudget(40, 64);
        for (int tick = 0; tick < 200; tick++) {
            b.newTick();
            int checks = 0, launched = 0;
            for (int i = 0; i < 500; i++) { if (b.tryCheck()) checks++; if (b.tryLaunch()) launched++; }
            assertTrue(checks <= 64);
            assertTrue(b.airborne() <= 40);
            if (tick % 3 == 0) for (int i = 0; i < 10; i++) b.landed();
        }
        assertTrue(b.airborne() >= 0);
    }
}
