package com.stormreach.chase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** The storm-chasing items that do something when used. */
public final class ChaseItems {
    private ChaseItems() {}

    /** Places a Tornado Intercept Vehicle where you click, facing the way you face. */
    public static class VehicleItem extends Item {
        public VehicleItem(Properties p) { super(p); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
            BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
            InterceptVehicle v = ChaseRegistry.INTERCEPT_VEHICLE_ENTITY.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (v == null) return InteractionResult.FAIL;
            float yaw = ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYRot();
            v.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
            if (!level.noCollision(v, v.getBoundingBox())) return InteractionResult.FAIL;
            level.addFreshEntity(v);
            ctx.getItemInHand().consume(1, ctx.getPlayer());
            return InteractionResult.SUCCESS;
        }
    }

    /** Launches a weather balloon from where you stand. */
    public static class BalloonItem extends Item {
        public BalloonItem(Properties p) { super(p); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                WeatherBalloon b = ChaseRegistry.WEATHER_BALLOON_ENTITY.create(server, EntitySpawnReason.SPAWN_ITEM_USE);
                if (b == null) return InteractionResult.FAIL;
                b.snapTo(player.getX(), player.getY() + 1.2, player.getZ(), player.getYRot(), 0);
                b.launch(sp);
                server.addFreshEntity(b);
                ChaserLog.recordBalloon(sp);
                player.getItemInHand(hand).consume(1, player);
                player.sendOverlayMessage(Component.translatable("item.stormreach.weather_balloon.launched"));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Shows the log. */
    public static class LogbookItem extends Item {
        public LogbookItem(Properties p) { super(p); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) for (Component c : ChaserLog.summary(player.getItemInHand(hand), player)) player.sendSystemMessage(c);
            return InteractionResult.SUCCESS;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            var t = ChaserLog.read(stack);
            out.accept(Component.translatable("item.stormreach.chasers_logbook.tooltip", t.getIntOr("Seen", 0)).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
