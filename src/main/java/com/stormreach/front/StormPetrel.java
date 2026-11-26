package com.stormreach.front;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Storm Petrel: a small sooty seabird with a white rump that flutters low over the waves, pattering its feet on the
 * water. Sailors called them storm birds: flocks gather over the sea in the hours before a storm and leave once it
 * has passed. Drops feathers and, now and then, an Omen Feather.
 */
public class StormPetrel extends PathfinderMob {
    private int calm;

    public StormPetrel(EntityType<? extends StormPetrel> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.55);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        goalSelector.addGoal(2, new SkimGoal());
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < 0) setDeltaMovement(v.multiply(1, 0.6, 1));
        if (level() instanceof ServerLevel server && tickCount % 40 == 0) {
            // once the storm has passed and none is due, the flock heads out to sea and is gone
            calm = (server.isRaining() || FrontArrivals.rainSoon(server)) ? 0 : calm + 40;
            if (calm > 1200 && random.nextInt(4) == 0) discard();
        }
    }

    /** Wanders in long, low loops just above the water (or the ground), a block or two up. */
    class SkimGoal extends Goal {
        SkimGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { return getNavigation().isDone() && random.nextInt(8) == 0; }
        @Override public boolean canContinueToUse() { return getNavigation().isInProgress(); }

        @Override
        public void start() {
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 6 + random.nextDouble() * 12;
                BlockPos col = BlockPos.containing(getX() + Math.cos(a) * r, 0, getZ() + Math.sin(a) * r);
                if (!level().isLoaded(col)) continue;   // 6.0
                BlockPos top = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
                double y = top.getY() + 0.6 + random.nextDouble() * 2.2;
                if (getNavigation().moveTo(col.getX() + 0.5, y, col.getZ() + 0.5, 1.0)) return;
            }
        }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return true; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_PETREL_CALL; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SMALL_DEATH; }
    @Override public float getVoicePitch() { return 1.0f + random.nextFloat() * 0.25f; }
}
