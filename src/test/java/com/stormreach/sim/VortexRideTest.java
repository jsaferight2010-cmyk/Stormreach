package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 6.0 (test list 11): entities caught in a tornado circle, rise, bob and stay near their orbit. */
class VortexRideTest {
    @Test
    void onlyStrongWindsNearTheCoreCaptureABody() {
        assertFalse(VortexForces.captured(90, 1.0, 5, 20), "EF1 winds should not lift a player");
        assertTrue(VortexForces.captured(160, 1.0, 10, 20), "EF3 winds at the core should lift a player");
        assertFalse(VortexForces.captured(160, 1.0, 80, 20), "four core radii out is too far to be lifted");
        assertTrue(VortexForces.captured(125, 1.0, 5, 20), "125 mph lifts a player");
        assertFalse(VortexForces.captured(125, 8.0, 5, 20), "a heavy body needs stronger winds");
    }

    @Test
    void ridersCircleClimbAndBob() {
        double x = 12, z = 0, y = 0, orbit = 12;
        double minY = Double.MAX_VALUE, maxDy = -1, minDy = 1;
        double angle0 = Math.atan2(z, x);
        for (int t = 0; t < 120; t++) {
            double[] v = VortexForces.ride(x, z, y, orbit, 0.6, 0.12, 30, 1, 0.3, t);
            x += v[0]; z += v[2]; y += v[1];
            maxDy = Math.max(maxDy, v[1]); minDy = Math.min(minDy, v[1]);
            if (t > 20) minY = Math.min(minY, y);
            double r = Math.hypot(x, z);
            assertTrue(r > orbit * 0.4 && r < orbit * 1.8, "the rider left its orbit: r=" + r);
        }
        assertTrue(y > 4, "riders should rise overall: y=" + y);
        assertTrue(minDy < 0 && maxDy > 0, "riders should bob (some downward motion too)");
        double travelled = Math.atan2(z, x) - angle0;
        assertTrue(Math.abs(travelled) > 1e-3, "riders should circle the axis");
    }

    @Test
    void ridersLevelOffUnderTheCeiling() {
        double[] v = VortexForces.ride(10, 0, 31, 10, 0.6, 0.2, 30, 1, 0, 5);
        assertTrue(v[1] <= 0, "above the ceiling a rider must not keep climbing");
    }
}
