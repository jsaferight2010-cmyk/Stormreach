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
     * Downburst: a column of rain-cooled air slams into the ground and spreads out in all directions. Microbursts
     * (under 4 km across) can reach EF1–EF2-equivalent speeds; the damage pattern is divergent, not rotational.
     */
    public static final class Downburst extends ActiveEvent {
        final double radius;
        final double peakMph;

        Downburst(ServerLevel level, Vec3 centre, double radius, double peakMph) {
            super(level, centre, 140);
            this.radius = radius;
            this.peakMph = peakMph;
        }

        double mphAt(Vec3 p) {
            double r = Math.hypot(p.x - centre.x, p.z - centre.z);
            double rm = radius * 0.6;
            double shape = r < rm ? r / rm : Math.exp(-(r - rm) / (radius * 0.5));
            double life = age < 30 ? age / 30.0 : age > 100 ? Math.max(0, (duration - age) / 40.0) : 1.0;
            return peakMph * shape * life;
        }

        @Override
        void run() {
            if (age % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2, d = level.getRandom().nextDouble() * radius;
                    level.sendParticles(ParticleTypes.CLOUD, centre.x + Math.cos(a) * d, centre.y + 1, centre.z + Math.sin(a) * d, 1, Math.cos(a) * 0.5, 0, Math.sin(a) * 0.5, 0.1);
                }
            }
            AABB box = new AABB(centre.x - radius * 2, centre.y - 6, centre.z - radius * 2, centre.x + radius * 2, centre.y + 30, centre.z + radius * 2);
            for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof TornadoEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                double mph = mphAt(e.position());
                if (mph < 35) continue;
                Vec3 out = new Vec3(e.getX() - centre.x, 0, e.getZ() - centre.z).normalize().scale(EFScale.mphToMs(mph) * 0.05 * 0.06);
                e.setDeltaMovement(e.getDeltaMovement().add(out));
                e.needsSync = true;
            }
            if (StormreachConfig.tornadoDestruction && age > 20 && age < 110) {
                var r = level.getRandom();
                for (int i = 0; i < Math.min(48, StormreachConfig.tornadoBlockChecksPerTick / 2); i++) {
                    double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius * 1.6;
                    int x = Mth.floor(centre.x + Math.cos(a) * d), z = Mth.floor(centre.z + Math.sin(a) * d);
                    BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                    BlockState s = level.getBlockState(pos);
                    MaterialClass m = BlockWind.classify(level, pos, s);
                    if (m == MaterialClass.UNBREAKABLE || m == MaterialClass.LOOSE_EARTH) continue;
                    if (mphAt(Vec3.atCenterOf(pos)) >= BlockWind.failureMph(level, pos, s, m)) level.destroyBlock(pos, m != MaterialClass.FOLIAGE);
                }
            }
        }
    }

    /** Hail burst: falling hailstones damage anyone without a helmet and smash glass panes and crops. */
    public static final class HailBurst extends ActiveEvent {
        final double radius = 28;

        HailBurst(ServerLevel level, Vec3 centre) {
            super(level, centre, 1200);
        }

        @Override
        void run() {
            var r = level.getRandom();
            for (int i = 0; i < 10; i++) {
                double x = centre.x + (r.nextDouble() - 0.5) * radius * 2, z = centre.z + (r.nextDouble() - 0.5) * radius * 2;
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, centre.y + 18, z, 2, 0.2, 0.2, 0.2, 0.0);
            }
            if (age % 20 != 0) return;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(centre)).inflate(radius, 40, radius))) {
                if (!level.canSeeSky(e.blockPosition().above()) || (e instanceof Player p && (p.isCreative() || p.isSpectator()))) continue;
                if (e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) e.hurtServer(level, level.damageSources().source(StormWeather.HAIL), 1.0f);
            }
            for (int i = 0; i < 4; i++) {
                int x = Mth.floor(centre.x + (r.nextDouble() - 0.5) * radius * 2), z = Mth.floor(centre.z + (r.nextDouble() - 0.5) * radius * 2);
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                BlockState s = level.getBlockState(pos);
                if (s.is(net.minecraft.tags.BlockTags.CROPS) || s.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS && !s.isSolidRender()) {
                    level.destroyBlock(pos, false);
                }
            }
            if (age % 100 == 0) level.playSound(null, BlockPos.containing(centre), SoundEvents.SNOW_BREAK, SoundSource.WEATHER, 2f, 0.5f);
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

    public static void downburst(ServerLevel level, Vec3 at) {
        double peak = 60 + level.getRandom().nextDouble() * 70; // most downbursts are EF0–EF1 equivalent; strong ones exceed 100 mph
        TornadoManager.schedule(new Downburst(level, at, 18 + level.getRandom().nextDouble() * 22, peak));
        level.playSound(null, BlockPos.containing(at), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.WEATHER, 4f, 0.4f);
    }

    public static void hail(ServerLevel level, Vec3 at) {
        TornadoManager.schedule(new HailBurst(level, at));
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
                e -> e instanceof com.stormreach.entity.StormFront || e instanceof com.stormreach.entity.Squall || e instanceof com.stormreach.entity.BallLightning)) {
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.8, 0.8, 0.8, 0.05);
            e.discard();
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
        level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                net.minecraft.sounds.SoundSource.WEATHER, 1.2f, 1.4f);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(n > 0 ? "event.stormreach.dispel" : "event.stormreach.dispel.clear"));
        return n;
    }
}
