package com.stormreach.client.tornado;

import com.stormreach.StormreachConfig;
import com.stormreach.client.StormKeys;
import com.stormreach.tornado.TornadoClientEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Creative/Spectator viewing aid: tornadoes are drawn nearly transparent, their debris clouds and particles thinned
 * out, and the core (the ring of strongest wind) marked on the ground. Toggle with the "See inside tornadoes" key
 * (O by default) or the creativeSeeThroughTornadoes line in config/stormreach.properties. It does nothing in
 * Survival or Adventure, so it can't be used to cheat.
 */
public final class TornadoSeeThrough {
    private static KeyMapping key;

    private TornadoSeeThrough() {}

    /** True when the setting is on and the local player is in Creative or Spectator mode. */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return StormreachConfig.creativeSeeThroughTornadoes && mc.player != null && (mc.player.isCreative() || mc.player.isSpectator());
    }

    public static void init() {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.stormreach.see_inside_tornadoes", 79 /* GLFW_KEY_O */, StormKeys.category()));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.consumeClick()) {
                if (mc.player == null) continue;
                if (!(mc.player.isCreative() || mc.player.isSpectator())) {
                    mc.player.sendOverlayMessage(Component.translatable("key.stormreach.see_inside_tornadoes.creative_only").withStyle(ChatFormatting.GRAY));
                    continue;
                }
                StormreachConfig.creativeSeeThroughTornadoes = !StormreachConfig.creativeSeeThroughTornadoes;
                StormreachConfig.save();
                mc.player.sendOverlayMessage(Component.translatable(StormreachConfig.creativeSeeThroughTornadoes
                        ? "key.stormreach.see_inside_tornadoes.on" : "key.stormreach.see_inside_tornadoes.off"));
            }
            TornadoClientEffects.seeThrough = active();
        });
    }
}
