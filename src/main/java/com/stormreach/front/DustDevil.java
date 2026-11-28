package com.stormreach.front;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Dust Devil: a whirlwind of hot, clear afternoons in deserts, badlands and savannas. It skitters across the ground
 * for a minute or so, lifting sand and loose items and spinning anyone it crosses, then falls apart. It does no harm.
 * 6.0: it has 20 health (each hit tears dust out of it) instead of vanishing at one touch; an explosion still blows it
 * apart. Its spawn egg is intentional (for builders and testing): an egg-spawned devil isn't broken up by rain. Catch one in a glass bottle. A thrown Bottled Dust Devil sets a short-lived one down that
 * stays put and sweeps up the dropped items around it.
 */
public class DustDevil extends PathfinderMob {
    private int life = 900;
    private boolean anchored;
    private boolean fromEgg;

    public DustDevil(EntityType<? extends DustDevil> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.32);
    }

    /** A bottled devil: stays where it lands for {@code ticks} and sweeps items in (radius 8). */
    public void anchor(int ticks) {
        anchored = true;
        life = ticks;
    }

    public boolean isAnchored() { return anchored; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.3) {
            @Override public boolean canUse() { return !anchored && super.canUse(); }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (--life <= 0 || isInWater() || (server.isRaining() && !anchored && !fromEgg)) { scatter(server); return; }
        if (anchored) setDeltaMovement(0, getDeltaMovement().y, 0);
        double reach = anchored ? 8 : 2.5;
        Vec3 c = position();
        for (Entity e : server.getEntities(this, getBoundingBox().inflate(reach, 1, reach), e -> e instanceof ItemEntity || e instanceof LivingEntity)) {
            Vec3 d = e.position().subtract(c);
            double dist = Math.max(0.3, Math.hypot(d.x, d.z));
            Vec3 tangent = new Vec3(-d.z / dist, 0, d.x / dist);
            if (e instanceof ItemEntity item) {
                // swirl items round and up, then (anchored) in to the centre
                Vec3 in = anchored ? new Vec3(-d.x / dist, 0, -d.z / dist).scale(0.12) : Vec3.ZERO;
                item.setDeltaMovement(tangent.scale(0.18).add(in).add(0, e.getY() < getY() + 2.5 ? 0.12 : -0.02, 0));
                item.needsSync = true;
            } else if (dist < 1.6 && !(e instanceof DustDevil) && !(e instanceof Player p && p.isSpectator())) {
                e.push(tangent.x * 0.15, 0.08, tangent.z * 0.15);
                e.needsSync = true;
                if (e instanceof LivingEntity le) le.setYRot(le.getYRot() + 25);
            }
        }
        if (tickCount % 2 == 0) {
            BlockState ground = server.getBlockState(blockPosition().below());
            if (ground.isAir()) ground = Blocks.SAND.defaultBlockState();
            for (int i = 0; i < 3; i++) {
                double a = (tickCount * 0.5 + i * 2.1), r = 0.3 + i * 0.35;
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX() + Math.cos(a) * r, getY() + i * 0.9 + random.nextDouble() * 0.6,
                        getZ() + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.02);
            }
            if (random.nextInt(3) == 0) server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 1, 0.4, 0.05, 0.4, 0.01);
        }
    }

    private void scatter(ServerLevel server) {
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.2, getZ(), 24, 0.6, 1.0, 0.6, 0.05);
        server.playSound(null, blockPosition(), ModSfx.WEATHER_GUST, SoundSource.NEUTRAL, 0.6f, 1.6f);
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) { scatter(level); return true; }
        if (isInvulnerableTo(level, source)) return false;
        // each hit tears dust out of it; the last one breaks it up (it never leaves a body)
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 6 + (int) amount, 0.4, 0.6, 0.4, 0.04);
        if (amount >= getHealth()) { scatter(level); return true; }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance diff,
                                                                   net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.world.entity.SpawnGroupData data) {
        if (reason == net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE) fromEgg = true;
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE) || anchored) return super.mobInteract(player, hand);
        if (level() instanceof ServerLevel server) {
            held.consume(1, player);
            player.getInventory().placeItemBackInInventory(new ItemStack(FrontRegistry.BOTTLED_DUST_DEVIL), net.minecraft.util.Prediction.SERVER_ONLY);
            server.playSound(null, blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0f, 1.3f);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.stormreach.progress.StormAdvancements.award(sp, "bottled_dust_devil");
            discard();
        }
        return InteractionResult.SUCCESS;
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity e) {}
    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return true; }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_DUST_DEVIL_WHIRL; }
    @Override public int getAmbientSoundInterval() { return 60; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.WEATHER_GUST; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Life", life);
        out.putBoolean("Anchored", anchored);
        out.putBoolean("FromEgg", fromEgg);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        life = in.getIntOr("Life", 900);
        anchored = in.getBooleanOr("Anchored", false);
        fromEgg = in.getBooleanOr("FromEgg", false);
    }
}
