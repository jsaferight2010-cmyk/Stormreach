package com.stormreach.tornado.radar;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Places the whole Doppler radar (3x3 pad, mast, pedestal and dish) centred on the block you click. Holding it shows
 * the footprint as an outline (green fits, red blocked); that outline can be turned off with radarPlacementOutline.
 */
public class DopplerRadarItem extends BlockItem {
    public DopplerRadarItem(Block block, Properties p) {
        super(block, p);
    }

    /** Where the base centre goes for a click: the clicked block if it can be replaced (grass, snow), else the one in front. */
    public static BlockPos baseFor(Level level, BlockPos clicked, net.minecraft.core.Direction face) {
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.tornado_radar.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos base = baseFor(level, ctx.getClickedPos(), ctx.getClickedFace());
        Player p = ctx.getPlayer();
        if (!RadarStructure.blocked(level, base).isEmpty()) {
            if (p != null && !level.isClientSide()) p.sendOverlayMessage(Component.translatable("block.stormreach.tornado_radar.blocked").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            RadarStructure.build(level, base);
            level.playSound(null, base, com.stormreach.registry.ModSfx.BLOCK_METAL_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
            if (p == null || !p.isCreative()) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
