package com.stormreach.test;

import com.stormreach.Stormreach;
import net.minecraft.gametest.framework.GameTestHelper;

/** GameTest assertions that also write the failure to the log, so CI output shows which check failed and why. */
final class Check {
    private Check() {}

    static void isTrue(GameTestHelper h, boolean ok, String msg) {
        if (!ok) Stormreach.LOGGER.error("GAMETEST CHECK FAILED: {}", msg);
        h.assertTrue(ok, msg);
    }

    static void isFalse(GameTestHelper h, boolean bad, String msg) {
        isTrue(h, !bad, msg);
    }
}
