package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 6.0 stage 6: the Gale Cloud throws you according to how you met it, never always the same way. */
class MobFixesStage6Test {
    private static double[] gust(double vx, double vy, double vz, double ox, double oy, double oz) {
        return GaleLaunch.vector(vx, vy, vz, ox, oy, oz, 0, 1);
    }

    @Test
    void sideEntriesCarryOnInTheDirectionOfTravel() {
        double[][] dirs = {{0.3, 0}, {-0.3, 0}, {0, 0.3}, {0, -0.3}, {0.2, 0.2}};
        for (double[] d : dirs) {
            double[] v = gust(d[0], 0, d[1], -d[0], 0, -d[1]);
            double dot = v[0] * d[0] + v[2] * d[1];
            assertTrue(dot > 0, "walking in along " + d[0] + "," + d[1] + " should be flung onward, got " + v[0] + "," + v[2]);
            assertTrue(v[1] > 0.2, "a side entry gets some lift");
        }
        double[] east = gust(0.3, 0, 0, -0.4, 0, 0), west = gust(-0.3, 0, 0, 0.4, 0, 0);
        assertTrue(east[0] > 0 && west[0] < 0, "opposite entries go opposite ways (the old bug threw both the same way)");
    }

    @Test
    void fromAboveBouncesUpAndFromBelowThrowsHigher() {
        double[] above = gust(0, -0.8, 0, 0, 0.4, 0);
        assertTrue(above[1] >= GaleLaunch.BOUNCE * 0.99, "falling in from above should bounce up");
        double[] below = gust(0.1, 0.5, 0, 0, -0.4, 0);
        assertTrue(below[1] > 0.5 + 0.8, "jumping in from below should throw you higher than you were going");
        assertTrue(below[0] > 0.1, "from below it keeps your sideways motion");
    }

    @Test
    void standingStillIsPushedOutTheFarSide() {
        double[] v = gust(0, 0, 0, 0.4, 0, 0);    // standing at the east edge
        assertTrue(v[0] < 0, "standing at the east edge should push out to the west, got " + v[0]);
    }

    @Test
    void fallingWithSidewaysSpeedFollowsTheMovement() {
        double[] v = GaleLaunch.vector(-0.6, -0.3, 0, 0, 0.2, 0, 1, 0);
        assertTrue(v[0] < 0, "a diagonal fall moving west should go west, not the way you face");
    }
}
