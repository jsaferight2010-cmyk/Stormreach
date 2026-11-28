package com.stormreach.block;

import com.stormreach.StormreachConfig;
import com.stormreach.entity.Stormwarden;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModItems;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Block of Storm Crystal. Using a Storm Cell on it during a thunderstorm, under open sky,
 * shatters the block and calls down the Stormwarden boss.
 */
public class StormCrystalBlock extends Block {
    public StormCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.STORM_CELL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!StormreachConfig.bossSummoning) {
            player.sendOverlayMessage(Component.translatable("message.stormreach.boss_disabled"));
            return InteractionResult.FAIL;
        }
        if (!server.isThundering() || !server.canSeeSky(pos.above())) {
            player.sendOverlayMessage(Component.translatable("message.stormreach.boss_needs_storm").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        Stormwarden boss = ModEntities.STORMWARDEN.create(server, EntitySpawnReason.TRIGGERED);
        if (boss == null) return InteractionResult.FAIL;
        server.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        StormWeather.strikeVisual(server, pos);
        boss.snapTo(pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5, player.getYRot() + 180f, 0f);
        server.addFreshEntity(boss);
        stack.consume(1, player);
        server.players().forEach(p -> {
            if (p.distanceToSqr(boss) < 128 * 128) {
                p.sendSystemMessage(Component.translatable("message.stormreach.boss_summoned").withStyle(ChatFormatting.DARK_AQUA));
            }
        });
        return InteractionResult.SUCCESS_SERVER;
    }
}
