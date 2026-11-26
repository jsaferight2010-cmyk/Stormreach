package com.stormreach.wild;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;

/**
 * Cloud Whale: a gentle giant of the Tempest Realm that drifts in long, slow arcs high above the ground, singing,
 * with a little rain always falling from its belly. Every so often it leaves a lump of Cloud Ambergris behind.
 */
public class CloudWhale extends Animal {
    private int ambergrisTimer;

    public CloudWhale(EntityType<? extends CloudWhale> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 4, true);
        this.setNoGravity(true);
        this.ambergrisTimer = 6000 + random.nextInt(6000);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.FLYING_SPEED, 0.16).add(Attributes.ARMOR, 4).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DriftGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16f));
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
            if (--ambergrisTimer <= 0) {
                spawnAtLocation(server, new ItemStack(WildRegistry.CLOUD_AMBERGRIS));
                playSound(ModSfx.ITEM_POP, 1.5f, 0.6f);
                ambergrisTimer = 6000 + random.nextInt(6000);
            }
        } else if (random.nextFloat() < 0.7f) {
            // its own little rain shower
            float w = getBbWidth() * 0.4f;
            level().addParticle(ParticleTypes.FALLING_WATER, getX() + (random.nextFloat() - 0.5f) * w * 2, getY() - 0.1,
                    getZ() + (random.nextFloat() - 0.5f) * w * 2, 0, 0, 0);
        }
    }

    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public int getAmbientSoundInterval() { return 320; }
    @Override protected float getSoundVolume() { return 3.0f; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_WHALE_SONG; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_WHALE_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_WHALE_DEATH; }
    @Override public float getVoicePitch() { return 0.85f + random.nextFloat() * 0.3f; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Ambergris", ambergrisTimer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        ambergrisTimer = in.getIntOr("Ambergris", 6000);
    }

    /** Long, slow drifts to points 20-40 blocks out, staying 14-32 blocks above whatever is below. */
    static class DriftGoal extends Goal {
        private final CloudWhale whale;

        DriftGoal(CloudWhale whale) {
            this.whale = whale;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() { return !whale.getMoveControl().hasWanted() || whale.random.nextInt(200) == 0; }
        @Override public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            double a = whale.getYRot() * Mth.DEG_TO_RAD + Mth.HALF_PI + (whale.random.nextDouble() - 0.5) * 1.6;
            double d = 20 + whale.random.nextDouble() * 20;
            double x = whale.getX() + Math.cos(a) * d, z = whale.getZ() + Math.sin(a) * d;
            Level l = whale.level();
            int ground = l.isLoaded(BlockPos.containing(x, 0, z)) ? l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(x, 0, z)).getY() : l.getMinY();   // 6.0
            double y = ground <= l.getMinY() + 1 ? whale.getY() + (whale.random.nextDouble() - 0.5) * 8 : ground + 14 + whale.random.nextDouble() * 18;
            y = Mth.clamp(y, l.getMinY() + 24, l.getMaxY() - 12);
            whale.getMoveControl().setWantedPosition(x, y, z, 1.0);
        }
    }
}
