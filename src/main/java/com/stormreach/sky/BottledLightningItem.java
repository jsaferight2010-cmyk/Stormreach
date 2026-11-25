package com.stormreach.sky;

import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bottled Lightning. To catch it, hold an empty glass bottle within 8 blocks of where lightning strikes: one bottle
 * fills. Throw it and it shatters, calling the bolt down where it lands.
 */
public class BottledLightningItem extends Item {
    public BottledLightningItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.accept(net.minecraft.network.chat.Component.translatable(getDescriptionId() + ".tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
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

    /** From StormWeather.onLightning: fills one empty bottle for each player holding one near the strike. */
    public static void onLightning(ServerLevel level, BlockPos hit) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(Vec3.atCenterOf(hit)) > 64) continue;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack s = p.getItemInHand(hand);
                if (s.is(Items.GLASS_BOTTLE)) {
                    s.consume(1, p);
                    ItemStack caught = new ItemStack(SkyRegistry.BOTTLED_LIGHTNING);
                    p.getInventory().placeItemBackInInventory(caught, net.minecraft.util.Prediction.SERVER_ONLY);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
                    level.playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0f, 1.2f);
                    break;
                }
            }
        }
    }

    public static class Thrown extends ThrowableItemProjectile {
        public Thrown(EntityType<? extends Thrown> type, Level level) {
            super(type, level);
        }

        public Thrown(Level level, LivingEntity owner, ItemStack stack) {
            super(SkyRegistry.BOTTLED_LIGHTNING_ENTITY, owner, level, stack);
        }

        @Override
        protected Item getDefaultItem() {
            return SkyRegistry.BOTTLED_LIGHTNING;
        }

        @Override
        protected void onHit(HitResult result) {
            super.onHit(result);
            if (level() instanceof ServerLevel server) {
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
                StormWeather.strike(server, BlockPos.containing(result.getLocation()), getOwner() instanceof ServerPlayer sp ? sp : null);
                discard();
            }
        }
    }
}
