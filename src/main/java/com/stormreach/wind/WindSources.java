package com.stormreach.wind;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 6.0: the wind that Wind Generators blow, as a field every instrument, turbine and the radar can read. Each running
 * generator refreshes its entry every tick; an entry not refreshed for two seconds (the generator was removed, turned
 * off or unloaded) is dropped the next time anyone reads the field, so nothing has to clean up after a generator.
 */
public final class WindSources {
    private WindSources() {}

    /** One fan: blows toward (dirX, dirZ) from {@code origin}, out to {@code radius} blocks, at {@code mph} at the mouth. */
    public record Source(Vec3 origin, double dirX, double dirZ, double mph, int radius, float gustiness, long seen) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Source>> LEVELS = new ConcurrentHashMap<>();

    static void put(ServerLevel level, BlockPos pos, Source s) {
        LEVELS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(pos.immutable(), s);
    }

    static void remove(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Source> m = LEVELS.get(level.dimension());
        if (m != null) m.remove(pos);
    }

    /** Shape of the field (see {@link com.stormreach.sim.FanField}). */
    public static double strength(double relX, double relY, double relZ, double dirX, double dirZ, int radius) {
        return com.stormreach.sim.FanField.strength(relX, relY, relZ, dirX, dirZ, radius);
    }

    private static Map<BlockPos, Source> live(ServerLevel level) {
        Map<BlockPos, Source> m = LEVELS.get(level.dimension());
        if (m == null || m.isEmpty()) return null;
        long now = level.getGameTime();
        for (Iterator<Source> it = m.values().iterator(); it.hasNext(); ) if (now - it.next().seen() > 40) it.remove();
        return m.isEmpty() ? null : m;
    }

    /** Strongest generator wind at {@code p}, in mph (0 when no generator reaches it). */
    public static double mphAt(ServerLevel level, Vec3 p) {
        Source best = strongest(level, p);
        return best == null ? 0 : best.mph() * strength(p.x - best.origin().x, p.y - best.origin().y, p.z - best.origin().z,
                best.dirX(), best.dirZ(), best.radius());
    }

    /** The generator that dominates the wind at {@code p}, or null. */
    public static Source strongest(ServerLevel level, Vec3 p) {
        Map<BlockPos, Source> m = live(level);
        if (m == null) return null;
        Source best = null;
        double bestMph = 0.5;
        for (Source s : m.values()) {
            double mph = s.mph() * strength(p.x - s.origin().x, p.y - s.origin().y, p.z - s.origin().z, s.dirX(), s.dirZ(), s.radius());
            if (mph > bestMph) { bestMph = mph; best = s; }
        }
        return best;
    }

    /** 16-point compass label for where a generator's wind comes FROM at {@code p} (null: no generator there). */
    public static String fromAt(ServerLevel level, Vec3 p) {
        Source s = strongest(level, p);
        if (s == null || mphAt(level, p) < 3) return null;
        return com.stormreach.tornado.Instruments.compass(Math.toDegrees(Math.atan2(s.dirX(), -s.dirZ())) + 180);
    }
}
