package com.stormreach.client;

import com.stormreach.chase.InterceptVehicle;
import com.stormreach.chase.TivControls;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

/** 6.0: "Drop/raise TIV anchors" (default Z, rebindable under Controls > Stormreach). Acts on the press, no delay. */
public final class TivKeys {
    private TivKeys() {}

    private static KeyMapping anchor;

    public static void init() {
        anchor = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.stormreach.tiv_anchor", 90 /* GLFW_KEY_Z */, StormKeys.category()));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (anchor.consumeClick()) {
                if (mc.player != null && mc.player.getVehicle() instanceof InterceptVehicle)
                    ClientPlayNetworking.send(new TivControls.AnchorPayload());
            }
        });
    }
}
