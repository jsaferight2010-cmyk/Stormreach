package com.stormreach.power;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Multimeter: use it on any Live Wire block for a reading: stored SU and what it makes or uses, water in a tank, and
 * for a cable, how long the run is and how many machines are on it.
 */
public class MultimeterItem extends Item {
    public MultimeterItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || ctx.getPlayer() == null) return InteractionResult.SUCCESS;
        var pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        MutableComponent msg = null;
        if (be instanceof PowerNode n) {
            msg = Component.translatable("item.stormreach.multimeter.node", Component.translatable("item.stormreach.multimeter.role." + n.role().name().toLowerCase()),
                    n.stored(), n.capacity(), n.rate(), PowerNet.reach(level, pos).size());
        } else if (be instanceof WaterTank t) {
            msg = Component.translatable("block.stormreach.tank.reading", t.water() / 1000.0, t.waterCapacity() / 1000,
                    Component.translatable(t.isStormwater() ? "block.stormreach.tank.stormwater" : "block.stormreach.tank.water"));
        } else if (level.getBlockState(pos).getBlock() instanceof CopperCableBlock) {
            msg = Component.translatable("item.stormreach.multimeter.cable", PowerNet.cableCount(level, pos) + 1, PowerNet.reach(level, pos).size());
        }
        ctx.getPlayer().sendOverlayMessage((msg == null ? Component.translatable("item.stormreach.multimeter.nothing") : msg).withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS;
    }
}
