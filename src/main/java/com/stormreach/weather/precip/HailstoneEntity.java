package com.stormreach.weather.precip;

import com.stormreach.sim.HailSize;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One falling hailstone. It falls at its size's terminal speed (drifting a little with the wind), hurts whatever it
 * hits by its size, smashes crops, panes, glass and leaves when it's big enough, and settles as hail on the ground.
 */
public class HailstoneEntity extends Entity {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(HailstoneEntity.class, EntityDataSerializers.INT);
    /** Live stones per level, so storms can't flood the server with entities. */
    private static final Set<HailstoneEntity> LIVE = Collections.newSetFromMap(new WeakHashMap<>());
    public static final int MAX_LIVE = 600;

    private double driftX, driftZ;
    /** Client only: spin for the renderer. */
    public float spin, spinO;

    public HailstoneEntity(EntityType<? extends HailstoneEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SIZE, 0);
    }

    public HailSize size() {
        return HailSize.values()[Mth.clamp(entityData.get(SIZE), 0, HailSize.values().length - 1)];
    }

    public static int live() {
        LIVE.removeIf(Entity::isRemoved);
        return LIVE.size();
    }

    /** Drops a stone of this size from {@code at}, drifting with the given wind (blocks/tick). Null when at the cap. */
    public static HailstoneEntity drop(ServerLevel level, Vec3 at, HailSize size, double windX, double windZ) {
        if (live() >= MAX_LIVE) return null;
        HailstoneEntity h = PrecipContent.HAILSTONE.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (h == null) return null;
        h.entityData.set(SIZE, size.ordinal());
        h.driftX = windX;
        h.driftZ = windZ;
        h.setPos(at);
        h.setDeltaMovement(windX, -size.terminalMs() * 0.05 * 0.6, windZ);
        level.addFreshEntity(h);
        LIVE.add(h);
        return h;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spinO = spin;
            spin += 0.35f;
            return;
        }
        if (tickCount > 240) { discard(); return; }
        HailSize size = size();
        double vt = size.terminalMs() * 0.05;
        Vec3 v = getDeltaMovement();
        // accelerate to terminal speed; the wind carries it sideways
        v = new Vec3(v.x + (driftX - v.x) * 0.1, Math.max(-vt, v.y - 0.08), v.z + (driftZ - v.z) * 0.1);
        setDeltaMovement(v);
        Vec3 from = position(), to = from.add(v);
        ServerLevel level = (ServerLevel) level();
        // anything alive in the path?
        AABB sweep = new AABB(from, to).inflate(0.3);
        for (Entity e : level.getEntities(this, sweep, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
            if (e instanceof Player p && p.isCreative()) continue;
            if (e.getBoundingBox().inflate(0.15).clip(from, to).isPresent()) {
                hitEntity(level, (LivingEntity) e, size);
                return;
            }
        }
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            hitBlock(level, hit, size);
            return;
        }
        setPos(to);
        if (getY() < level.getMinY()) discard();
    }

    private void hitEntity(ServerLevel level, LivingEntity e, HailSize size) {
        DamageSource src = level.damageSources().source(StormWeather.HAIL);
        e.hurtServer(level, src, size.damage());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), getX(), getY(), getZ(), 4 + size.ordinal() * 2, 0.1, 0.1, 0.1, 0.1);
        discard();
    }

    private void hitBlock(ServerLevel level, BlockHitResult hit, HailSize size) {
        BlockPos pos = hit.getBlockPos();
        BlockState s = level.getBlockState(pos);
        Vec3 at = hit.getLocation();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), at.x, at.y + 0.05, at.z,
                3 + size.ordinal() * 3, 0.1, 0.05, 0.1, 0.12);
        if (!s.getFluidState().isEmpty()) {
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.1, at.z, 2 + size.ordinal() * 2, 0.1, 0, 0.1, 0.1);
            discard();
            return;
        }
        boolean broke = false;
        if (size.breaksCrops() && (s.is(BlockTags.CROPS) || s.is(BlockTags.FLOWERS) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.FERN))) {
            level.destroyBlock(pos, size.ordinal() < 4); // big hail pulps the crop
            broke = true;
        } else if (size.breaksPanes() && s.getSoundType() == SoundType.GLASS && !s.isSolidRender()) {
            level.destroyBlock(pos, false);
            broke = true;
        } else if (size.breaksGlassBlocks() && s.getSoundType() == SoundType.GLASS) {
            level.destroyBlock(pos, false);
            broke = true;
        } else if (size.shredsLeaves() && s.is(BlockTags.LEAVES) && random.nextInt(3) == 0) {
            level.destroyBlock(pos, false);
            broke = true;
        }
        float pitch = 1.8f - size.ordinal() * 0.22f;
        level.playSound(null, pos, broke ? com.stormreach.registry.ModSfx.MAGIC_SHATTER : com.stormreach.registry.ModSfx.BLOCK_STONE_TAP, SoundSource.WEATHER, 0.25f + size.ordinal() * 0.15f, pitch);
        if (!broke && hit.getDirection() == Direction.UP && random.nextDouble() < size.accumulation()) {
            HailLayerBlock.accumulate(level, pos.above(), size);
        }
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 96 * 96; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) { out.putInt("Size", entityData.get(SIZE)); }

    @Override
    protected void readAdditionalSaveData(ValueInput in) { entityData.set(SIZE, in.getIntOr("Size", 0)); }
}
