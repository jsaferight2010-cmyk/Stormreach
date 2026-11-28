package com.stormreach.front;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Bottled Dust Devil: a whirlwind caught in a glass bottle. Throw it and it shatters into a dust devil that stays
 * where it landed for ten seconds, sweeping every dropped item within 8 blocks in to the middle.
 */
public class BottledDustDevilItem extends Item {
    public BottledDustDevilItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.6f, 0.8f);
        if (level instanceof ServerLevel server) {
            Projectile.spawnProjectileFromRotation((l, owner, s) -> new Thrown(l, owner, s), server, stack, player, -10f, 0.9f, 1.0f);
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    public static class Thrown extends ThrowableItemProjectile {
        public Thrown(EntityType<? extends Thrown> type, Level level) { super(type, level); }
        public Thrown(Level level, LivingEntity owner, ItemStack stack) { super(FrontRegistry.BOTTLED_DUST_DEVIL_ENTITY, owner, level, stack); }

        @Override protected Item getDefaultItem() { return FrontRegistry.BOTTLED_DUST_DEVIL; }

        @Override
        protected void onHit(HitResult result) {
            super.onHit(result);
            if (level() instanceof ServerLevel server) {
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
                DustDevil d = FrontRegistry.DUST_DEVIL.create(server, EntitySpawnReason.TRIGGERED);
                if (d != null) {
                    BlockPos at = BlockPos.containing(result.getLocation());
                    d.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                    d.anchor(200);
                    server.addFreshEntity(d);
                }
                discard();
            }
        }
    }
}
