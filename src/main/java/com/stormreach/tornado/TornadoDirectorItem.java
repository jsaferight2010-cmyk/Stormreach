package com.stormreach.tornado;

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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Creative Tornado Director: point at the ground (up to 256 blocks away) and use it to send the nearest tornado
 * there; it curves toward the spot and stops on it. Sneak and use to let the tornado wander on its own again.
 * Creative mode only, no recipe. Waterspouts still won't leave the water.
 */
public class TornadoDirectorItem extends Item {
    public TornadoDirectorItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!player.isCreative()) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_director.creative_only").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        TornadoEntity nearest = null;
        double best = 512 * 512;
        for (TornadoEntity t : TornadoManager.live(server)) {
            double d = t.distanceToSqr(player);
            if (d < best && !t.isRemoved()) { best = d; nearest = t; }
        }
        if (nearest == null) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_director.none").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            nearest.release();
            player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_director.released"));
            return InteractionResult.SUCCESS_SERVER;
        }
        HitResult hit = player.pick(256, 1.0f, false);
        Vec3 aim = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getViewVector(1.0f).scale(64)) : hit.getLocation();
        int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(aim.x), (int) Math.floor(aim.z));
        Vec3 target = new Vec3(aim.x, y, aim.z);
        nearest.steerTo(target);
        // a marker column at the target, and a message with where the tornado is going
        for (int i = 0; i < 20; i++) server.sendParticles(ParticleTypes.END_ROD, target.x, target.y + i * 0.6, target.z, 2, 0.15, 0.1, 0.15, 0.01);
        player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_director.sent", (int) Math.round(target.x), (int) Math.round(target.z),
                (int) Math.round(Math.sqrt(nearest.distanceToSqr(target.x, nearest.getY(), target.z)))).withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.tornado_director.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.stormreach.tornado_director.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.stormreach.tornado_director.tooltip3").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
