package com.stormreach.story;

import com.stormreach.registry.ModSounds;
import com.stormreach.sim.ShadowRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The Stranger: a storm-weathered copy of whoever sees it, seen far off among trees, in rain and fog. It only watches.
 * It can't be hurt or interacted with, and it's gone the moment you look straight at it or come close.
 */
public class Stranger extends PathfinderMob {
    private UUID watching;
    private int lifetime = 200 + 200;

    public Stranger(EntityType<? extends Stranger> type, Level level) {
        super(type, level);
                this.setSilent(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void watch(ServerPlayer p, int ticks) {
        this.watching = p.getUUID();
        this.lifetime = ticks;
    }

    @Override
    protected void registerGoals() {
        // no goals: it does nothing but stand and stare
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // it has been standing in the rain a long time: water runs off it
            if (random.nextFloat() < 0.35f) {
                level().addParticle(ParticleTypes.DRIPPING_WATER, getX() + (random.nextFloat() - 0.5f) * 0.6f,
                        getY() + 0.3f + random.nextFloat() * 1.5f, getZ() + (random.nextFloat() - 0.5f) * 0.6f, 0, 0, 0);
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) return;
        Player p = watching == null ? null : level.getPlayerByUUID(watching);
        if (p == null || --lifetime <= 0) {
            vanish(level, null);
            return;
        }
        Vec3 to = position().add(0, 1.6, 0).subtract(p.getEyePosition());
        getLookControl().setLookAt(p, 360, 360);
        setYRot((float) Math.toDegrees(Math.atan2(-to.x, to.z)) + 180f);
        yHeadRot = getYRot();
        yBodyRot = getYRot();
        Vec3 look = p.getViewVector(1.0f);
        boolean stared = p.hasLineOfSight(this) && ShadowRules.beingStaredAt(look.x, look.y, look.z, to.x, to.y, to.z);
        if (stared || p.distanceToSqr(this) < 14 * 14) vanish(level, p instanceof ServerPlayer sp ? sp : null);
    }

    private void vanish(ServerLevel level, ServerPlayer witness) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.01);
        if (witness != null) {
            level.playSound(null, blockPosition(), ModSounds.STRANGER_WHISPER, SoundSource.AMBIENT, 0.6f, 0.8f);
            StrangerDirector.recordSighting(witness);
            // once you've seen it a few times, it starts leaving something behind
            if (StrangerDirector.sightings(witness) >= 3 && level.getRandom().nextFloat() < 0.25f) {
                ItemEntity remnant = new ItemEntity(level, getX(), getY() + 0.3, getZ(), new ItemStack(StoryRegistry.HOLLOW_REMNANT));
                remnant.setUnlimitedLifetime();
                level.addFreshEntity(remnant);
            }
        }
        discard();
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean shouldShowName() { return false; }
}
