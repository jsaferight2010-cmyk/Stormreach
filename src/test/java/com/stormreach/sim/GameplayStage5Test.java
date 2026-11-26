package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 6.0 stage 5: the Wind Generator's field, the overcharged Stormheart's frontier and unit-free readings. */
class GameplayStage5Test {
    @Test
    void fanBlowsForwardNotBackward() {
        // facing +X: in front it blows, behind it doesn't
        assertTrue(FanField.strength(4, 0, 0, 1, 0, 24) > 0.6, "4 blocks in front of a fan should feel most of it");
        assertEquals(0, FanField.strength(-4, 0, 0, 1, 0, 24), 1e-9, "behind a fan there is no wind");
        assertEquals(0, FanField.strength(30, 0, 0, 1, 0, 24), 1e-9, "past the reach there is no wind");
    }

    @Test
    void fanWeakensWithDistanceAndOffAxis() {
        double near = FanField.strength(2, 0, 0, 0, 1, 32) , far;
        near = FanField.strength(0, 0, 2, 0, 1, 32);
        far = FanField.strength(0, 0, 24, 0, 1, 32);
        assertTrue(near > far, "wind should fade with distance from the fan");
        double axis = FanField.strength(0, 0, 10, 0, 1, 32), edge = FanField.strength(4, 0, 10, 0, 1, 32);
        assertTrue(axis > edge, "wind should be strongest on the fan's axis");
        assertTrue(edge > 0, "the cone should be about 5 blocks wide 10 blocks out");
        assertEquals(0, FanField.strength(8, 0, 10, 0, 1, 32), 1e-9, "far off the axis there is no wind");
    }

    @Test
    void fanConeWidensDownwind() {
        assertEquals(0, FanField.strength(1, 3, 1, 1, 0, 40), 1e-9, "3 blocks up right at the fan is outside the cone");
        assertTrue(FanField.strength(20, 3, 0, 1, 0, 40) > 0, "3 blocks up 20 blocks out is inside the cone");
    }

    @Test
    void frontierGrowsSlowerAsItSpreads() {
        assertTrue(HeartFrontier.samplesPerStep(200) > HeartFrontier.samplesPerStep(40), "a bigger ring takes more sampling to advance");
        assertTrue(HeartFrontier.columnsPerCycle(10) > HeartFrontier.columnsPerCycle(1), "spread speed should sample more columns");
        // seconds for the frontier to go from 32 to 64 at speed 5
        double samples = 0;
        for (int r = 32; r < 64; r++) samples += HeartFrontier.samplesPerStep(r);
        double seconds = samples / HeartFrontier.columnsPerCycle(5);
        assertTrue(seconds > 30 && seconds < 600, "doubling a 32-block blight should take minutes, not ticks or hours: " + seconds);
    }

    @Test
    void frontierRespectsItsCap() {
        assertTrue(HeartFrontier.mayGrow(5000, 0), "0 means no cap");
        assertFalse(HeartFrontier.mayGrow(100, 100), "the cap stops it");
        assertTrue(HeartFrontier.mayGrow(99, 100));
        assertTrue(HeartFrontier.sampleRadius(40, 1.0) >= 40 - HeartFrontier.BAND && HeartFrontier.sampleRadius(40, 0) == 40);
    }

    @Test
    void readingsCarryTheirOwnUnits() {
        assertEquals("100 mph", Units.speed(100, Units.System.IMPERIAL));
        assertEquals("161 km/h", Units.speed(100, Units.System.METRIC));
        assertEquals("1013 hPa", Units.pressure(1013.25, Units.System.METRIC));
        assertEquals("29.92 inHg", Units.pressure(1013.25, Units.System.IMPERIAL));
    }
}
