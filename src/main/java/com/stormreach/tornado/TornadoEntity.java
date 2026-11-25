package com.stormreach.tornado;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.sim.DebrisBudget;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.FunnelShape;
import com.stormreach.sim.VortexForces;
import com.stormreach.sim.StructuralIntegrity.MaterialClass;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoMorphology;
import com.stormreach.sim.TornadoProfile;
import com.stormreach.sim.TornadoRelation;
import com.stormreach.sim.TornadoStage;
import com.stormreach.sim.VortexStructure;
import com.stormreach.sim.WindField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * A simulated tornado. All world changes happen on the server; clients get the synced shape data and draw the
 * condensation funnel and debris cloud. See {@link com.stormreach.sim} for the science.
 */
public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Integer> MORPH = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORMATION = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SUBVORTICES = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROTATION = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INTENSITY = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CLOUD_BASE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FUNNEL_REACH = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);

    private TornadoProfile profile;
    private int age;
    private boolean dissipating;
    private double survivedMaxMph;
    private int damagedBlocks;
    private double pathLength;
    private float maxWidth;
    private UUID parentId;
    private double orbitAngle;
    private final DebrisBudget budget = new DebrisBudget(0, 0);
    /** Blocks the tornado has picked up, and where each one is in its ride (spiralling up, then thrown). */
    private final Map<FallingBlockEntity, Flight> debris = new LinkedHashMap<>();
    private int offWaterTicks;
    /** Creative Tornado Director: where this tornado has been told to go (null = its own path). */
    private Vec3 steerTarget;

    private static final class Flight {
        int ticks;
        final int carry;
        final double orbit;
        boolean thrown;
        Flight(int carry, double orbit) { this.carry = carry; this.orbit = orbit; }
    }
    private double headingDeg;
    /** Client only: the funnel's accumulated spin angle (radians), and last tick's, for smooth interpolation. */
    public float clientSpin, clientSpinO;

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(MORPH, 0);
        b.define(FORMATION, 0);
        b.define(SUBVORTICES, 0);
        b.define(ROTATION, 1);
        b.define(WIDTH, 10f);
        b.define(INTENSITY, 0f);
        b.define(CLOUD_BASE, 80f);
        b.define(FUNNEL_REACH, 0f);
    }

    public void setup(TornadoProfile p) {
        this.profile = p;
        this.headingDeg = p.headingDeg();
        entityData.set(MORPH, p.morphology().ordinal());
        entityData.set(FORMATION, p.formation().ordinal());
        entityData.set(SUBVORTICES, p.subvortices());
        entityData.set(ROTATION, p.rotationSign());
        // cloud base: supercell bases are typically 500–1500 m up; scaled down for Minecraft's build height
        float base = switch (p.formation()) {
            case SUPERCELL -> 70f + random.nextFloat() * 50f;
            case LANDSPOUT -> 60f + random.nextFloat() * 30f;
            case WATERSPOUT -> 45f + random.nextFloat() * 25f;
        };
        entityData.set(CLOUD_BASE, base);
    }

    public void setParent(UUID parent, double angle) {
        this.parentId = parent;
        this.orbitAngle = angle;
    }

    public TornadoProfile profile() {
        return profile;
    }

    // ---------------- client-visible state ----------------
    public TornadoMorphology morphology() {
        int i = entityData.get(MORPH);
        return TornadoMorphology.values()[Mth.clamp(i, 0, TornadoMorphology.values().length - 1)];
    }

    public TornadoFormation formation() {
        return TornadoFormation.values()[Mth.clamp(entityData.get(FORMATION), 0, 2)];
    }

    public int subvortices() { return entityData.get(SUBVORTICES); }
    public int rotationSign() { return entityData.get(ROTATION); }
    public float visualWidth() { return entityData.get(WIDTH); }
    public float intensity() { return entityData.get(INTENSITY); }
    public float cloudBase() { return entityData.get(CLOUD_BASE); }
    public float funnelReach() { return entityData.get(FUNNEL_REACH); }

    /** Dispel Weather: the tornado ropes out and lifts over a couple of seconds instead of vanishing. */
    public void dissipate() {
        dissipating = true;
    }

    public boolean isDissipating() { return dissipating; }

    /** Creative Tornado Director: head for this spot and hold there. */
    public void steerTo(Vec3 target) { steerTarget = target; }

    /** Creative Tornado Director: let the tornado follow its own path again. */
    public void release() { steerTarget = null; }

    public Vec3 steerTarget() { return steerTarget; }

    private boolean simulateWithoutPlayers;

    /** GameTests: jump to a point in the tornado's life (0..1). */
    public void skipTo(double lifeFraction) {
        if (profile != null) age = (int) Math.round(profile.lifetimeTicks() * Math.max(0, Math.min(1, lifeFraction)));
    }

    /** GameTests: run the full simulation even with no real player nearby (mock players aren't in the level's list). */
    public void simulateWithoutPlayers() { simulateWithoutPlayers = true; }

    public int damagedBlocks() { return damagedBlocks; }

    public double lifeFraction() {
        return profile == null ? 0 : Math.min(1, age / (double) profile.lifetimeTicks());
    }

    /** Current wind field (server). */
    public WindField windField() {
        if (profile == null) return new WindField(5, 0, 0, 0, 1, 0, 0);
        return profile.windField(lifeFraction());
    }

    /** Ground-relative wind at a world position, m/s components (x, y, z). */
    public Vec3 windAt(Vec3 p) {
        WindField f = windField();
        WindField.Sample s = f.sample(p.x - getX(), p.z - getZ(), Math.max(0, p.y - getY()), (tickCount) / 20.0);
        return new Vec3(s.u(), s.vertical(), s.w());
    }

    public double windMphAt(Vec3 p) {
        Vec3 w = windAt(p);
        return EFScale.msToMph(Math.hypot(w.x, w.z));
    }

    public EFScale estimatedRating() {
        return profile == null ? EFScale.EF0 : EFScale.fromGustMph(profile.peakGustMph() * Math.max(0.01, TornadoStage.intensityAt(lifeFraction())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            TornadoClientEffects.tick(this);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (profile == null) {
            discard();
            return;
        }
        age++;
        if (dissipating) age += Math.max(1, (profile.lifetimeTicks() - age) / 30); // rope out over about 2 seconds
        double f = lifeFraction();
        if (age % 110 == 1) { // the roar: louder (heard farther) for stronger tornadoes; the clip is 5.5 s and loops seamlessly
            level.playSound(null, getX(), getY() + 4, getZ(), com.stormreach.registry.ModSounds.TORNADO_ROAR, net.minecraft.sounds.SoundSource.WEATHER,
                    2.5f + 0.5f * profile.potentialRating().ordinal(), 0.85f + 0.05f * (float) getRandom().nextGaussian());
        }
        float width = (float) (profile.width() * TornadoStage.widthAt(f));
        maxWidth = Math.max(maxWidth, width);
        entityData.set(WIDTH, width);
        entityData.set(INTENSITY, (float) TornadoStage.intensityAt(f));
        double reach = FunnelShape.reach(f);
        entityData.set(FUNNEL_REACH, (float) reach);
        if (f >= 1.0) {
            finish(level);
            return;
        }
        move(level);
        Player near = level.getNearestPlayer(getX(), getY(), getZ(), StormreachConfig.tornadoSimulationDistance, false);
        if (near == null && !simulateWithoutPlayers) return; // keep moving and ageing, but skip the expensive parts far from players
        // nothing happens on the ground until the funnel has come down and touched it
        boolean down = FunnelShape.touchedDown(reach);
        if (down) {
            pushEntities(level);
            if (StormreachConfig.tornadoDestruction) damageBlocks(level);
        }
        flyDebris(level, down);
        if (tickCount % 40 == 0) {
            level.playSound(null, blockPosition(), com.stormreach.registry.ModSfx.WEATHER_WIND, SoundSource.WEATHER, 3.0f + intensity() * 4f, 0.4f);
        }
    }

    private void move(ServerLevel level) {
        Vec3 before = position();
        if (parentId != null) {
            Entity parent = level.getEntity(parentId);
            if (!(parent instanceof TornadoEntity p) || p.isRemoved()) {
                parentId = null;
            } else {
                // a satellite tornado orbits its parent at about 1.5–2.5 parent radii
                double r = p.visualWidth() * 0.5 * 2.0 + 15;
                orbitAngle += p.rotationSign() * 0.5 / Math.max(10, r);
                setPos(p.getX() + Math.cos(orbitAngle) * r, groundY(level, p.getX() + Math.cos(orbitAngle) * r, p.getZ() + Math.sin(orbitAngle) * r), p.getZ() + Math.sin(orbitAngle) * r);
                pathLength += position().distanceTo(before);
                return;
            }
        }
        double f = lifeFraction();
        double speed = profile.translationMs() * (f > 0.85 ? 0.6 : 1.0) * 0.05; // m/s to blocks/tick
        if (steerTarget != null) {
            // directed: turn toward the target (a few degrees a tick, so it curves rather than snaps) and stop on it
            double dx = steerTarget.x - getX(), dz = steerTarget.z - getZ(), dist = Math.hypot(dx, dz);
            double want = Math.toDegrees(Math.atan2(dz, dx));
            double err = Mth.wrapDegrees(want - headingDeg);
            double turn = dist < 10 ? 30 : 4; // close in, turn sharply so it doesn't circle the target
            headingDeg += Mth.clamp(err, -turn, turn);
            speed = Math.min(Math.max(speed, 0.4), dist * 0.1); // at least ~8 m/s so a slow tornado still gets there
            speed *= Math.max(0, Math.cos(Math.toRadians(Math.min(90, Math.abs(err))))); // slow down while turning
            if (dist < 0.5) speed = 0;
        } else {
            // meander, and in the decaying stage curve to the left (as occluding tornadoes often do)
            headingDeg += (random.nextDouble() - 0.5) * 0.6;
            if (f > 0.7) headingDeg -= 0.05 * rotationSign();
        }
        if (formation() == TornadoFormation.WATERSPOUT) speed *= 0.6; // waterspouts drift slowly
        double rad = Math.toRadians(headingDeg);
        double nx = getX() + Math.cos(rad) * speed, nz = getZ() + Math.sin(rad) * speed;
        if (formation() == TornadoFormation.WATERSPOUT) {
            // a waterspout stays over open water: steer away from shore, and if it is pushed onto land it dies
            double look = Math.max(6, visualWidth());
            if (!overWater(level, getX() + Math.cos(rad) * look, getZ() + Math.sin(rad) * look)) {
                for (int turn = 20; turn <= 180; turn += 20) {
                    boolean found = false;
                    for (int sign : new int[]{1, -1}) {
                        double h = Math.toRadians(headingDeg + sign * turn);
                        if (overWater(level, getX() + Math.cos(h) * look, getZ() + Math.sin(h) * look)) {
                            headingDeg += sign * Math.min(turn, 6); // turn a few degrees per tick, not instantly
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                rad = Math.toRadians(headingDeg);
                nx = getX() + Math.cos(rad) * speed;
                nz = getZ() + Math.sin(rad) * speed;
            }
            if (!overWater(level, nx, nz)) {
                nx = getX(); nz = getZ(); // hold position rather than go ashore
            }
            if (!overWater(level, getX(), getZ())) {
                if (++offWaterTicks > 40 && !dissipating) dissipate(); // landfall: the waterspout falls apart
            } else offWaterTicks = 0;
        }
        setPos(nx, groundY(level, nx, nz), nz);
        pathLength += position().distanceTo(before);
    }

    /** True if the surface at x/z is water (a lake, river or ocean). */
    public static boolean overWater(ServerLevel level, double x, double z) {
        BlockPos top = BlockPos.containing(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) - 1, z);
        return level.isLoaded(top) && level.getFluidState(top).is(net.minecraft.tags.FluidTags.WATER);
    }

    /** Nearest open water to {@code at} within {@code radius} blocks, or null. */
    public static Vec3 findWater(ServerLevel level, Vec3 at, int radius) {
        if (overWater(level, at.x, at.z)) return at;
        for (int r = 4; r <= radius; r += 4) {
            for (int a = 0; a < 16; a++) {
                double ang = a * Math.PI / 8;
                double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
                if (overWater(level, x, z) && overWater(level, x + 3, z) && overWater(level, x - 3, z) && overWater(level, x, z + 3) && overWater(level, x, z - 3))
                    return new Vec3(x, at.y, z);
            }
        }
        return null;
    }

    private static double groundY(ServerLevel level, double x, double z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return Math.max(level.getMinY() + 1, top);
    }

    private void pushEntities(ServerLevel level) {
        // the pull reaches several core radii out; the wind sampled at each body sets how hard (it falls off with distance)
        double reach = Math.max(24, visualWidth() * 3.5);
        double peak = currentPeakMph();
        double ceiling = VortexForces.ceiling(peak);
        AABB box = new AABB(getX() - reach, getY() - 4, getZ() - reach, getX() + reach, getY() + Math.max(cloudBase(), ceiling + 8), getZ() + reach);
        for (Entity e : level.getEntities(this, box, e -> !(e instanceof TornadoEntity) && !e.isSpectator() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                && !(e instanceof FallingBlockEntity fb && debris.containsKey(fb)) && !e.isPassenger()
                && !(e instanceof com.stormreach.chase.InterceptVehicle iv && iv.isAnchored()))) {
            // sampled at least 10 m up: the EF rating describes the wind there, and the ground-level wind is what hits you
            Vec3 w = windAt(new Vec3(e.getX(), Math.max(e.getY(), getY() + 10), e.getZ()));
            double mph = EFScale.msToMph(Math.hypot(w.x, w.z));
            if (mph < 40) continue;
            double above = e.getY() - groundY(level, e.getX(), e.getZ());
            int armor = e instanceof LivingEntity le ? le.getArmorValue() : 0;
            double mass = VortexForces.mass(e.getBbWidth(), e.getBbHeight(), armor) * com.stormreach.sky.ChaserGear.massFactor(e);
            double dx = e.getX() - getX(), dz = e.getZ() - getZ(), r = Math.max(0.01, Math.hypot(dx, dz));
            Vec3 v = e.getDeltaMovement();
            double[] nv = VortexForces.step(new double[]{v.x, v.y, v.z}, new double[]{w.x * 0.05, w.y * 0.05 * 0.3, w.z * 0.05}, mph, mass,
                    e.onGround(), e instanceof Player p && p.isShiftKeyDown(), above, ceiling, new double[]{dx / r, dz / r});
            e.setDeltaMovement(nv[0], nv[1], nv[2]);
            e.needsSync = true;
            if (nv[1] > 0.05) e.resetFallDistance(); // fall damage counts from where they're dropped, not from the ground
            if (e instanceof LivingEntity le && tickCount % 10 == 0) {
                double dps = VortexForces.windDamagePerSecond(mph);
                if (dps > 0) le.hurtServer(level, level.damageSources().flyIntoWall(), (float) (dps * 0.5) * (carriesBadge(le) ? 0.5f : 1f));
            }
            if (e instanceof ServerPlayer sp && mph > 65 && tickCount % 200 == 0) {
                com.stormreach.progress.StormAdvancements.award(sp, "inside_tornado");
            }
        }
    }

    /** The strongest ground-relative wind the tornado can produce right now, mph. */
    public double currentPeakMph() {
        return profile == null ? 0 : profile.peakGustMph() * Math.max(0.01, TornadoStage.intensityAt(lifeFraction()));
    }

    private void damageBlocks(ServerLevel level) {
        budget.newTick();
        int checks = VortexForces.blockChecks(StormreachConfig.tornadoBlockChecksPerTick, currentPeakMph(), visualWidth());
        boolean water = formation() == TornadoFormation.WATERSPOUT;
        int limit = StormreachConfig.tornadoDebrisLimit;
        double radius = Math.max(6, visualWidth() * 1.4 + 4);
        RandomSource rnd = level.getRandom();
        WindField field = windField();
        for (int i = 0; i < checks; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rnd.nextDouble()) * radius;
            int x = Mth.floor(getX() + Math.cos(a) * r), z = Mth.floor(getZ() + Math.sin(a) * r);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            // the exposed top block first (roofs, treetops); violent winds also tear into the walls below it
            int y = top;
            if (currentPeakMph() > 135 && rnd.nextInt(3) == 0) y = top - 1 - rnd.nextInt(Math.max(1, Math.min(24, top - (int) getY() + 3)));
            else if (rnd.nextInt(5) == 0) y = top - rnd.nextInt(Math.max(1, Math.min(6, top - (int) getY() + 3)));
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) continue;
            WindField.Sample sample = field.sample(x + 0.5 - getX(), z + 0.5 - getZ(), Math.max(0, y - getY()), tickCount / 20.0);
            double mph = sample.horizontalMph();
            if (mph < 45) continue;
            if (!s.getFluidState().isEmpty() && s.getFluidState().isSource()) continue; // water itself is not debris
            MaterialClass m = BlockWind.classify(level, pos, s);
            if (water && m == MaterialClass.LOOSE_EARTH) continue; // a waterspout doesn't scour the lakebed
            if (m == MaterialClass.UNBREAKABLE) continue;
            double threshold = BlockWind.failureMph(level, pos, s, m);
            if (mph < threshold) continue;
            survivedMaxMph = Math.max(survivedMaxMph, mph);
            damagedBlocks++;
            fail(level, pos, s, m, sample, limit);
        }
    }

    private void fail(ServerLevel level, BlockPos pos, BlockState s, MaterialClass m, WindField.Sample sample, int limit) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.2);
        if (m == MaterialClass.LOOSE_EARTH) {
            // ground scouring: strip turf to dirt, then to coarse dirt; soil isn't carried off whole
            if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM)) level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            else if (s.is(Blocks.DIRT)) level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
            return;
        }
        if (m == MaterialClass.FOLIAGE) {
            level.destroyBlock(pos, level.getRandom().nextInt(4) == 0);
            return;
        }
        if (m.liftable && debris.size() < limit && !s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                && !s.hasBlockEntity()) {
            FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, s);
            fb.dropItem = true;
            fb.setHurtsEntities(2.0f, 20);
            Vec3 w = new Vec3(sample.u(), 0, sample.w()).scale(0.05 * 0.5);
            fb.setDeltaMovement(w.x, 0.4 + Math.min(0.8, sample.vertical() * 0.02), w.z);
            double core = Math.max(2, visualWidth() * 0.5);
            debris.put(fb, new Flight(VortexForces.carryTicks(currentPeakMph(), level.getRandom().nextDouble()), core * (0.7 + 0.6 * level.getRandom().nextDouble())));
        } else {
            level.destroyBlock(pos, true);
        }
    }

    /** Picked-up blocks spiral up around the core, then are thrown out; flying debris hurts what it hits. */
    private void flyDebris(ServerLevel level, boolean down) {
        double peak = currentPeakMph();
        double ceiling = Math.max(6, VortexForces.ceiling(peak) * 0.9);
        Vec3 drift = new Vec3(getX() - xo, 0, getZ() - zo);
        for (Iterator<Map.Entry<FallingBlockEntity, Flight>> it = debris.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<FallingBlockEntity, Flight> en = it.next();
            FallingBlockEntity d = en.getKey();
            Flight fl = en.getValue();
            if (d.isRemoved()) { it.remove(); continue; }
            fl.ticks++;
            double dx = d.getX() - getX(), dz = d.getZ() - getZ();
            Vec3 w = windAt(d.position());
            double tangential = Math.hypot(w.x, w.z) * 0.05 * 0.6;
            if (!fl.thrown) {
                double above = d.getY() - getY();
                if (!down || fl.ticks > fl.carry || above > ceiling) {
                    fl.thrown = true;
                    double[] v = VortexForces.eject(dx, dz, tangential, rotationSign());
                    d.setDeltaMovement(v[0] + drift.x, v[1], v[2] + drift.z);
                } else {
                    double rise = 0.08 + 0.22 * intensity();
                    double[] v = VortexForces.carry(dx, dz, above, fl.orbit, tangential, rise, ceiling, rotationSign(), 0.04);
                    d.setDeltaMovement(v[0] + drift.x, v[1], v[2] + drift.z);
                }
                d.needsSync = true;
            }
            // impacts: anything flying fast enough hurts what it hits
            Vec3 dv = d.getDeltaMovement();
            double speed = dv.length();
            if (speed > 0.7 && fl.ticks % 2 == 0) {
                for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, d.getBoundingBox().inflate(0.4),
                        le -> !(le instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                    float dmg = (float) Math.min(14, speed * 4) * (carriesBadge(le) ? 0.5f : 1f);
                    le.hurtServer(level, level.damageSources().fallingBlock(d), dmg);
                }
            }
        }
        budget.setAirborne(debris.size());
    }

    private void finish(ServerLevel level) {
        EFScale survey = EFScale.fromGustMph(survivedMaxMph);
        String rating = survey == null ? "EF-U (no damage found)" : survey.name();
        Component msg = Component.translatable("event.stormreach.tornado.survey", rating,
                        Math.round(survivedMaxMph), Math.round(profile.peakGustMph()), damagedBlocks,
                        String.format("%.1f", pathLength / 1000.0), Math.round(maxWidth),
                        Component.translatable("tornado.stormreach.morph." + profile.morphology().name().toLowerCase()))
                .withStyle(ChatFormatting.GRAY);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 768 * 768) p.sendSystemMessage(msg);
            if (p.distanceToSqr(this) < 300 * 300 && survey != null) {
                com.stormreach.progress.StormAdvancements.award(p, "tornado_survey");
                if (survey.ordinal() >= EFScale.EF4.ordinal()) com.stormreach.progress.StormAdvancements.award(p, "violent_tornado");
                if (survey == EFScale.EF5) com.stormreach.progress.StormAdvancements.award(p, "ef5_tornado");
            }
        }
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 1024 * 1024;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        if (profile == null) return;
        out.putInt("Age", age);
        out.putInt("Formation", profile.formation().ordinal());
        out.putInt("Rating", profile.potentialRating().ordinal());
        out.putInt("Morph", profile.morphology().ordinal());
        out.putInt("Subs", profile.subvortices());
        out.putInt("Life", profile.lifetimeTicks());
        out.putInt("Rot", profile.rotationSign());
        out.putInt("Gust10", (int) Math.round(profile.peakGustMph() * 10));
        out.putInt("Width10", (int) Math.round(profile.width() * 10));
        out.putInt("Trans10", (int) Math.round(profile.translationMs() * 10));
        out.putInt("Heading", (int) Math.round(headingDeg));
        out.putInt("Survived", (int) Math.round(survivedMaxMph));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        int life = in.getIntOr("Life", 0);
        if (life <= 0) return;
        TornadoProfile p = new TornadoProfile(TornadoFormation.values()[Mth.clamp(in.getIntOr("Formation", 0), 0, 2)],
                EFScale.values()[Mth.clamp(in.getIntOr("Rating", 0), 0, 5)], in.getIntOr("Gust10", 700) / 10.0,
                TornadoMorphology.values()[Mth.clamp(in.getIntOr("Morph", 0), 0, TornadoMorphology.values().length - 1)],
                in.getIntOr("Subs", 0) > 0 ? VortexStructure.MULTIPLE_VORTEX : VortexStructure.SINGLE, in.getIntOr("Subs", 0),
                in.getIntOr("Width10", 200) / 10.0, in.getIntOr("Trans10", 100) / 10.0, in.getIntOr("Heading", 0),
                in.getIntOr("Rot", 1), life, TornadoRelation.NONE);
        setup(p);
        age = in.getIntOr("Age", 0);
        survivedMaxMph = in.getIntOr("Survived", 0);
    }

    /** Creates and adds a tornado. Returns null if the active-tornado cap is reached. */
    public static TornadoEntity spawn(ServerLevel level, Vec3 at, TornadoProfile p) {
        if (TornadoManager.active(level) >= Math.max(1, StormreachConfig.maxActiveTornadoes) && StormreachConfig.maxActiveTornadoes > 0) {
            return null;
        }
        if (p.formation() == TornadoFormation.WATERSPOUT) {
            at = findWater(level, at, 48); // waterspouts only form over open water
            if (at == null) return null;
        }
        TornadoEntity t = TornadoTypes.TORNADO.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (t == null) return null;
        t.setup(p);
        t.setPos(at.x, groundY(level, at.x, at.z), at.z);
        level.addFreshEntity(t);
        TornadoManager.track(t);
        Stormreach.LOGGER.debug("Tornado spawned: {} {} {} {} mph", p.formation(), p.morphology(), p.potentialRating(), Math.round(p.peakGustMph()));
        return t;
    }

    public static Random javaRandom(RandomSource r) {
        return new Random(r.nextLong());
    }

    /** The Chaser's Badge (achievement reward) halves debris and wind damage from tornadoes. */
    static boolean carriesBadge(net.minecraft.world.entity.LivingEntity le) {
        if (!(le instanceof net.minecraft.world.entity.player.Player p)) return false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(com.stormreach.story.StoryRegistry.CHASERS_BADGE)) return true;
        return false;
    }
}
