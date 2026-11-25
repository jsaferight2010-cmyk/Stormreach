package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VortexForcesTest {
    private static double climbAfter(double mph, int ticks) {
        // a player standing in a steady wind with the tornado's updraft
        double m = VortexForces.mass(0.6, 1.8, 0);
        double ceiling = VortexForces.ceiling(mph);
        double[] v = {0, 0, 0};
        double y = 0;
        double ms = EFScale.mphToMs(mph) * 0.05;
        for (int i = 0; i < ticks; i++) {
            v = VortexForces.step(v, new double[]{ms, ms * 0.15, 0}, mph, m, y <= 0, false, y, ceiling, new double[]{1, 0});
            v[1] -= VortexForces.LIVING_GRAVITY;
            v[1] *= 0.98;
            y = Math.max(0, y + v[1]);
            if (y <= 0) v[1] = 0;
        }
        return y;
    }

    @Test
    void ef0ShovesEf1PullsEf2Lifts() {
        double m = VortexForces.mass(0.6, 1.8, 0);
        assertEquals(0, VortexForces.pull(75, m), 1e-9, "EF0 winds should not pull a player in");
        assertTrue(VortexForces.pull(95, m) > 0, "EF1 winds should pull a player toward the funnel");
        assertEquals(0, climbAfter(80, 100), 1e-9, "an EF0 wind lifted a player");
        assertEquals(0, climbAfter(100, 100), 1e-9, "an EF1 wind lifted a player");
        assertTrue(climbAfter(120, 100) > 1.5, "an EF2 wind should lift a player off the ground, got " + climbAfter(120, 100));
        assertTrue(climbAfter(120, 200) < VortexForces.ceiling(120) + 3, "an EF2 should only lift a player a modest height");
    }

    @Test
    void forcesFallOffWithDistanceFromTheTornado() {
        // an EF2 tornado: 125 mph peak, 10-block core radius
        WindField f = WindField.forPeakGust(EFScale.mphToMs(125), 10, 0, 0, 1, 0);
        double m = VortexForces.mass(0.6, 1.8, 0);
        double prevPull = Double.MAX_VALUE;
        boolean liftedAtCore = false, liftedFar = false;
        for (double r = 10; r <= 60; r += 5) {
            double mph = f.sample(r, 0, 10, 0).horizontalMph();
            double p = VortexForces.pull(mph, m);
            assertTrue(p <= prevPull + 1e-12, "pull should weaken with distance (r=" + r + ")");
            prevPull = p;
            if (r <= 10) liftedAtCore = VortexForces.lift(mph, m, 0, 20) > 0;
            if (r >= 30) liftedFar |= VortexForces.lift(mph, m, 0, 20) > 0;
        }
        assertTrue(liftedAtCore, "an EF2 should lift a player at its core's edge");
        assertFalse(liftedFar, "an EF2 should not lift a player three core radii away");
        assertEquals(0, VortexForces.pull(f.sample(60, 0, 10, 0).horizontalMph(), m), 1e-9, "no pull six core radii out");
    }

    @Test
    void anEf5CoreCarriesAPlayerHigh() {
        double y = climbAfter(230, 200);
        assertTrue(y > 12, "an EF5 core should carry a player high into the air, got " + y);
        assertTrue(y < VortexForces.ceiling(230) + 6, "bodies must not climb past the ceiling, got " + y);
    }

    @Test
    void heavierBodiesResistMore() {
        double player = VortexForces.mass(0.6, 1.8, 0), golem = VortexForces.mass(1.4, 2.7, 0);
        assertTrue(VortexForces.dragFactor(golem, false, false) < VortexForces.dragFactor(player, false, false));
        assertTrue(VortexForces.liftThresholdMph(golem) > VortexForces.liftThresholdMph(player));
        assertTrue(VortexForces.dragFactor(player, true, true) < VortexForces.dragFactor(player, true, false), "bracing should help");
    }

    @Test
    void speedIsCapped() {
        double[] v = VortexForces.step(new double[]{5, 5, 5}, new double[]{9, 9, 9}, 300, 0.2, false, false, 0, 40, new double[]{1, 0});
        assertTrue(Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) <= VortexForces.MAX_SPEED + 1e-9);
    }

    @Test
    void ef5DamageIsMajorAndWeakWindIsHarmless() {
        assertEquals(0, VortexForces.windDamagePerSecond(100), 1e-9);
        assertTrue(VortexForces.windDamagePerSecond(220) >= 6, "an EF5 core should hurt fast");
        // with the wind, the fall from the carry ceiling is lethal on its own (vanilla: 1 damage per block past 3)
        assertTrue(VortexForces.ceiling(220) - 3 > 20);
    }

    @Test
    void debrisOrbitsWithTheRotationAndIsThrownOutward() {
        // cyclonic (+1): at (r, 0) the tangential direction is (0, -1) in x/z, matching WindField
        double[] v = VortexForces.carry(10, 0, 2, 10, 1.0, 0.2, 30, 1, 0.04);
        assertTrue(v[2] < -0.9 && Math.abs(v[0]) < 0.05, "orbit direction wrong: " + v[0] + "," + v[2]);
        assertTrue(v[1] > 0.2, "debris should rise");
        double[] in = VortexForces.carry(20, 0, 2, 10, 1.0, 0.2, 30, 1, 0.04);
        assertTrue(in[0] < 0, "debris outside the orbit should be drawn inward");
        double[] e = VortexForces.eject(10, 0, 1.0, 1);
        assertTrue(e[0] > 0.4, "ejected debris should fly outward");
        WindField f = new WindField(10, 50, 0, 0, 1, 0, 0);
        WindField.Sample s = f.sample(10, 0, 5, 0);
        assertTrue(Math.signum(s.w()) == Math.signum(v[2]), "debris orbit must match the wind's rotation");
    }

    @Test
    void strongerTornadoesDoMoreWorkAndCarryLonger() {
        assertEquals(96, VortexForces.blockChecks(96, 60, 60));
        assertTrue(VortexForces.blockChecks(96, 230, 200) >= 96 * 3, "an EF5 should check far more blocks");
        assertTrue(VortexForces.blockChecks(96, 230, 200) <= 96 * 4);
        assertTrue(VortexForces.carryTicks(220, 0.5) > VortexForces.carryTicks(90, 0.5));
    }
}
