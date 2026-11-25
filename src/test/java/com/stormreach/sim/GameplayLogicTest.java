package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameplayLogicTest {
    @Test
    void backpackTiersGrowStrictly() {
        int prev = 0;
        for (BackpackTier t : BackpackTier.values()) {
            assertTrue(t.baseSlots > prev, t + " must hold more than the tier before it");
            prev = t.baseSlots;
            assertTrue(t.slots(3) <= 256, "fits in the container component");
            if (t.ordinal() > 0) assertEquals(BackpackTier.values()[t.ordinal() - 1], t.previous());
        }
        assertNull(BackpackTier.DIAMOND.previous());
    }

    @Test
    void capacityEnchantmentAndPaging() {
        assertEquals(27, BackpackTier.DIAMOND.slots(0));
        assertEquals(54, BackpackTier.DIAMOND.slots(3));
        assertEquals(54, BackpackTier.DIAMOND.slots(99));
        assertEquals(3, BackpackTier.rows(27));
        assertEquals(6, BackpackTier.rows(54));
        assertEquals(6, BackpackTier.rows(135));
        assertEquals(3, BackpackTier.pages(135));
        assertEquals(134, BackpackTier.backingIndex(135, 2, 26));
        assertEquals(-1, BackpackTier.backingIndex(135, 2, 27));
        // every backing slot is reachable exactly once across pages
        int slots = BackpackTier.MAELSTROM.slots(3);
        boolean[] hit = new boolean[slots];
        for (int p = 0; p < BackpackTier.pages(slots); p++)
            for (int v = 0; v < BackpackTier.PAGE_SIZE; v++) {
                int idx = BackpackTier.backingIndex(slots, p, v);
                if (idx >= 0) { assertFalse(hit[idx], "slot mapped twice"); hit[idx] = true; }
            }
        for (boolean b : hit) assertTrue(b);
    }

    @Test
    void mazeIsLargeConnectedAndFullOfDeadEnds() {
        for (long seed = 0; seed < 20; seed++) {
            MazeLayout m = new MazeLayout(25, seed);
            assertTrue(m.consistent(), "passages must be two-way");
            assertEquals(25 * 25, m.reachableFromEntrance(), "every cell reachable (seed " + seed + ")");
            assertTrue(m.deadEnds() > 25, "should have many dead ends");
            assertEquals(3, m.keyVaults.size());
            assertFalse(m.lockerSpots.isEmpty());
            int c = 12;
            assertTrue((m.openAt(c, m.treasureMin) & MazeLayout.N) != 0, "treasure chamber door");
        }
        assertThrows(IllegalArgumentException.class, () -> new MazeLayout(5, 0));
    }

    @Test
    void hidingRules() {
        assertEquals(0.0, HidingRules.discoverChance(10, false, 1000), 0);
        assertEquals(1.0, HidingRules.discoverChance(2, true, 1000), 0);
        assertTrue(HidingRules.discoverChance(2, false, 10) > HidingRules.discoverChance(2, false, 1000));
        assertTrue(HidingRules.discoverChance(2, false, 1000) < 0.1);
        assertTrue(HidingRules.inside(5, 5, 5, 0, 0, 0, 10, 10, 10));
        assertFalse(HidingRules.inside(12, 5, 5, 0, 0, 0, 10, 10, 10));
        assertEquals(0.0, HidingRules.musicIntensity(80, false), 0);
        assertEquals(2.0, HidingRules.musicIntensity(80, true), 0);
        assertTrue(HidingRules.musicIntensity(10, false) > HidingRules.musicIntensity(30, false));
    }

    @Test
    void bossPhasesAndAttackSelection() {
        assertEquals(1, BossPlan.phaseFor(1.0));
        assertEquals(2, BossPlan.phaseFor(0.8));
        assertEquals(3, BossPlan.phaseFor(0.5));
        assertEquals(4, BossPlan.phaseFor(0.3));
        assertEquals(5, BossPlan.phaseFor(0.1));
        for (int p = 1; p <= 5; p++) assertTrue(BossPlan.weights(p).size() >= 3, "phase " + p + " needs variety");
        assertTrue(BossPlan.weights(5).containsKey(BossPlan.Attack.MAELSTROM));
        // themed phases: lightning, rain and visibility, wind, summons, everything
        assertTrue(BossPlan.weights(1).containsKey(BossPlan.Attack.LIGHTNING_VOLLEY) && BossPlan.weights(1).containsKey(BossPlan.Attack.LIGHTNING_RING));
        assertTrue(BossPlan.weights(2).containsKey(BossPlan.Attack.DOWNPOUR));
        assertTrue(BossPlan.weights(3).containsKey(BossPlan.Attack.CROSSWIND) && BossPlan.weights(3).containsKey(BossPlan.Attack.GALE_PUSH));
        assertTrue(BossPlan.weights(4).containsKey(BossPlan.Attack.SUMMON_HOST) && BossPlan.weights(4).containsKey(BossPlan.Attack.SUMMON_SQUALLS));
        BossPlan plan = new BossPlan();
        Random r = new Random(9);
        for (long tick = 0; tick < 20_000; tick += 20) {
            int phase = 1 + (int) (tick / 4000) % 5;
            BossPlan.Attack a = plan.choose(phase, tick, r);
            if (a != null) {
                assertTrue(BossPlan.weights(phase).containsKey(a), a + " not allowed in phase " + phase);
                assertTrue(plan.coolingDown(a, tick + 1));
            }
        }
    }

    @Test
    void shadowVanishesOnlyWhenStaredAt() {
        assertTrue(ShadowRules.beingStaredAt(0, 0, 1, 0, 0.1, 20));
        assertFalse(ShadowRules.beingStaredAt(1, 0, 0, 0, 0, 20));
        assertFalse(ShadowRules.beingStaredAt(0, 0, 1, 5, 0, 20));
        assertTrue(ShadowRules.appearanceChance(0.01, true, true, true, true, 0) > ShadowRules.appearanceChance(0.01, false, false, false, false, 0));
        assertTrue(ShadowRules.appearanceChance(1, true, true, true, true, 10) <= 0.25);
    }

    @Test
    void dialogueParsing() {
        DialogueScript s = DialogueScript.parse("# comment\n0|a|key.a|stormreach:voice.a|rise\n\n40|b|-|-|grow:2.0, thunder\n");
        assertEquals(2, s.cues.size());
        assertEquals(40, s.length());
        assertNull(s.cues.get(1).sound());
        assertEquals(2, s.cues.get(1).actions().size());
        assertThrows(IllegalArgumentException.class, () -> DialogueScript.parse("10|a|-|-|\n5|b|-|-|"));
        assertThrows(IllegalArgumentException.class, () -> DialogueScript.parse("10|a|-"));
    }
}
