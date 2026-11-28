package com.stormreach.anim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tells clients that a Stormreach mob just attacked (or cast something), so its model can play that clip.
 * The server broadcasts an entity event; {@code LivingEntityMixin} catches it on the client and records the tick here.
 */
public final class AnimPulse {
    public static final byte ATTACK = 113, CAST = 114;
    private static final Map<Entity, int[]> LAST = new WeakHashMap<>();

    private AnimPulse() {}

    /** Server side: plays the attack (or cast) clip for everyone watching. */
    public static void fire(Entity e, byte kind) {
        if (!e.level().isClientSide() && isOurs(e)) e.level().broadcastEntityEvent(e, kind);
    }

    public static boolean isOurs(Entity e) {
        return "stormreach".equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
    }

    /** Client side: called from the entity-event hook. */
    public static synchronized void record(Entity e, byte kind) {
        LAST.put(e, new int[] {e.tickCount, kind});
    }

    /** Client side: ticks since the last pulse (partial-tick accurate), or a large number if there wasn't one. */
    public static synchronized float age(Entity e, float partial) {
        int[] v = LAST.get(e);
        return v == null ? 1e6f : e.tickCount - v[0] + partial;
    }

    public static synchronized int kind(Entity e) {
        int[] v = LAST.get(e);
        return v == null ? ATTACK : v[1];
    }
}
