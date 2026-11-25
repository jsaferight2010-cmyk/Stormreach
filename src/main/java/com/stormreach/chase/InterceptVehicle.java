package com.stormreach.chase;

import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModSfx;
import com.stormreach.sim.EFScale;
import com.stormreach.tornado.Instruments;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Tornado Intercept Vehicle: an armoured storm-chasing truck. Right-click to drive (W/S throttle and brake, A/D steer,
 * sneak to get out); sneak + right-click from outside to pack it back into an item. Its dashboard (the action bar)
 * tracks the nearest tornado: rating, range, bearing, motion and wind. Press the anchor key (default Z) to drop its
 * hydraulic spikes at once so even a violent tornado can't move it, and again to raise them.
 */
public class InterceptVehicle extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(InterceptVehicle.class, EntityDataSerializers.BOOLEAN);
    public static final double TOP_SPEED = 0.55, REVERSE = 0.18, ACCEL = 0.012, BRAKE = 0.04;
    private double speed;
    private int stillTicks;

    public InterceptVehicle(EntityType<? extends InterceptVehicle> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.ARMOR, 15).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ANCHORED, false);
    }

    public boolean isAnchored() { return entityData.get(ANCHORED); }
    public void setAnchored(boolean a) { entityData.set(ANCHORED, a); }
    public double speed() { return speed; }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!isVehicle() && level() instanceof ServerLevel server) {
                if (!player.getAbilities().instabuild) spawnAtLocation(server, new ItemStack(ChaseRegistry.INTERCEPT_VEHICLE));
                discard();
            }
            return InteractionResult.SUCCESS;
        }
        if (!isVehicle()) {
            if (!level().isClientSide()) player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public void travel(Vec3 input) {
        float steer = 0, throttle = 0;
        if (getControllingPassenger() instanceof Player rider) {
            steer = rider.xxa;
            throttle = rider.zza;
        }
        double target = throttle > 0 ? TOP_SPEED : throttle < 0 ? -REVERSE : 0;
        double rate = (target == 0 || Math.signum(target) != Math.signum(speed)) && speed != 0 ? BRAKE : ACCEL;
        speed += Mth.clamp(target - speed, -rate, rate);
        if (Math.abs(speed) < 0.003 && target == 0) speed = 0;
        if (isAnchored()) speed = 0;   // spikes down: the throttle does nothing until they are raised again
        float turn = steer * 4.0f * (float) Math.min(1.0, Math.abs(speed) * 5 + 0.15) * (speed < 0 ? -1 : 1);
        setYRot(getYRot() - turn);
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        Vec3 m = getDeltaMovement();
        double vy = onGround() ? -0.04 : (m.y - 0.08) * 0.98;
        Vec3 drift = isAnchored() ? Vec3.ZERO : new Vec3(m.x, 0, m.z).subtract(fwd.scale(fwd.dot(new Vec3(m.x, 0, m.z)))).scale(0.5); // side slip decays
        setDeltaMovement(fwd.x * speed + drift.x, vy, fwd.z * speed + drift.z);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) speed *= 0.4;
        calculateEntityAnimation(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            if (Math.abs(speed) > 0.2 && random.nextFloat() < 0.3f)
                level().addParticle(ParticleTypes.CLOUD, getX() - Math.sin(-getYRot() * Mth.DEG_TO_RAD) * 1.6, getY() + 0.3,
                        getZ() - Math.cos(getYRot() * Mth.DEG_TO_RAD) * 1.6, 0, 0.02, 0);
            return;
        }
        // 6.0: the spikes are on their own key now (no more two-second wait); see toggleAnchors
        if (tickCount % 10 == 0 && getControllingPassenger() instanceof ServerPlayer driver) dashboard(server, driver);
    }

    /** The dashboard readout: the nearest tornado (or the local weather when there isn't one). */
    private void dashboard(ServerLevel level, ServerPlayer driver) {
        TornadoEntity best = null;
        double bestD = 1024;
        for (TornadoEntity t : TornadoManager.live(level)) {
            double d = t.position().distanceTo(position());
            if (d < bestD) { bestD = d; best = t; }
        }
        Component msg;
        if (best == null) {
            double[] wind = Instruments.windMph(level, blockPosition());
            msg = Component.translatable("chase.stormreach.dash.clear", Math.round(Math.abs(speed) * 20 * 2.237),
                    Math.round(wind[0]), Instruments.windFrom(), Math.round(Instruments.pressureHpa(level, blockPosition()))).withStyle(ChatFormatting.AQUA);
        } else {
            double peak = best.currentPeakMph();
            EFScale ef = EFScale.fromGustMph(peak);
            double dx = best.getX() - getX(), dz = best.getZ() - getZ();
            String bearing = Instruments.compass(Math.toDegrees(Math.atan2(dx, -dz)));
            com.stormreach.sim.WindField f = best.windField();
            String heading = Instruments.compass(Math.toDegrees(Math.atan2(f.translateX, -f.translateZ)));
            double motion = EFScale.msToMph(Math.hypot(f.translateX, f.translateZ));
            double here = best.windMphAt(position().add(0, 10, 0));
            ChatFormatting col = bestD < 60 ? ChatFormatting.RED : bestD < 250 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            msg = Component.translatable("chase.stormreach.dash.tornado", ef == null ? "EF?" : ef.name(), Math.round(bestD), bearing, heading,
                    Math.round(motion), Math.round(peak), Math.round(here), isAnchored() ? Component.translatable("chase.stormreach.dash.anchored") : Component.empty())
                    .withStyle(col);
            if (bestD < 40 && isAnchored() && peak > 85) StormAdvancements.award(driver, "tiv_intercept");
            ChaserLog.recordIntercept(driver, best, bestD, isAnchored());
        }
        // 6.0: the dashboard is drawn by the HUD panel (WeatherHud); msg is kept for the advancement/log logic above
    }

    /**
     * The anchor key: drops the four hydraulic spikes at once (the truck stops dead where it stands, and nothing
     * short of the ground giving way moves it), or raises them again. Server side; called from the key packet.
     */
    public void toggleAnchors(ServerPlayer driver) {
        if (getControllingPassenger() != driver) return;
        boolean down = !isAnchored();
        if (down && !onGround()) return;                 // nothing to bite into in mid-air
        entityData.set(ANCHORED, down);
        if (down) { speed = 0; setDeltaMovement(0, getDeltaMovement().y, 0); }
        if (level() instanceof ServerLevel server)
            server.playSound(null, blockPosition(), ModSfx.TIV_SPIKES, SoundSource.NEUTRAL, 1.5f, down ? 0.9f : 1.15f);
    }

    /** Ground speed in mph (for the dashboard). */
    public double speedMph() { return Math.abs(speed) * 20 * 2.237; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild) {
            discard();
            return true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override public boolean isPushable() { return !isAnchored(); }
    @Override public boolean canBeCollidedWith(Entity other) { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.ITEM_EQUIP_METAL; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.BLOCK_BOOM; }
    @Override protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Anchored", isAnchored());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(ANCHORED, in.getBooleanOr("Anchored", false));
    }
}
