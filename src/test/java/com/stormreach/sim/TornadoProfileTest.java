package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TornadoProfileTest {
    @Test
    void ratingDistributionFollowsClimatology() {
        Random r = new Random(1);
        int n = 200_000;
        int[] counts = new int[6];
        for (int i = 0; i < n; i++) counts[TornadoProfile.roll(r, TornadoFormation.SUPERCELL, 1.0, 1.0, 1.0, 45).potentialRating().ordinal()]++;
        for (int i = 0; i < 6; i++) {
            double expected = TornadoProfile.SUPERCELL_CLIMATOLOGY[i];
            assertEquals(expected, counts[i] / (double) n, Math.max(0.004, expected * 0.15), "share of EF" + i);
        }
    }

    @Test
    void gustsStayInsideTheirRatingRangeAndVary() {
        Random r = new Random(2);
        Map<EFScale, Set<Long>> seen = new EnumMap<>(EFScale.class);
        for (EFScale ef : EFScale.values()) {
            for (int i = 0; i < 2000; i++) {
                TornadoProfile p = TornadoProfile.rollWith(r, TornadoFormation.SUPERCELL, ef, null, 1, 1, 0);
                assertTrue(p.peakGustMph() >= ef.minMph, ef + " gust " + p.peakGustMph());
                if (!ef.openEnded()) assertTrue(p.peakGustMph() <= ef.maxMph + 1e-9, ef + " gust " + p.peakGustMph());
                assertEquals(ef, EFScale.fromGustMph(p.peakGustMph()));
                seen.computeIfAbsent(ef, k -> new HashSet<>()).add(Math.round(p.peakGustMph() * 10));
            }
            assertTrue(seen.get(ef).size() > 50, "gusts within " + ef + " should vary");
        }
    }

    @Test
    void landspoutsAndWaterspoutsStayWeak() {
        Random r = new Random(3);
        for (int i = 0; i < 50_000; i++) {
            assertTrue(TornadoProfile.roll(r, TornadoFormation.LANDSPOUT, 3.0, 1, 1, 0).potentialRating().ordinal() <= EFScale.EF2.ordinal());
            assertTrue(TornadoProfile.roll(r, TornadoFormation.WATERSPOUT, 3.0, 1, 1, 0).potentialRating().ordinal() <= EFScale.EF1.ordinal());
        }
    }

    @Test
    void wedgesAreMoreCommonInViolentTornadoes() {
        Random r = new Random(4);
        int weakWedge = 0, strongWedge = 0, n = 20_000;
        for (int i = 0; i < n; i++) {
            if (isWedge(TornadoProfile.rollWith(r, TornadoFormation.SUPERCELL, EFScale.EF0, null, 1, 1, 0))) weakWedge++;
            if (isWedge(TornadoProfile.rollWith(r, TornadoFormation.SUPERCELL, EFScale.EF4, null, 1, 1, 0))) strongWedge++;
        }
        assertEquals(0, weakWedge);
        assertTrue(strongWedge > n * 0.4);
    }

    private static boolean isWedge(TornadoProfile p) {
        return p.morphology() == TornadoMorphology.WEDGE || p.morphology() == TornadoMorphology.LARGE_WEDGE;
    }

    @Test
    void widthsTranslationAndLifetimesAreRealistic() {
        Random r = new Random(5);
        double transSum = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            TornadoProfile p = TornadoProfile.roll(r, TornadoFormation.SUPERCELL, 1, 1, 1, 0);
            assertTrue(p.width() >= p.morphology().minWidth * 0.99 && p.width() <= p.morphology().maxWidth * 1.01, "width " + p.width() + " for " + p.morphology());
            assertTrue(p.translationMs() >= 0 && p.translationMs() <= 31);
            assertTrue(p.lifetimeTicks() >= 400 && p.lifetimeTicks() <= 60 * 60 * 20);
            transSum += p.translationMs();
        }
        double mean = transSum / n;
        assertEquals(13.7, mean, 2.0, "mean translation ~30 mph");
    }

    @Test
    void multiVortexOnlyWhereItMakesSense() {
        Random r = new Random(6);
        for (int i = 0; i < 20_000; i++) {
            TornadoProfile p = TornadoProfile.rollWith(r, TornadoFormation.SUPERCELL, EFScale.EF3, TornadoMorphology.ROPE, 1, 1, 0);
            assertEquals(VortexStructure.SINGLE, p.structure());
            TornadoProfile l = TornadoProfile.roll(r, TornadoFormation.LANDSPOUT, 1, 1, 1, 0);
            assertEquals(VortexStructure.SINGLE, l.structure());
        }
    }

    @Test
    void stagesRampUpAndDown() {
        assertEquals(0.0, TornadoStage.intensityAt(0), 1e-9);
        assertEquals(1.0, TornadoStage.intensityAt(0.5), 1e-9);
        assertEquals(0.0, TornadoStage.intensityAt(1), 1e-9);
        assertEquals(TornadoStage.DECAYING, TornadoStage.at(0.95));
        assertEquals(TornadoStage.ORGANIZING, TornadoStage.at(0.05));
        double prev = -1;
        for (double f = 0; f <= 0.25; f += 0.01) {
            assertTrue(TornadoStage.intensityAt(f) >= prev);
            prev = TornadoStage.intensityAt(f);
        }
    }

    @Test
    void rotationIsUsuallyCyclonicForSupercells() {
        Random r = new Random(7);
        int anti = 0, n = 50_000;
        for (int i = 0; i < n; i++) if (TornadoProfile.roll(r, TornadoFormation.SUPERCELL, 1, 1, 1, 0).rotationSign() < 0) anti++;
        assertEquals(0.02, anti / (double) n, 0.005);
    }
}
