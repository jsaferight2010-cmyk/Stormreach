package com.stormreach.tornado;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.sim.DebrisBudget;
import com.stormreach.sim.EFScale;
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

import java.util.ArrayList;
import java.util.List;
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
    private final List<FallingBlockEntity> debris = new ArrayList<>();
    private double headingDeg;

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
        entityData.set(FUNNEL_REACH, (float) Mth.clamp(f / 0.2, 0.15, 1.0) * (f > 0.95 ? (float) ((1 - f) / 0.05) : 1f));
        if (f >= 1.0) {
            finish(level);
            return;
        }
        move(level);
        Player near = level.getNearestPlayer(getX(), getY(), getZ(), StormreachConfig.tornadoSimulationDistance, false);
        if (near == null) return; // keep moving and ageing, but skip the expensive parts far from players
        if (tickCount % 2 == 0) pushEntities(level);
        if (StormreachConfig.tornadoDestruction) damageBlocks(level);
        debris.removeIf(e -> e.isRemoved());
        budget.setAirborne(debris.size());
        for (FallingBlockEntity d : debris) {
            if (d.tickCount < 60) {
                Vec3 w = windAt(d.position()).scale(0.05); // m/s to blocks per tick
                d.setDeltaMovement(d.getDeltaMovement().scale(0.8).add(w.scale(0.2)).add(0, 0.03, 0));
            }
        }
        if (tickCount % 40 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 3.0f + intensity() * 4f, 0.4f);
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
        // meander, and in the decaying stage curve to the left (as occluding tornadoes often do)
        headingDeg += (random.nextDouble() - 0.5) * 0.6;
        if (f > 0.7) headingDeg -= 0.05 * rotationSign();
        double speed = profile.translationMs() * (f > 0.85 ? 0.6 : 1.0) * 0.05; // m/s to blocks/tick
        double rad = Math.toRadians(headingDeg);
        double nx = getX() + Math.cos(rad) * speed, nz = getZ() + Math.sin(rad) * speed;
        setPos(nx, groundY(level, nx, nz), nz);
        pathLength += position().distanceTo(before);
    }

    private static double groundY(ServerLevel level, double x, double z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return Math.max(level.getMinY() + 1, top);
    }

    private void pushEntities(ServerLevel level) {
        double reach = Math.max(12, visualWidth() * 1.8);
        AABB box = new AABB(getX() - reach, getY() - 4, getZ() - reach, getX() + reach, getY() + cloudBase(), getZ() + reach);
        for (Entity e : level.getEntities(this, box, e -> !(e instanceof TornadoEntity) && !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
            Vec3 w = windAt(e.position());
            double mph = EFScale.msToMph(Math.hypot(w.x, w.z));
            if (mph < 40) continue;
            // drag pulls the entity toward the wind's velocity; heavier (armored) players resist slightly
            double k = e instanceof LivingEntity le ? 0.04 / (1 + le.getArmorValue() * 0.02) : 0.06;
            Vec3 target = w.scale(0.05);
            Vec3 v = e.getDeltaMovement();
            Vec3 dv = target.subtract(v).scale(k);
            double lift = mph > 95 ? Math.min(0.12, (mph - 95) / 900.0) : 0;
            e.setDeltaMovement(v.add(dv.x, dv.y * 0.5 + lift, dv.z));
            e.needsSync = true;
            if (e instanceof LivingEntity le && mph > 110 && tickCount % 20 == 0) {
                le.hurtServer(level, level.damageSources().flyIntoWall(), (float) ((mph - 110) / 25.0) * (carriesBadge(le) ? 0.5f : 1f));
            }
            if (e instanceof ServerPlayer sp && mph > 65 && tickCount % 200 == 0) {
                com.stormreach.progress.StormAdvancements.award(sp, "inside_tornado");
            }
        }
    }

    private void damageBlocks(ServerLevel level) {
        budget.newTick();
        int checks = StormreachConfig.tornadoBlockChecksPerTick;
        int limit = StormreachConfig.tornadoDebrisLimit;
        double radius = Math.max(6, visualWidth() * 1.4 + 4);
        RandomSource rnd = level.getRandom();
        WindField field = windField();
        for (int i = 0; i < checks; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rnd.nextDouble()) * radius;
            int x = Mth.floor(getX() + Math.cos(a) * r), z = Mth.floor(getZ() + Math.sin(a) * r);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            int y = rnd.nextInt(4) == 0 ? top - rnd.nextInt(Math.max(1, Math.min(20, top - (int) getY() + 3))) : top;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) continue;
            WindField.Sample sample = field.sample(x + 0.5 - getX(), z + 0.5 - getZ(), Math.max(0, y - getY()), tickCount / 20.0);
            double mph = sample.horizontalMph();
            if (mph < 45) continue;
            MaterialClass m = BlockWind.classify(level, pos, s);
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
        if (m.liftable && debris.size() < limit && level.getBlockState(pos).getBlock().defaultBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == false) {
            FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, s);
            fb.dropItem = true;
            fb.setHurtsEntities(1.5f, 12);
            Vec3 w = new Vec3(sample.u(), 0, sample.w()).scale(0.05 * 0.5);
            fb.setDeltaMovement(w.x, 0.4 + Math.min(0.8, sample.vertical() * 0.02), w.z);
            debris.add(fb);
        } else {
            level.destroyBlock(pos, true);
        }
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
