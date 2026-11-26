package com.stormreach.realm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0: the Tempest Eye won from the Sovereign, hanging over the Sovereign Altar until someone takes it. It bobs and
 * glows, can't be hurt or pushed, is saved with the world (so it survives logging out, dying or a restart) and gives
 * exactly one Tempest Eye to the first player who uses it, then disappears.
 */
public class TempestEyeReward extends Entity implements ItemSupplier {
    private Vec3 anchor;
    private boolean taken;

    public TempestEyeReward(EntityType<? extends TempestEyeReward> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    /** Places a reward floating 2 blocks above {@code over}. */
    public static TempestEyeReward spawn(ServerLevel level, Vec3 over) {
        TempestEyeReward e = new TempestEyeReward(RealmEntities.TEMPEST_EYE_REWARD, level);
        e.anchor = over.add(0, 2.0, 0);
        e.setPos(e.anchor);
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY(), e.getZ(), 40, 0.3, 0.3, 0.3, 0.1);
        return e;
    }

    @Override public ItemStack getItem() { return new ItemStack(RealmItems.TEMPEST_EYE); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isCurrentlyGlowing() { return true; }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            if (random.nextInt(3) == 0) level().addParticle(com.stormreach.particle.ModParticles.STORM_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            return;
        }
        if (anchor == null) anchor = position();
        setPos(anchor.x, anchor.y + Math.sin(tickCount * 0.08) * 0.15, anchor.z);   // a slow bob
        if (tickCount % 40 == 0) server.playSound(null, getX(), getY(), getZ(), com.stormreach.registry.ModSfx.MAGIC_HUM, SoundSource.NEUTRAL, 0.6f, 1.4f);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 at) {
        if (!(level() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (taken || isRemoved()) return InteractionResult.FAIL;
        taken = true;   // exactly once, even if two players click in the same tick
        ItemStack eye = getItem();
        player.getInventory().placeItemBackInInventory(eye, net.minecraft.util.Prediction.SERVER_ONLY);   // inventory, or at your feet if full
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 30, 0.2, 0.2, 0.2, 0.15);
        server.playSound(null, getX(), getY(), getZ(), com.stormreach.registry.ModSfx.MAGIC_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
        player.sendSystemMessage(Component.translatable("entity.stormreach.tempest_eye_reward.taken").withStyle(ChatFormatting.AQUA));
        discard();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        if (anchor != null) { out.putDouble("AX", anchor.x); out.putDouble("AY", anchor.y); out.putDouble("AZ", anchor.z); }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        double ay = in.getDoubleOr("AY", Double.NaN);
        if (!Double.isNaN(ay)) anchor = new Vec3(in.getDoubleOr("AX", 0), ay, in.getDoubleOr("AZ", 0));
    }
}
