package com.stormreach.tornado;

import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModEntities;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.StructuralIntegrity.MaterialClass;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoProfile;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Non-tornado severe weather events: downbursts (straight-line winds), hail bursts, supercells (a rotating
 * storm that produces a tornado after a short delay) and ball-lightning swarms.
 */
public final class WeatherEvents {
    private WeatherEvents() {}

    public abstract static class ActiveEvent {
        final ServerLevel level;
        final Vec3 centre;
        int age;
        final int duration;

        ActiveEvent(ServerLevel level, Vec3 centre, int duration) {
            this.level = level;
            this.centre = centre;
            this.duration = duration;
        }

        /** @return true when finished */
        boolean tick() {
            age++;
            run();
            return age >= duration;
        }

        abstract void run();
    }

    /**
     * Downburst: a column of rain-cooled air slams into the ground and spreads out as a ring (the gust front).
     * A <b>wet</b> downburst comes down inside a curtain of heavy rain and its outflow drives a wall of spray and mist
     * (the "rain foot"); a <b>dry</b> one falls out of virga (rain that evaporates on the way down) and shows only as
     * a ring of dust racing across the ground. Winds peak just behind the front; the damage pattern spreads out from
     * the centre instead of turning like a tornado's. See {@link com.stormreach.sim.Outflow}.
     */
    public static final class Downburst extends ActiveEvent {
        final double radius;
        final double peakMph;
        final boolean wet;
        final BlockState ground;

        Downburst(ServerLevel level, Vec3 centre, double radius, double peakMph, boolean wet) {
            super(level, centre, 160);
            this.radius = radius;
            this.peakMph = peakMph;
            this.wet = wet;
            BlockPos g = BlockPos.containing(centre.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(centre.x), Mth.floor(centre.z)) - 1, centre.z);
            BlockState gs = level.getBlockState(g);
            this.ground = gs.isAir() ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState() : gs;
        }

        double mphAt(Vec3 p) {
            double r = Math.hypot(p.x - centre.x, p.z - centre.z);
            return com.stormreach.sim.Outflow.windMph(r, age, duration, peakMph, radius * 2);
        }

        @Override
        void run() {
            var rnd = level.getRandom();
            double front = com.stormreach.sim.Outflow.frontRadius(age, radius * 2);
            float density = StormreachConfig.particleDensity / 100f;
            // the shaft: a curtain of rain (wet) or ragged virga hanging under the cloud (dry)
            if (age < duration - 30) {
                for (int i = 0; i < (int) (14 * density); i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * radius * 0.55;
                    double x = centre.x + Math.cos(a) * d, z = centre.z + Math.sin(a) * d;
                    if (wet) {
                        level.sendParticles(ParticleTypes.FALLING_WATER, x, centre.y + 4 + rnd.nextDouble() * 26, z, 1, 0, 0, 0, 0);
                        if (i % 3 == 0) level.sendParticles(ParticleTypes.CLOUD, x, centre.y + 0.3, z, 1, 0.2, 0.1, 0.2, 0.02);
                    } else {
                        level.sendParticles(ParticleTypes.WHITE_ASH, x, centre.y + 22 + rnd.nextDouble() * 10, z, 1, 0.3, 0.6, 0.3, 0.0);
                    }
                }
            }
            // the gust front: a ring racing outward along the ground, curling up at its leading edge
            int n = (int) (Math.min(48, 10 + front * 1.2) * density);
            for (int i = 0; i < n; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = front - rnd.nextDouble() * 3;
                double x = centre.x + Math.cos(a) * d, z = centre.z + Math.sin(a) * d;
                double vx = Math.cos(a) * 0.6, vz = Math.sin(a) * 0.6;
                if (wet) {
                    level.sendParticles(i % 2 == 0 ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, x, centre.y + 0.4 + rnd.nextDouble() * 2.5, z, 0, vx, 0.12, vz, 0.5);
                } else {
                    level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground), x, centre.y + 0.3, z, 1, 0.3, 0.2, 0.3, 0.2);
                    if (i % 2 == 0) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, centre.y + 0.5 + rnd.nextDouble() * 2, z, 0, vx * 0.5, 0.05, vz * 0.5, 0.4);
                }
            }
            // wind on bodies: outward, falling off with distance behind and ahead of the front
            AABB box = new AABB(centre.x - radius * 2, centre.y - 6, centre.z - radius * 2, centre.x + radius * 2, centre.y + 30, centre.z + radius * 2);
            for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof TornadoEntity) && !e.isSpectator() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                double mph = mphAt(e.position());
                if (mph < 30) continue;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z, r = Math.max(0.01, Math.hypot(dx, dz));
                double bpt = EFScale.mphToMs(mph) * 0.05;
                int armor = e instanceof LivingEntity le ? le.getArmorValue() : 0;
                double mass = com.stormreach.sim.VortexForces.mass(e.getBbWidth(), e.getBbHeight(), armor);
                Vec3 v = e.getDeltaMovement();
                double[] nv = com.stormreach.sim.VortexForces.step(new double[]{v.x, v.y, v.z}, new double[]{dx / r * bpt, -0.02, dz / r * bpt}, mph, mass,
                        e.onGround(), e instanceof Player p && p.isShiftKeyDown(), 0, 3, new double[]{0, 0});
                e.setDeltaMovement(nv[0], nv[1], nv[2]);
                e.needsSync = true;
                if (e instanceof LivingEntity le && age % 10 == 0 && mph > 110) le.hurtServer(level, level.damageSources().flyIntoWall(), (float) ((mph - 110) / 30.0));
            }
            if (StormreachConfig.tornadoDestruction && age > 15 && age < duration - 30) {
                for (int i = 0; i < Math.min(48, StormreachConfig.tornadoBlockChecksPerTick / 2); i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * front;
                    int x = Mth.floor(centre.x + Math.cos(a) * d), z = Mth.floor(centre.z + Math.sin(a) * d);
                    BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                    BlockState s = level.getBlockState(pos);
                    MaterialClass m = BlockWind.classify(level, pos, s);
                    if (m == MaterialClass.UNBREAKABLE || m == MaterialClass.LOOSE_EARTH) continue;
                    if (mphAt(Vec3.atCenterOf(pos)) >= BlockWind.failureMph(level, pos, s, m)) level.destroyBlock(pos, m != MaterialClass.FOLIAGE);
                }
            }
            if (age == 1) level.playSound(null, BlockPos.containing(centre), com.stormreach.registry.ModSfx.WEATHER_GUST, SoundSource.WEATHER, 4f, wet ? 0.4f : 0.6f);
            if (age % 30 == 5) level.playSound(null, BlockPos.containing(centre), com.stormreach.registry.ModSfx.WEATHER_WIND, SoundSource.WEATHER, 3f, 0.5f);
        }
    }

    /**
     * Hailstorm: a core of falling hailstones under a storm. Its severity (0..1, the strength of the updraft) decides
     * how big the stones get; see {@link com.stormreach.sim.HailSize}. Stones only fall near players, and the total
     * number in the air is capped.
     */
    public static final class HailBurst extends ActiveEvent {
        final double radius;
        final double severity;
        final java.util.Random random;

        HailBurst(ServerLevel level, Vec3 centre, double severity) {
            super(level, centre, 900 + level.getRandom().nextInt(900));
            this.severity = severity;
            this.radius = 24 + severity * 24;
            this.random = new java.util.Random(level.getRandom().nextLong());
        }

        @Override
        void run() {
            double ramp = age < 100 ? age / 100.0 : age > duration - 100 ? (duration - age) / 100.0 : 1.0;
            if (age % 30 == 0) for (Player p : level.players())   // the clatter of hail all around
                if (Math.hypot(p.getX() - centre.x, p.getZ() - centre.z) < radius + 8)
                    level.playSound(null, p.getX(), p.getY() + 2, p.getZ(), com.stormreach.registry.ModSfx.WEATHER_HAIL, SoundSource.WEATHER, (float) (0.4 + 0.6 * severity * ramp), 0.9f + level.getRandom().nextFloat() * 0.2f);
            double windX = Math.cos(age * 0.003) * 0.06, windZ = Math.sin(age * 0.003) * 0.06;
            for (Player p : level.players()) {
                if (p.isSpectator() || Math.hypot(p.getX() - centre.x, p.getZ() - centre.z) > radius + 48) continue;
                int count = (int) Math.round((2 + 6 * severity) * ramp * StormreachConfig.particleDensity / 100.0);
                for (int i = 0; i < count; i++) {
                    double x = p.getX() + (random.nextDouble() - 0.5) * 48, z = p.getZ() + (random.nextDouble() - 0.5) * 48;
                    if (Math.hypot(x - centre.x, z - centre.z) > radius) continue;
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
                    double y = Math.max(top, p.getY()) + 24 + random.nextDouble() * 8;
                    com.stormreach.weather.precip.HailstoneEntity.drop(level, new Vec3(x, y, z), com.stormreach.sim.HailSize.roll(random, severity), windX, windZ);
                }
            }
        }
    }

    /** A supercell: a rotating wall cloud appears and, after 20–40 seconds, a tornado forms beneath it. */
    public static final class Supercell extends ActiveEvent {
        final int genesisAt;
        final com.stormreach.sim.EFScale forced;
        final com.stormreach.sim.TornadoMorphology morph;

        Supercell(ServerLevel level, Vec3 centre, com.stormreach.sim.EFScale forced, com.stormreach.sim.TornadoMorphology morph) {
            super(level, centre, 800);
            this.genesisAt = 400 + level.getRandom().nextInt(400);
            this.forced = forced;
            this.morph = morph;
        }

        @Override
        void run() {
            if (age == 1 && StormreachConfig.stormClouds) {
                com.stormreach.weather.cloud.StormCells.spawnNear(level, centre, level.getRandom(), true, true);
            }
            if (age % 3 == 0) {
                double a = age * 0.05;
                for (int i = 0; i < 6; i++) {
                    double ang = a + i * Math.PI / 3;
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x + Math.cos(ang) * 30, centre.y + 55, centre.z + Math.sin(ang) * 30, 1, 3, 1, 3, 0.01);
                }
            }
            if (age == genesisAt) {
                Random random = new Random(level.getRandom().nextLong());
                TornadoProfile p = forced == null
                        ? TornadoProfile.roll(random, TornadoFormation.SUPERCELL, StormreachConfig.tornadoIntensityBias / 100.0, TornadoManager.sizeScale(), TornadoManager.durationScale(), TornadoManager.steeringHeadingDeg())
                        : TornadoProfile.rollWith(random, TornadoFormation.SUPERCELL, forced, morph, TornadoManager.sizeScale(), TornadoManager.durationScale(), TornadoManager.steeringHeadingDeg());
                TornadoManager.spawnWithRelations(level, centre, p, random);
            }
        }
    }

    /** A downburst at {@code at}: wet if it is raining there, dry otherwise. */
    public static void downburst(ServerLevel level, Vec3 at) {
        downburst(level, at, level.isRainingAt(BlockPos.containing(at).above(2)));
    }

    public static void downburst(ServerLevel level, Vec3 at, boolean wet) {
        double peak = 60 + level.getRandom().nextDouble() * 70; // most downbursts are EF0–EF1 equivalent; strong ones exceed 100 mph
        TornadoManager.schedule(new Downburst(level, at, 18 + level.getRandom().nextDouble() * 22, peak, wet));
    }

    /** A hailstorm at {@code at} with a random severity (most storms drop small hail). */
    public static void hail(ServerLevel level, Vec3 at) {
        double r = level.getRandom().nextDouble();
        hail(level, at, 0.15 + 0.85 * r * r);
    }

    /** True if a hailstorm is already falling within {@code r} blocks of {@code at}. */
    public static boolean hailNear(ServerLevel level, Vec3 at, double r) {
        for (ActiveEvent e : TornadoManager.events())
            if (e instanceof HailBurst h && h.level == level && Math.hypot(h.centre.x - at.x, h.centre.z - at.z) < r) return true;
        return false;
    }

    /** True if a downburst is happening within {@code r} blocks of {@code at}. */
    public static boolean downburstNear(ServerLevel level, Vec3 at, double r) {
        for (ActiveEvent e : TornadoManager.events())
            if (e instanceof Downburst d && d.level == level && Math.hypot(d.centre.x - at.x, d.centre.z - at.z) < r) return true;
        return false;
    }

    public static void hail(ServerLevel level, Vec3 at, double severity) {
        TornadoManager.schedule(new HailBurst(level, at, severity));
    }

    public static void supercell(ServerLevel level, Vec3 at, com.stormreach.sim.EFScale forced, com.stormreach.sim.TornadoMorphology morph) {
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD && !level.isThundering()) {
            level.getServer().setWeatherParameters(0, 6000, true, true);
        }
        TornadoManager.schedule(new Supercell(level, at, forced, morph));
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(at) < 300 * 300) p.sendSystemMessage(Component.translatable("event.stormreach.supercell"));
        }
    }

    public static void ballLightningSwarm(ServerLevel level, Vec3 at) {
        for (int i = 0; i < 5; i++) {
            var b = ModEntities.BALL_LIGHTNING.create(level, EntitySpawnReason.EVENT);
            if (b == null) continue;
            b.snapTo(at.x + level.getRandom().nextInt(9) - 4, at.y + 2 + level.getRandom().nextInt(3), at.z + level.getRandom().nextInt(9) - 4, 0, 0);
            level.addFreshEntity(b);
        }
    }

    /** Strongest downburst wind acting at a position, for instruments. */
    static double downburstMph(ServerLevel level, Vec3 p) {
        double best = 0;
        for (ActiveEvent e : TornadoManager.events()) {
            if (e instanceof Downburst d && d.level == level) best = Math.max(best, d.mphAt(p));
        }
        return best;
    }

    /**
     * Dispel Weather. Clears the weather itself, not just its effects:
     * tornadoes rope out and lift, Stormreach events (downbursts, hail, supercells) end, storm fronts, squalls and
     * ball lightning break up, and the vanilla rain and thunder stop (rain fades out over a few seconds the normal
     * way, which every client sees because weather is server-side and synced).
     * @return how many things were dispelled (0 means the sky was already clear)
     */
    public static int dispel(ServerLevel level, Player player) {
        int n = 0;
        for (TornadoEntity t : TornadoManager.live(level)) if (!t.isDissipating()) { t.dissipate(); n++; }
        int events = TornadoManager.events().size();
        TornadoManager.events().removeIf(e -> e.level == level);
        n += events - TornadoManager.events().size();
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(256);
        for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, area,
                e -> e instanceof com.stormreach.entity.StormFront || e instanceof com.stormreach.entity.Squall || e instanceof com.stormreach.entity.BallLightning
                        || e instanceof com.stormreach.weather.precip.HailstoneEntity)) {
            if (!(e instanceof com.stormreach.weather.precip.HailstoneEntity)) level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.8, 0.8, 0.8, 0.05);
            e.discard();
            if (!(e instanceof com.stormreach.weather.precip.HailstoneEntity)) n++;
        }
        for (com.stormreach.weather.cloud.StormCellEntity c : level.getEntitiesOfClass(com.stormreach.weather.cloud.StormCellEntity.class, player.getBoundingBox().inflate(600, 400, 600))) {
            c.dissipate();
            n++;
        }
        boolean wasWet = level.isRaining() || level.isThundering();
        if (wasWet) n++;
        // clear sky for 10-20 minutes; Stormreach's Gale and Thunderhead follow the vanilla weather, so they end too
        level.getServer().setWeatherParameters(12000 + level.getRandom().nextInt(12000), 0, false, false);
        // feedback: an expanding ring of clearing cloud, a rising chime, and a message
        for (int ring = 1; ring <= 4; ring++) {
            for (int a = 0; a < 24; a++) {
                double ang = a * Math.PI / 12;
                level.sendParticles(ParticleTypes.CLOUD, player.getX() + Math.cos(ang) * ring * 2.5, player.getY() + 0.6 + ring * 0.4,
                        player.getZ() + Math.sin(ang) * ring * 2.5, 1, 0, 0, 0, 0.02);
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.5, player.getZ(), 40, 0.3, 1.2, 0.3, 0.08);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), com.stormreach.registry.ModSfx.MAGIC_DISPEL,
                net.minecraft.sounds.SoundSource.WEATHER, 1.2f, 1.4f);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(n > 0 ? "event.stormreach.dispel" : "event.stormreach.dispel.clear"));
        return n;
    }
}
