package com.stormreach.sky;

import com.stormreach.progress.StormAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Cloud Seeder: a rocket that sows the clouds. Fired outdoors, it brings rain for five minutes; the Supercell Seeder
 * brings a thunderstorm (and with it Thunderbirds, Hailstone Golems and tornado weather). Only under open sky, and not
 * in the Nether or the End.
 */
public class CloudSeederItem extends Item {
    private final boolean supercell;

    public CloudSeederItem(Properties p, boolean supercell) {
        super(p);
        this.supercell = supercell;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.accept(net.minecraft.network.chat.Component.translatable(getDescriptionId() + ".tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!server.dimensionType().hasSkyLight() || server.dimensionType().hasCeiling() || !server.canSeeSky(player.blockPosition().above())) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.cloud_seeder.no_sky").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (supercell ? server.isThundering() : server.isRaining()) {
            player.sendOverlayMessage(Component.translatable("item.stormreach.cloud_seeder.already").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        server.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0f, 0.8f);
        for (int i = 0; i < 40; i++)
            server.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 2 + i * 0.8, player.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
        server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 34, player.getZ(), 60, 3, 1, 3, 0.02);
        server.getServer().setWeatherParameters(0, 6000, true, supercell);
        player.sendSystemMessage(Component.translatable(supercell ? "item.stormreach.supercell_seeder.done" : "item.stormreach.cloud_seeder.done")
                .withStyle(supercell ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        if (player instanceof ServerPlayer sp) StormAdvancements.award(sp, supercell ? "supercell_seeder" : "cloud_seeder");
        player.getItemInHand(hand).consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
