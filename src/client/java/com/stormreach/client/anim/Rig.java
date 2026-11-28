package com.stormreach.client.anim;

import net.minecraft.util.Mth;

/** Small helpers the generated models use. */
public final class Rig {
    public static final float DEG = Mth.DEG_TO_RAD;

    private Rig() {}

    public static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    /**
     * A fidget (an idle flourish like a claw snap or ear flick) that plays now and then: returns its progress in [0,1),
     * or -1 between plays. Each entity's seed staggers its timing.
     */
    public static float fidget(float t, float seed, float period, float dur) {
        float local = (t + seed * period * 7.31f) % period;
        float start = period - dur;
        return local >= start ? (local - start) / dur : -1f;
    }

    /** One-shot progress for a pulse clip: [0,1) while it plays, -1 otherwise. */
    public static float pulse(float age, float dur) {
        return age >= 0 && age < dur ? age / dur : -1f;
    }
}
