package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindFieldTest {
    @Test
    void tangentialWindPeaksAtTheCoreRadius() {
        double rc = 50, vm = 60;
        assertEquals(vm, WindField.tangential(vm, rc, rc), 1e-9);
        assertTrue(WindField.tangential(vm, rc, rc * 0.5) < vm);
        assertTrue(WindField.tangential(vm, rc, rc * 2) < vm);
        // inside the core the profile is solid-body rotation (linear in r)
        assertEquals(vm * 0.25, WindField.tangential(vm, rc, rc * 0.25), 1e-9);
    }

    @Test
    void windDecaysWithDistanceInsteadOfBeingUniform() {
        WindField f = new WindField(40, 70, 0, 0, 1, 0, 0);
        double near = f.sample(40, 0, 15, 0).horizontal();
        double far = f.sample(400, 0, 15, 0).horizontal();
        assertTrue(far < near * 0.4, "10 core radii out should be far weaker: near=" + near + " far=" + far);
    }

    @Test
    void groundRelativePeakMatchesTheTargetGust() {
        for (double gustMph : new double[]{75, 120, 160, 210}) {
            double g = EFScale.mphToMs(gustMph);
            WindField f = WindField.forPeakGust(g, 60, 12, 5, 1, 0);
            double peak = f.scanPeakGust(72);
            assertEquals(g, peak, g * 0.03, "peak for " + gustMph + " mph");
        }
    }

    @Test
    void translationMakesTheRightSideStronger() {
        // moving east (+x); cyclonic rotation; the right side of the motion is the south side (+z) in MC coordinates
        WindField f = new WindField(50, 60, 15, 0, 1, 0, 0);
        double south = f.sample(0, 50, 15, 0).horizontal();
        double north = f.sample(0, -50, 15, 0).horizontal();
        assertTrue(Math.abs(south - north) > 25, "asymmetry expected, south=" + south + " north=" + north);
    }

    @Test
    void cyclonicRotationIsCounterClockwiseSeenFromAbove() {
        WindField f = new WindField(50, 60, 0, 0, 1, 0, 0);
        // east of centre, counter-clockwise flow seen from above (with +z = south) points north (-z)
        WindField.Sample s = f.sample(50, 0, 15, 0);
        assertTrue(s.w() < -40, "expected northward wind east of a cyclonic vortex, got v=" + s.w());
    }

    @Test
    void pressureDeficitFollowsCyclostrophicBalance() {
        WindField f = new WindField(50, 90, 0, 0, 1, 0, 0);
        double centreHpa = f.pressureDeficit(0) / 100.0;
        assertEquals(1.2 * 90 * 90 / 100.0, centreHpa, 0.01);
        assertTrue(centreHpa > 80 && centreHpa < 120, "~100 hPa for a violent tornado, got " + centreHpa);
        assertTrue(f.pressureDeficit(500) < f.pressureDeficit(50));
    }

    @Test
    void subvorticesProduceLocalMaxima() {
        WindField single = WindField.forPeakGust(80, 100, 0, 0, 1, 0);
        WindField multi = WindField.forPeakGust(80, 100, 0, 0, 1, 4);
        assertTrue(multi.vMax < single.vMax, "parent circulation weaker when subvortices carry the peak");
        assertEquals(80, multi.scanPeakGust(90), 80 * 0.05);
    }

    @Test
    void inflowAndUpdraftExist() {
        WindField f = new WindField(50, 70, 0, 0, 1, 0, 0);
        WindField.Sample out = f.sample(120, 0, 5, 0);
        assertTrue(out.radial() < 0, "near-surface flow should spiral inward");
        WindField.Sample core = f.sample(40, 0, 60, 0);
        assertTrue(core.vertical() > 10, "updraft near the core");
    }
}
