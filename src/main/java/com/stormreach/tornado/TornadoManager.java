package com.stormreach.tornado;

import com.stormreach.StormreachConfig;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoMorphology;
import com.stormreach.sim.TornadoProfile;
import com.stormreach.sim.TornadoRelation;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Natural tornadogenesis, the live-tornado registry and scheduled weather events. */
public final class TornadoManager {
    /** 6.0: forget everything from the world that just closed (a single-player session can open another). */
    public static void resetForNewWorld() {
        LIVE.clear();
        EVENTS.clear();
    }

    private static final List<TornadoEntity> LIVE = new ArrayList<>();
    private static final List<WeatherEvents.ActiveEvent> EVENTS = new ArrayList<>();

    private TornadoManager() {}

    public static int active(ServerLevel level) {
        LIVE.removeIf(t -> t.isRemoved());
        int n = 0;
        for (TornadoEntity t : LIVE) if (t.level() == level) n++;
        return n;
    }

    static void track(TornadoEntity t) {
        if (!LIVE.contains(t)) LIVE.add(t);
    }

    public static List<TornadoEntity> live(ServerLevel level) {
        LIVE.removeIf(t -> t.isRemoved());
        List<TornadoEntity> out = new ArrayList<>();
        for (TornadoEntity t : LIVE) if (t.level() == level) out.add(t);
        return out;
    }

    static List<WeatherEvents.ActiveEvent> events() {
        return EVENTS;
    }

    public static void schedule(WeatherEvents.ActiveEvent e) {
        EVENTS.add(e);
    }

    public static void tick(ServerLevel level) {
        for (Iterator<WeatherEvents.ActiveEvent> it = EVENTS.iterator(); it.hasNext(); ) {
            WeatherEvents.ActiveEvent e = it.next();
            if (e.level != level) continue;
            if (e.tick()) it.remove();
        }
        if (level.getGameTime() % 200 != 0 || level.dimension() != Level.OVERWORLD || !StormreachConfig.naturalTornadoes) return;
        double freq = StormreachConfig.tornadoFrequency / 100.0;
        if (freq <= 0) return;
        Random random = new Random(level.getRandom().nextLong());
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || active(level) >= StormreachConfig.maxActiveTornadoes) return;
            Holder<Biome> biome = level.getBiome(p.blockPosition());
            boolean ocean = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
            boolean frozen = biome.value().getBaseTemperature() < 0.15f;
            if (frozen) continue;
            TornadoFormation formation = null;
            if (level.isThundering() && random.nextDouble() < 0.006 * freq) {
                formation = TornadoFormation.SUPERCELL; // over water this is a tornadic waterspout
            } else if (!level.isThundering() && level.isRaining() && !ocean && StormreachConfig.landspouts
                    && biome.value().getBaseTemperature() > 0.6f && random.nextDouble() < 0.0015 * freq) {
                formation = TornadoFormation.LANDSPOUT;
            } else if (ocean && random.nextDouble() < (level.isRaining() ? 0.004 : 0.001) * freq) {
                formation = TornadoFormation.WATERSPOUT;
            }
            if (formation == null) continue;
            spawnNatural(level, p, formation, random);
        }
    }

    public static double sizeScale() {
        return StormreachConfig.tornadoSizePercent / 100.0;
    }

    public static double durationScale() {
        return StormreachConfig.tornadoDurationPercent / 100.0;
    }

    public static double steeringHeadingDeg() {
        Vec3 w = StormWeather.windVector();
        return Math.toDegrees(Math.atan2(w.z, w.x));
    }

    static void spawnNatural(ServerLevel level, ServerPlayer near, TornadoFormation formation, Random random) {
        TornadoProfile profile = TornadoProfile.roll(random, formation, StormreachConfig.tornadoIntensityBias / 100.0,
                sizeScale(), durationScale(), steeringHeadingDeg());
        // form upstream of the player so the storm's motion brings it closer
        double rad = Math.toRadians(profile.headingDeg());
        double dist = 150 + random.nextDouble() * 110;
        double side = (random.nextDouble() - 0.5) * 140;
        Vec3 at = near.position().add(-Math.cos(rad) * dist - Math.sin(rad) * side, 0, -Math.sin(rad) * dist + Math.cos(rad) * side);
        spawnWithRelations(level, at, profile, random);
    }

    /** Spawns a tornado and, sometimes, a satellite or companion tornado with it. */
    public static TornadoEntity spawnWithRelations(ServerLevel level, Vec3 at, TornadoProfile profile, Random random) {
        TornadoEntity main = TornadoEntity.spawn(level, at, profile);
        if (main == null) return null;
        track(main);
        announce(level, main);
        if (profile.formation() == TornadoFormation.SUPERCELL) {
            boolean big = profile.potentialRating().ordinal() >= EFScale.EF3.ordinal() && profile.width() / sizeScale() >= 200;
            if (big && random.nextDouble() < 0.08) spawnSatellite(level, main, random);
            if (random.nextDouble() < 0.05) spawnCompanion(level, main, random);
        }
        return main;
    }

    public static TornadoEntity spawnSatellite(ServerLevel level, TornadoEntity parent, Random random) {
        TornadoProfile sp = TornadoProfile.rollWith(random, TornadoFormation.SUPERCELL, random.nextBoolean() ? EFScale.EF0 : EFScale.EF1,
                random.nextBoolean() ? TornadoMorphology.ROPE : TornadoMorphology.CONE, sizeScale(), durationScale() * 0.5,
                parent.profile().headingDeg()).withRelation(TornadoRelation.SATELLITE);
        TornadoEntity s = TornadoEntity.spawn(level, parent.position().add(parent.visualWidth() + 20, 0, 0), sp);
        if (s != null) {
            s.setParent(parent.getUUID(), random.nextDouble() * Math.PI * 2);
            track(s);
        }
        return s;
    }

    public static TornadoEntity spawnCompanion(ServerLevel level, TornadoEntity main, Random random) {
        TornadoProfile cp = TornadoProfile.roll(random, TornadoFormation.SUPERCELL, 1.0, sizeScale(), durationScale(),
                main.profile().headingDeg()).withRelation(TornadoRelation.COMPANION);
        double rad = Math.toRadians(main.profile().headingDeg() + 90);
        double d = 220 + random.nextDouble() * 260;
        TornadoEntity c = TornadoEntity.spawn(level, main.position().add(Math.cos(rad) * d, 0, Math.sin(rad) * d), cp);
        if (c != null) track(c);
        return c;
    }

    static void announce(ServerLevel level, TornadoEntity t) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(t.position());
            if (d < 400) {
                StormAdvancements.award(p, "witness_tornado");
                if (Instruments.hasWarningDevice(p)) {
                    p.sendSystemMessage(Component.translatable("event.stormreach.tornado.warning", Instruments.bearing(p.position(), t.position()),
                            com.stormreach.units.PlayerUnits.distance(p, d)).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public static TornadoEntity nearest(ServerLevel level, Vec3 pos, double max) {
        TornadoEntity best = null;
        double bd = max * max;
        for (TornadoEntity t : live(level)) {
            double d = t.position().distanceToSqr(pos);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    /** Strongest tornado wind (mph) currently acting at a position, 0 if none. */
    public static double tornadoWindMph(ServerLevel level, Vec3 pos) {
        double best = 0;
        for (TornadoEntity t : live(level)) {
            if (t.position().distanceToSqr(pos) < Math.pow(t.visualWidth() * 6 + 60, 2)) best = Math.max(best, t.windMphAt(pos));
        }
        return best;
    }

    public static BlockPos below(Vec3 v) {
        return BlockPos.containing(v);
    }
}
