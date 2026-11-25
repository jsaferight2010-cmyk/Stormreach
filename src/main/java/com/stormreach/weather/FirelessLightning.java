package com.stormreach.weather;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Lightning that hurts but never burns: used by the Tempest (Lightning) Staff. The bolt still does its lightning
 * damage, but it places no fire blocks and anything it hits keeps exactly the burn time it had before the strike, so
 * the caster and targets take no fire damage from it. Nothing else is touched: no Fire Resistance effect, and fire or
 * lava from any other source still hurts as normal.
 */
public final class FirelessLightning {
    private static final Set<LightningBolt> BOLTS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LightningBolt, Map<Entity, Integer>> BEFORE = new WeakHashMap<>();

    private FirelessLightning() {}

    public static void mark(LightningBolt bolt) { BOLTS.add(bolt); }

    public static boolean isFireless(LightningBolt bolt) { return BOLTS.contains(bolt); }

    /** Before the bolt's tick: remember how long everything that it could hit (vanilla radius 3) was burning. */
    public static void beforeTick(LightningBolt bolt) {
        if (!isFireless(bolt)) return;
        Map<Entity, Integer> m = new HashMap<>();
        AABB box = new AABB(bolt.getX() - 3, bolt.getY() - 3, bolt.getZ() - 3, bolt.getX() + 3, bolt.getY() + 9, bolt.getZ() + 3);
        for (Entity e : bolt.level().getEntities(bolt, box)) m.put(e, e.getRemainingFireTicks());
        BEFORE.put(bolt, m);
    }

    /** After the bolt's tick: whatever it struck gets its earlier burn time back. */
    public static void afterTick(LightningBolt bolt) {
        Map<Entity, Integer> m = BEFORE.remove(bolt);
        if (m == null) return;
        bolt.getHitEntities().forEach(e -> e.setRemainingFireTicks(m.getOrDefault(e, 0)));
    }
}
