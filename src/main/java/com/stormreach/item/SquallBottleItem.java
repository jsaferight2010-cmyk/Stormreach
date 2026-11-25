package com.stormreach.item;

import com.stormreach.entity.SquallBottleProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Throw it and it bursts into a gust. Throw it at your feet to launch yourself. */
public class SquallBottleItem extends Item {
    public SquallBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS, 0.5f, 0.6f);
        if (level instanceof ServerLevel server) {
            Projectile.spawnProjectileFromRotation((lvl, owner, s) -> new SquallBottleProjectile(lvl, owner, s),
                    server, stack, player, 0.0f, 1.2f, 1.0f);
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
