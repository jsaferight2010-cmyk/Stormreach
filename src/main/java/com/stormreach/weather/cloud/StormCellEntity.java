package com.stormreach.weather.cloud;

import com.stormreach.sim.CloudGrowth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A thunderstorm cloud you can watch develop: it starts as a low cumulus, towers up, spreads an anvil at the top,
 * then slumps and fades ({@link CloudGrowth}). The entity sits at the cloud base; the client draws the cloud from
 * the synced growth, size and height. It drifts with the steering wind and never touches the world.
 */
public class StormCellEntity extends Entity {
    private static final EntityDataAccessor<Float> GROWTH = SynchedEntityData.defineId(StormCellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(StormCellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DEPTH = SynchedEntityData.defineId(StormCellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SEVERE = SynchedEntityData.defineId(StormCellEntity.class, EntityDataSerializers.BOOLEAN);

    private int age;
    private int lifetime = 9600;
    private boolean dissipating;
    private double driftX, driftZ;

    public StormCellEntity(EntityType<? extends StormCellEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(GROWTH, 0f);
        b.define(WIDTH, 60f);
        b.define(DEPTH, 120f);
        b.define(SEVERE, false);
    }

    public void setup(float width, float depth, boolean severe, int lifetime, double driftX, double driftZ) {
        entityData.set(WIDTH, width);
        entityData.set(DEPTH, depth);
        entityData.set(SEVERE, severe);
        this.lifetime = lifetime;
        this.driftX = driftX;
        this.driftZ = driftZ;
    }

    public float growth() { return entityData.get(GROWTH); }
    public float cloudWidth() { return entityData.get(WIDTH); }
    public float cloudDepth() { return entityData.get(DEPTH); }
    public boolean severe() { return entityData.get(SEVERE); }

    /** Start collapsing now (weather cleared or dispelled). */
    public void dissipate() { dissipating = true; }

    /** GameTests: jump to a stage of life. */
    public void skipTo(double f) { age = (int) (lifetime * Math.max(0, Math.min(1, f))); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        age++;
        if (dissipating && age < lifetime * 0.85) age = (int) (lifetime * 0.85);
        if (dissipating) age += 4; // fade over about 20 seconds
        double f = age / (double) lifetime;
        entityData.set(GROWTH, (float) f);
        if (f >= 1 || CloudGrowth.opacity(f) <= 0 && f > 0.5) { discard(); return; }
        setPos(getX() + driftX, getY(), getZ() + driftZ);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource s, float a) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 1200 * 1200; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("Age", age);
        out.putInt("Life", lifetime);
        out.putFloat("Width", cloudWidth());
        out.putFloat("Depth", cloudDepth());
        out.putBoolean("Severe", severe());
        out.putDouble("DriftX", driftX);
        out.putDouble("DriftZ", driftZ);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        age = in.getIntOr("Age", 0);
        lifetime = Math.max(200, in.getIntOr("Life", 9600));
        setup(in.getFloatOr("Width", 60), in.getFloatOr("Depth", 120), in.getBooleanOr("Severe", false), lifetime,
                in.getDoubleOr("DriftX", 0), in.getDoubleOr("DriftZ", 0));
    }
}
