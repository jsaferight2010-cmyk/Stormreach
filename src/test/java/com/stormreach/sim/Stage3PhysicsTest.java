package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Stage3PhysicsTest {
    @Test
    void biggerHailFallsFasterAndHurtsMuchMore() {
        HailSize prev = null;
        for (HailSize s : HailSize.values()) {
            if (prev != null) {
                assertTrue(s.terminalMs() > prev.terminalMs());
                assertTrue(s.damage() > prev.damage(), s + " should hurt more than " + prev);
                assertTrue(s.visualSize() > prev.visualSize());
            }
            prev = s;
        }
        assertTrue(HailSize.PEA.damage() <= 1.0f, "pea hail should only sting");
        assertTrue(HailSize.GOLF_BALL.damage() >= 4f, "golf-ball hail should hurt badly");
        assertTrue(HailSize.SOFTBALL.damage() >= 12f, "softball hail can be deadly");
        assertTrue(HailSize.GOLF_BALL.terminalMs() > 20 && HailSize.GOLF_BALL.terminalMs() < 30, "golf-ball hail falls at about 25 m/s");
    }

    @Test
    void weakStormsNeverDropGiantHail() {
        Random r = new Random(1);
        for (int i = 0; i < 20000; i++) {
            HailSize s = HailSize.roll(r, 0.2);
            assertTrue(s.ordinal() <= HailSize.QUARTER.ordinal(), "a weak storm dropped " + s);
        }
        int big = 0;
        for (int i = 0; i < 20000; i++) if (HailSize.roll(r, 1.0).ordinal() >= HailSize.BASEBALL.ordinal()) big++;
        assertTrue(big > 200 && big < 6000, "giant hail should be possible but uncommon in the worst storms: " + big);
    }

    @Test
    void puddlesNeedSteadyRainAndDryAfterward() {
        assertEquals(0, Rainfall.wetness(0.5, 1000), 1e-9, "puddles shouldn't form in the first minute of rain");
        assertTrue(Rainfall.wetness(1.0, 20000) > Rainfall.wetness(0.5, 20000), "a downpour soaks faster");
        assertTrue(Rainfall.wetness(0.5, 30000) > Rainfall.wetness(0.5, 6000), "longer rain, more water");
        assertEquals(3, Rainfall.maxPuddleSize(Rainfall.wetness(1.0, 40000)));
        assertEquals(0, Rainfall.evaporation(true, 1.0f), 1e-9);
        assertTrue(Rainfall.evaporation(false, 2.0f) > Rainfall.evaporation(false, 0.3f), "puddles dry faster in hot places");
    }

    @Test
    void theGustFrontSpreadsOutward() {
        double max = 40;
        assertEquals(0, Outflow.windMph(30, 10, 140, 100, max), 1e-9, "air ahead of the front should still be calm");
        assertTrue(Outflow.windMph(30, 45, 140, 100, max) > 40, "the front should have reached 30 blocks after ~2 s");
        assertTrue(Outflow.windMph(2, 45, 140, 100, max) < Outflow.windMph(25, 45, 140, 100, max), "winds peak behind the front, not at the centre");
        assertEquals(0, Outflow.windMph(20, 140, 140, 100, max), 1e-9, "it should be over at the end");
    }

    @Test
    void stormCloudsBuildUpThenSpreadThenFade() {
        assertTrue(CloudGrowth.tower(0.02) < 0.2, "a new cloud should start low");
        assertTrue(CloudGrowth.tower(0.25) > CloudGrowth.tower(0.1), "the tower should keep climbing");
        assertEquals(1, CloudGrowth.tower(0.5), 1e-9, "a mature storm reaches full height");
        assertEquals(0, CloudGrowth.anvil(0.2), 1e-9, "no anvil before the tower tops out");
        assertTrue(CloudGrowth.anvil(0.7) > 0.95, "a mature storm has a full anvil");
        assertTrue(CloudGrowth.tower(1.0) < 1, "a dying storm slumps");
        assertEquals(0, CloudGrowth.opacity(1.0), 1e-9);
        assertTrue(CloudGrowth.opacity(0.0) < 0.05 && CloudGrowth.opacity(0.5) == 1);
    }
}
