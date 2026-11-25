package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunnelShapeTest {
    @Test
    void theFunnelComesDownFromTheCloudAndReachesTheGround() {
        assertEquals(0, FunnelShape.reach(0), 1e-9, "a new tornado's funnel should start inside the cloud");
        double prev = -1;
        for (double f = 0; f <= FunnelShape.DESCENT_END; f += 0.01) {
            double r = FunnelShape.reach(f);
            assertTrue(r >= prev, "the funnel should lower steadily");
            prev = r;
        }
        assertEquals(1, FunnelShape.reach(0.5), 1e-9);
        assertTrue(FunnelShape.touchedDown(FunnelShape.reach(0.3)));
        assertEquals(0, FunnelShape.tipHeight(80, FunnelShape.reach(0.5)), 1e-9, "a mature funnel must reach the ground");
        assertFalse(FunnelShape.touchedDown(FunnelShape.reach(0.05)), "no damage before touchdown");
    }

    @Test
    void itLiftsBackIntoTheCloudAtTheEnd() {
        assertTrue(FunnelShape.reach(0.99) < 0.2);
        assertEquals(0, FunnelShape.reach(1.0), 1e-9);
        assertFalse(FunnelShape.touchedDown(FunnelShape.reach(0.98)));
    }

    @Test
    void theShapeHasATipWhileDescendingAndTouchesTheGroundWhenDown() {
        double base = 80;
        double reach = 0.5; // tip 40 blocks up
        assertEquals(0, FunnelShape.radius(TornadoMorphology.CONE, 20, 30, base, reach), 1e-9, "nothing below the tip");
        double nearTip = FunnelShape.radius(TornadoMorphology.CONE, 20, 40.5, base, reach);
        double higher = FunnelShape.radius(TornadoMorphology.CONE, 20, 60, base, reach);
        assertTrue(nearTip < higher * 0.5, "the descending tip should be pointed");
        for (TornadoMorphology m : TornadoMorphology.values())
            assertTrue(FunnelShape.radius(m, 20, 0.2, base, 1.0) > 0.5, m + " doesn't reach the ground when down");
        assertTrue(FunnelShape.radius(TornadoMorphology.CONE, 20, 79, base, 1) > FunnelShape.radius(TornadoMorphology.CONE, 20, 60, base, 1),
                "the funnel should flare into the cloud base");
    }
}
