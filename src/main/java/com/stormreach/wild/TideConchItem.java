package com.stormreach.wild;

import com.stormreach.registry.ModSfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tide Conch: blow it and everyone within 8 blocks can breathe underwater for a minute and swims like a dolphin for
 * half that; Shock Eels within 24 blocks calm down for a minute. One minute to recover between blows.
 */
public class TideConchItem extends Item {
    public TideConchItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            for (Player p : server.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8))) {
                p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 600, 0));
            }
            for (ShockEel e : server.getEntitiesOfClass(ShockEel.class, player.getBoundingBox().inflate(24))) e.soothe(1200);
            server.playSound(null, player.blockPosition(), ModSfx.ITEM_CONCH, SoundSource.PLAYERS, 3.0f, 1.0f);
            stack.hurtAndBreak(1, player, hand);
        }
        player.getCooldowns().addCooldown(stack, 1200);
        return InteractionResult.SUCCESS;
    }
}
