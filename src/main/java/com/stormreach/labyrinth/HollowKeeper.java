package com.stormreach.labyrinth;

import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModSounds;
import com.stormreach.sim.HidingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Hollow Keeper: the Labyrinth's guardian. It exists only while someone is inside the maze, can't leave it,
 * hunts by sound when it can't see you, and checks lockers where it last saw you.
 */
public class HollowKeeper extends Monster {
    private BoundingBox home;
    private Vec3 lastKnown;
    private int searchTicks;

    public HollowKeeper(EntityType<? extends HollowKeeper> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.setPersistenceRequired();
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(180 * StormreachConfig.guardianDifficulty / 100.0);
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 180).add(Attributes.ATTACK_DAMAGE, 13)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ARMOR, 10).add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 64).add(Attributes.STEP_HEIGHT, 1.1);
    }

    public void setHome(BoundingBox box) {
        this.home = box;
    }

    public BoundingBox home() {
        return home;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new HuntGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || home == null) return;
        if (!HidingRules.inside(getX(), getY(), getZ(), home.minX(), home.minY(), home.minZ(), home.maxX(), home.maxY(), home.maxZ())) {
            // never let it out: put it back in the middle of the maze
            teleportTo(home.getCenter().getX() + 0.5, home.minY() + 1, home.getCenter().getZ() + 0.5);
            getNavigation().stop();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.LIGHTNING_BOLT)) return false;
        if (source.isDirect() == false && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) amount *= 0.5f;
        return super.hurtServer(level, source, amount);
    }

    @Override public void thunderHit(ServerLevel level, LightningBolt lightning) {}
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.CREATURE_TITAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.stormreach.registry.ModSfx.CREATURE_TITAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_TITAN_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (home != null) {
            out.putInt("HX0", home.minX()); out.putInt("HY0", home.minY()); out.putInt("HZ0", home.minZ());
            out.putInt("HX1", home.maxX()); out.putInt("HY1", home.maxY()); out.putInt("HZ1", home.maxZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        int y0 = in.getIntOr("HY0", Integer.MIN_VALUE);
        if (y0 != Integer.MIN_VALUE) home = new BoundingBox(in.getIntOr("HX0", 0), y0, in.getIntOr("HZ0", 0), in.getIntOr("HX1", 0), in.getIntOr("HY1", 0), in.getIntOr("HZ1", 0));
    }

    /** Chases what it can see, walks to where it last heard you, and searches lockers when you vanish. */
    static class HuntGoal extends Goal {
        private final HollowKeeper k;
        private int listen;

        HuntGoal(HollowKeeper k) {
            this.k = k;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { return k.home != null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (!(k.level() instanceof ServerLevel level)) return;
            ServerPlayer visible = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || p.isCreative() || !k.home.isInside(p.blockPosition())) continue;
                if (HidingTracker.isHidden(p)) continue;
                double d = k.distanceToSqr(p);
                if (d < 32 * 32 && k.hasLineOfSight(p) && d < best) { best = d; visible = p; }
            }
            if (visible != null) {
                if (k.getTarget() != visible) k.playSound(ModSounds.GUARDIAN_SCREECH, 2.5f, 0.8f);
                k.setTarget(visible);
                k.setAggressive(true);
                HidingTracker.LAST_SEEN.put(visible.getUUID(), level.getGameTime());
                k.lastKnown = visible.position();
                k.getNavigation().moveTo(visible, HidingRules.huntSpeed(true, Math.sqrt(best)));
                k.getLookControl().setLookAt(visible, 30, 30);
                if (best < 3.2 && k.tickCount % 12 == 0) k.doHurtTarget(level, visible);
                k.searchTicks = 0;
                return;
            }
            k.setAggressive(false);
            k.setTarget(null);
            // it hears footsteps: every few seconds it learns roughly where the nearest moving player is
            if (++listen >= 80) {
                listen = 0;
                ServerPlayer heard = null;
                double hd = Double.MAX_VALUE;
                for (ServerPlayer p : level.players()) {
                    if (p.isSpectator() || p.isCreative() || !k.home.isInside(p.blockPosition())) continue;
                    double d = k.distanceToSqr(p);
                    if (HidingTracker.isHidden(p)) {
                        if (d < 7 * 7 && HidingTracker.searchCheck(level, k, p)) {
                            HidingTracker.discovered(p);
                            k.playSound(ModSounds.GUARDIAN_SCREECH, 3f, 0.6f);
                        }
                        continue;
                    }
                    if (d < hd) { hd = d; heard = p; }
                }
                if (heard != null) {
                    double noise = heard.isSprinting() ? 2 : heard.isShiftKeyDown() ? 10 : 5;
                    k.lastKnown = heard.position().add((level.getRandom().nextDouble() - 0.5) * noise * 2, 0, (level.getRandom().nextDouble() - 0.5) * noise * 2);
                }
            }
            if (k.lastKnown != null) {
                if (k.getNavigation().isDone()) k.getNavigation().moveTo(k.lastKnown.x, k.lastKnown.y, k.lastKnown.z, HidingRules.huntSpeed(false, 20));
                if (k.position().distanceTo(k.lastKnown) < 2.5) {
                    // search the lockers around here, one check a second
                    if (++k.searchTicks % 20 == 0) {
                        for (ServerPlayer p : level.players()) {
                            if (HidingTracker.isHidden(p) && p.distanceToSqr(k) < 6 * 6 && HidingTracker.searchCheck(level, k, p)) {
                                HidingTracker.discovered(p);
                                k.playSound(ModSounds.GUARDIAN_SCREECH, 3f, 0.6f);
                            }
                        }
                    }
                    if (k.searchTicks > 200) { k.lastKnown = null; k.searchTicks = 0; }
                }
            } else if (k.getNavigation().isDone() && k.tickCount % 60 == 0) {
                BlockPos c = k.home.getCenter();
                k.getNavigation().moveTo(c.getX() + level.getRandom().nextInt(120) - 60, k.home.minY() + 1, c.getZ() + level.getRandom().nextInt(120) - 60, 0.7);
            }
        }
    }

    public static void unused(ServerLevel l) { l.playSound(null, BlockPos.ZERO, com.stormreach.registry.ModSfx.CREATURE_TITAN_AMBIENT, SoundSource.HOSTILE, 0, 0); }
}
