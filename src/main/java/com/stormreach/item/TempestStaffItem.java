package com.stormreach.item;

import com.stormreach.StormreachConfig;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Calls lightning down where you aim. Works in any weather, but costs no durability during a storm. */
public class TempestStaffItem extends Item {
    public TempestStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(StormreachConfig.tempestStaffRange));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.tempest_staff.no_target"));
            return InteractionResult.FAIL;
        }

        StormWeather.strike(server, hit.getBlockPos().above(), player instanceof ServerPlayer sp ? sp : null, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 0.6f, 1.2f);
        if (!server.isThundering()) {
            stack.hurtAndBreak(1, player, hand);
        }
        if (StormreachConfig.tempestStaffCooldown > 0) {
            player.getCooldowns().addCooldown(stack, StormreachConfig.tempestStaffCooldown * 20);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.tempest_staff.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
