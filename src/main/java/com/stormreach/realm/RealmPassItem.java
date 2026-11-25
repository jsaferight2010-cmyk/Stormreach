package com.stormreach.realm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Creative Realm Pass: a testing shortcut. Use it in Creative mode to travel to the Tempest Realm and back, through the
 * same travel code as a real Stormgate. It has no recipe and no loot, so Survival players can't get it, and it does
 * nothing for a player who isn't in Creative mode, so the normal Stormgate progression is untouched.
 */
public class RealmPassItem extends Item {
    public RealmPassItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!player.isCreative()) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.realm_pass.creative_only").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (server.getServer().getLevel(TempestRealm.LEVEL) == null) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.realm_pass.missing").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.4, 0.9, 0.4, 0.1);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.2);
        TempestRealm.queueTravel(player);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 40);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.realm_pass.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.stormreach.realm_pass.tooltip2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
