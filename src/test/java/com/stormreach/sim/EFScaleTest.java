package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EFScaleTest {
    @Test
    void rangesMatchTheOfficialScale() {
        int[][] official = {{65, 85}, {86, 110}, {111, 135}, {136, 165}, {166, 200}};
        for (int i = 0; i < official.length; i++) {
            assertEquals(official[i][0], EFScale.values()[i].minMph, "min " + i);
            assertEquals(official[i][1], EFScale.values()[i].maxMph, "max " + i);
        }
        assertEquals(201, EFScale.EF5.minMph);
        assertTrue(EFScale.EF5.openEnded());
    }

    @Test
    void boundariesClassifyCorrectly() {
        assertNull(EFScale.fromGustMph(64.9));
        assertEquals(EFScale.EF0, EFScale.fromGustMph(65));
        assertEquals(EFScale.EF0, EFScale.fromGustMph(85));
        assertEquals(EFScale.EF1, EFScale.fromGustMph(86));
        assertEquals(EFScale.EF2, EFScale.fromGustMph(135));
        assertEquals(EFScale.EF3, EFScale.fromGustMph(136));
        assertEquals(EFScale.EF4, EFScale.fromGustMph(200));
        assertEquals(EFScale.EF5, EFScale.fromGustMph(201));
        assertEquals(EFScale.EF5, EFScale.fromGustMph(320));
    }

    @Test
    void rangesAreContiguousAndIncreasing() {
        for (int i = 1; i < EFScale.values().length; i++) {
            assertEquals(EFScale.values()[i - 1].maxMph + 1, EFScale.values()[i].minMph);
        }
    }

    @Test
    void unitConversions() {
        assertEquals(44.704, EFScale.mphToMs(100), 1e-9);
        assertEquals(100, EFScale.msToMph(44.704), 1e-9);
    }
}
