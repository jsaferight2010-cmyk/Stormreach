package com.stormreach.front;

import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Umbrella: held in either hand, it keeps hail off you (each stone it stops wears it a little) and, if you step off
 * a ledge, lets you float down gently with no fall damage. It is also the tallest metal point around, so carrying
 * one out in a thunderstorm now and then draws lightning onto you.
 */
public class UmbrellaItem extends Item {
    public UmbrellaItem(Properties p) {
        super(p);
    }

    static InteractionHand heldIn(LivingEntity e) {
        if (e.getMainHandItem().getItem() instanceof UmbrellaItem) return InteractionHand.MAIN_HAND;
        if (e.getOffhandItem().getItem() instanceof UmbrellaItem) return InteractionHand.OFF_HAND;
        return null;
    }

    /** Called by the hailstone when it would hit {@code e}: true (and the umbrella takes the knock) if it is shielded. */
    public static boolean shields(LivingEntity e) {
        InteractionHand hand = heldIn(e);
        if (hand == null) return false;
        ItemStack s = e.getItemInHand(hand);
        s.hurtAndBreak(1, e, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        if (e instanceof net.minecraft.server.level.ServerPlayer sp) com.stormreach.progress.StormAdvancements.award(sp, "umbrella_hail");
        return true;
    }

    /** Every tick, from LivingEntityMixin: the parachute and the lightning-rod risk. */
    public static void tick(LivingEntity e) {
        if (heldIn(e) == null || e.isSpectator()) return;
        if (e instanceof Player p && p.getAbilities().flying) return;
        Vec3 v = e.getDeltaMovement();
        if (!e.onGround() && !e.isInWater() && !e.isFallFlying() && v.y < -0.16) {
            e.setDeltaMovement(v.x, -0.16, v.z);
            e.resetFallDistance();
        }
        if (e.level() instanceof ServerLevel level && e.tickCount % 400 == 0 && level.isThundering() && level.canSeeSky(e.blockPosition())
                && !StormWeather.isLightningProof(e) && e.getRandom().nextInt(40) == 0) {
            StormWeather.strike(level, BlockPos.containing(e.position()), null);
        }
    }
}
