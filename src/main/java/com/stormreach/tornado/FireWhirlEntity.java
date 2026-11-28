package com.stormreach.tornado;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0: a fire whirl, the Nether's own rare "tornado": a spinning column of flame that rises off a lava sea or a
 * burning soul sand valley, wanders slowly over the ground, sets alight whatever it touches (fire resistance and fire
 * immunity protect as they do against any fire), lifts small things, and leaves fire behind on netherrack and soul
 * soil. It never forms outside the Nether. Server-authoritative; the client only draws it and throws embers.
 */
public class FireWhirlEntity extends Entity {
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(FireWhirlEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(FireWhirlEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STRENGTH = SynchedEntityData.defineId(FireWhirlEntity.class, EntityDataSerializers.FLOAT);

    private int life = 900, age;
    private double heading;
    /** Client: accumulated spin for the renderer. */
    public float spin, spinO;

    public FireWhirlEntity(EntityType<? extends FireWhirlEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(WIDTH, 4f);
        b.define(HEIGHT, 18f);
        b.define(STRENGTH, 0f);
    }

    public float whirlWidth() { return entityData.get(WIDTH); }
    public float whirlHeight() { return entityData.get(HEIGHT); }
    public float strength() { return entityData.get(STRENGTH); }

    /** Spawns one at {@code at} (Nether only). */
    public static FireWhirlEntity spawn(ServerLevel level, Vec3 at) {
        if (level.dimension() != Level.NETHER) return null;
        FireWhirlEntity w = TornadoTypes.FIRE_WHIRL.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (w == null) return null;
        var r = level.getRandom();
        w.entityData.set(WIDTH, 3f + r.nextFloat() * 4f);
        w.entityData.set(HEIGHT, 14f + r.nextFloat() * 16f);
        w.life = 600 + r.nextInt(900);
        w.heading = r.nextDouble() * Math.PI * 2;
        Double g = surface(level, at.x, at.y, at.z);
        w.setPos(at.x, g == null ? at.y : g, at.z);
        level.addFreshEntity(w);
        return w;
    }

    /** The floor under (x, z) near height y: the Nether has a roof, so no heightmap; search a few blocks up and down. */
    static Double surface(ServerLevel level, double x, double y, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z), by = Mth.floor(y);
        for (int d = 0; d <= 8; d++) {
            for (int s : new int[]{-1, 1}) {
                int yy = by + s * d;
                BlockPos p = new BlockPos(bx, yy, bz);
                if (!level.isLoaded(p)) return null;
                BlockState below = level.getBlockState(p.below()), here = level.getBlockState(p);
                if ((!below.isAir() || !below.getFluidState().isEmpty()) && (here.isAir() || here.getBlock() instanceof BaseFireBlock)) return (double) yy;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spinO = spin;
            spin += 0.45f;
            if (random.nextFloat() < 0.8f) {
                double a = random.nextDouble() * Math.PI * 2, r = whirlWidth() * 0.5 * random.nextDouble(), h = random.nextDouble() * whirlHeight() * strength();
                level().addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.LAVA, getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r,
                        -Math.sin(a) * 0.15, 0.05, Math.cos(a) * 0.15);
            }
            if (random.nextFloat() < 0.4f) level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + whirlHeight() * strength(), getZ(), 0, 0.12, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (level.dimension() != Level.NETHER) { discard(); return; }   // a Nether phenomenon only, even if summoned elsewhere
        age++;
        float f = age / (float) life;
        float str = f < 0.1f ? f / 0.1f : f > 0.85f ? Math.max(0, (1 - f) / 0.15f) : 1f;
        entityData.set(STRENGTH, str);
        if (age >= life) { discard(); return; }
        if (age % 70 == 1) level.playSound(null, getX(), getY() + 3, getZ(), ModSfx.FIRE_WHIRL_ROAR, SoundSource.WEATHER, 3.0f, 0.9f + random.nextFloat() * 0.2f);
        // wander slowly, turning a little, following the ground
        heading += (random.nextDouble() - 0.5) * 0.12;
        double speed = 0.06;
        double nx = getX() + Math.cos(heading) * speed, nz = getZ() + Math.sin(heading) * speed;
        Double g = surface(level, nx, getY(), nz);
        if (g == null) { heading += Math.PI * 0.5; return; }   // a drop or a wall ahead: turn away
        setPos(nx, g, nz);
        if (str < 0.3f) return;
        double r = whirlWidth() * 0.6;
        // what the flames touch burns (fire resistance and fire-immune mobs shrug it off as usual)
        AABB box = new AABB(getX() - r * 2, getY() - 1, getZ() - r * 2, getX() + r * 2, getY() + whirlHeight(), getZ() + r * 2);
        for (Entity e : level.getEntities(this, box, e -> !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ(), d = Math.max(0.01, Math.hypot(dx, dz));
            if (d < r) {
                e.igniteForSeconds(6);
                if (e instanceof LivingEntity le && !le.hasEffect(MobEffects.FIRE_RESISTANCE) && !le.fireImmune() && age % 10 == 0)
                    le.hurtServer(level, level.damageSources().inFire(), 2.0f * str);
            }
            if (d < r * 2) {
                // swirl and a gentle lift: enough to toss items and small mobs, not to carry a player away
                double tx = dz / d, tz = -dx / d;
                Vec3 v = e.getDeltaMovement();
                double lift = e.getBbHeight() < 1.0 ? 0.09 : 0.03;
                e.setDeltaMovement(v.x + (tx * 0.35 - dx / d * 0.08 - v.x) * 0.15, Math.min(0.5, v.y + lift * str), v.z + (tz * 0.35 - dz / d * 0.08 - v.z) * 0.15);
                e.needsSync = true;
            }
        }
        // it leaves fire on the ground it crosses
        if (age % 5 == 0) {
            double a = random.nextDouble() * Math.PI * 2, rr = random.nextDouble() * r;
            BlockPos p = BlockPos.containing(getX() + Math.cos(a) * rr, getY(), getZ() + Math.sin(a) * rr);
            if (level.isLoaded(p) && level.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(level, p, net.minecraft.core.Direction.UP))
                level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
        }
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 256 * 256; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("Age", age); out.putInt("Life", life); out.putFloat("Width", whirlWidth()); out.putFloat("Height", whirlHeight());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        age = in.getIntOr("Age", 0); life = in.getIntOr("Life", 900);
        entityData.set(WIDTH, in.getFloatOr("Width", 4f)); entityData.set(HEIGHT, in.getFloatOr("Height", 18f));
    }

    // ------------------------------------------------------------------ natural spawning

    /** Called once a second per level: rarely, near a player in the Nether, over lava or burning ground. */
    public static void naturalTick(ServerLevel level) {
        if (!StormreachConfig.fireWhirls || level.dimension() != Level.NETHER || level.getGameTime() % 20 != 0) return;
        if (!level.getEntities(TornadoTypes.FIRE_WHIRL, e -> true).isEmpty()) return;
        var r = level.getRandom();
        for (ServerPlayer p : level.players()) {
            if (r.nextInt(3000) != 0) continue;   // about one every 50 minutes spent in the Nether
            for (int tries = 0; tries < 12; tries++) {
                double a = r.nextDouble() * Math.PI * 2, d = 24 + r.nextDouble() * 40;
                double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
                Double g = surface(level, x, p.getY(), z);
                if (g == null) continue;
                BlockState under = level.getBlockState(BlockPos.containing(x, g - 1, z));
                if (under.is(Blocks.LAVA) || under.is(Blocks.MAGMA_BLOCK) || under.is(Blocks.SOUL_SAND) || under.is(Blocks.SOUL_SOIL) || under.is(Blocks.NETHERRACK)) {
                    spawn(level, new Vec3(x, g, z));
                    Stormreach.LOGGER.debug("Fire whirl formed near {}", p.getName().getString());
                    return;
                }
            }
        }
    }
}
