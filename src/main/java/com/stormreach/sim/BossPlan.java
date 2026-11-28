package com.stormreach.sim;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phases and attack selection for Cyrrus, the Hollow Storm. Attacks have cooldowns and weights per phase;
 * selection never picks an attack that is still cooling down or that the current phase doesn't allow.
 */
public final class BossPlan {
    public enum Attack {
        LIGHTNING_VOLLEY(80), LIGHTNING_RING(140), DOWNPOUR(260), GALE_PUSH(120), CROSSWIND(200),
        HAIL_BARRAGE(220), SUMMON_SQUALLS(400), SUMMON_HOST(520), SPAWN_TORNADO(600), LIGHTNING_CAGE(300),
        BLINK(100), MAELSTROM(700);

        public final int cooldownTicks;

        Attack(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
        }
    }

    /** Phase 1 (lightning) above 80% health, 2 (rain) above 60%, 3 (wind) above 40%, 4 (summons) above 20%, 5 (everything) below. */
    public static int phaseFor(double healthFraction) {
        if (healthFraction > 0.8) return 1;
        if (healthFraction > 0.6) return 2;
        if (healthFraction > 0.4) return 3;
        if (healthFraction > 0.2) return 4;
        return 5;
    }

    public static Map<Attack, Integer> weights(int phase) {
        Map<Attack, Integer> m = new EnumMap<>(Attack.class);
        switch (phase) {
            // 1: lightning
            case 1 -> { m.put(Attack.LIGHTNING_VOLLEY, 5); m.put(Attack.LIGHTNING_RING, 3); m.put(Attack.BLINK, 1); }
            // 2: rain and visibility
            case 2 -> { m.put(Attack.DOWNPOUR, 5); m.put(Attack.HAIL_BARRAGE, 3); m.put(Attack.LIGHTNING_VOLLEY, 2); m.put(Attack.BLINK, 2); }
            // 3: wind (crosswinds deflect arrows and tridents)
            case 3 -> { m.put(Attack.GALE_PUSH, 4); m.put(Attack.CROSSWIND, 5); m.put(Attack.SPAWN_TORNADO, 2); m.put(Attack.LIGHTNING_RING, 1); }
            // 4: summons
            case 4 -> { m.put(Attack.SUMMON_SQUALLS, 4); m.put(Attack.SUMMON_HOST, 4); m.put(Attack.GALE_PUSH, 2); m.put(Attack.LIGHTNING_VOLLEY, 2); }
            // 5: the whole storm
            default -> { m.put(Attack.MAELSTROM, 3); m.put(Attack.LIGHTNING_CAGE, 4); m.put(Attack.SPAWN_TORNADO, 2);
                m.put(Attack.CROSSWIND, 2); m.put(Attack.SUMMON_HOST, 1); m.put(Attack.BLINK, 3); }
        }
        return m;
    }

    private final EnumMap<Attack, Integer> readyAt = new EnumMap<>(Attack.class);

    /** Picks the next attack at the given tick, or null if everything allowed is cooling down. */
    public Attack choose(int phase, long tick, Random random) {
        List<Attack> options = new ArrayList<>();
        List<Integer> w = new ArrayList<>();
        for (Map.Entry<Attack, Integer> e : weights(phase).entrySet()) {
            if (tick >= readyAt.getOrDefault(e.getKey(), 0)) { options.add(e.getKey()); w.add(e.getValue()); }
        }
        if (options.isEmpty()) return null;
        int sum = 0;
        for (int x : w) sum += x;
        int r = random.nextInt(sum);
        for (int k = 0; k < options.size(); k++) {
            r -= w.get(k);
            if (r < 0) {
                Attack a = options.get(k);
                readyAt.put(a, (int) tick + a.cooldownTicks);
                return a;
            }
        }
        return options.get(options.size() - 1);
    }

    public boolean coolingDown(Attack a, long tick) {
        return tick < readyAt.getOrDefault(a, 0);
    }
}
