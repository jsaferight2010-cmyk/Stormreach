package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeeperSensesTest {
    @Test
    void quietPlayersCanSlipPast() {
        assertEquals(0, KeeperSenses.noiseRadius(false, false, false, false), 1e-9, "standing still makes no sound");
        assertFalse(KeeperSenses.hears(6, KeeperSenses.noiseRadius(true, true, false, false), false), "sneaking 6 blocks away should go unheard");
        assertTrue(KeeperSenses.hears(20, KeeperSenses.noiseRadius(true, false, true, false), false), "sprinting 20 blocks away should be heard");
        assertTrue(KeeperSenses.hears(30, KeeperSenses.noiseRadius(false, false, false, true), false), "breaking blocks carries far");
        assertTrue(KeeperSenses.hears(18, 14, true) && !KeeperSenses.hears(18, 14, false), "a wounded Keeper hears further");
    }

    @Test
    void itsGuessSharpensUpClose() {
        assertTrue(KeeperSenses.guessError(3) < KeeperSenses.guessError(25));
        assertTrue(KeeperSenses.guessError(100) <= 8);
        assertTrue(KeeperSenses.listenInterval(0.3f) < KeeperSenses.listenInterval(0.9f));
        assertTrue(KeeperSenses.lunge(6, 0) && !KeeperSenses.lunge(6, 5) && !KeeperSenses.lunge(2, 0) && !KeeperSenses.lunge(15, 0));
    }
}
