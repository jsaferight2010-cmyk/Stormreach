package com.stormreach.sky;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.util.EnumSet;

/**
 * Aurora Moth: a big night moth of the Tempest Realm whose wings shimmer through the colours of the aurora. Drawn to
 * bright light, it circles lamps and lanterns, and now and then sheds a pinch of Aurora Dust.
 */
public class AuroraMoth extends PathfinderMob {
    private int dustTimer = 2400;

    public AuroraMoth(EntityType<? extends AuroraMoth> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.45);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new CircleLightGoal());
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (--dustTimer <= 0) {
                spawnAtLocation(server, new ItemStack(SkyRegistry.AURORA_DUST));
                dustTimer = 2400 + random.nextInt(2400);
            }
        } else if (random.nextFloat() < 0.15f) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, -0.01, 0);
        }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return level().isBrightOutside(); }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_MOTH_FLUTTER; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SMALL_DEATH; }
    @Override protected float getSoundVolume() { return 0.4f; }

    /** Finds the brightest block light within 8 blocks and circles it. */
    class CircleLightGoal extends Goal {
        private BlockPos lamp;
        private int search;

        CircleLightGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (--search > 0) return lamp != null;
            search = 40;
            lamp = null;
            int best = 11;
            BlockPos at = blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, -4, -8), at.offset(8, 4, 8))) {
                int l = level().getBrightness(LightLayer.BLOCK, p);
                if (l > best && !level().getBlockState(p).isAir()) { best = l; lamp = p.immutable(); }
            }
            return lamp != null;
        }

        @Override public boolean canContinueToUse() { return lamp != null && search-- > -200; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            double a = tickCount * 0.12;
            getMoveControl().setWantedPosition(lamp.getX() + 0.5 + Math.cos(a) * 1.6, lamp.getY() + 1.0 + Math.sin(a * 0.7) * 0.6,
                    lamp.getZ() + 0.5 + Math.sin(a) * 1.6, 1.0);
        }
    }
}
