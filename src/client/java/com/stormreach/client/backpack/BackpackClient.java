package com.stormreach.client.backpack;

import com.stormreach.Stormreach;
import com.stormreach.backpack.Backpacks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

public final class BackpackClient {
    private static KeyMapping openKey;

    private BackpackClient() {}

    private static boolean heldInScreen;

    public static void init() {
        for (int rows = 1; rows <= 6; rows++) MenuScreens.register(Backpacks.MENUS[rows], BackpackScreen::new);
        KeyMapping.Category category = com.stormreach.client.StormKeys.category();
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.stormreach.open_backpack", 66 /* GLFW_KEY_B */, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(new Backpacks.OpenBackpackPayload());
            }
            // 6.0: the key also works with the inventory open (key mappings don't fire while a screen is up)
            boolean inInventory = client.player != null && client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen;
            int code = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(openKey).getValue();
            boolean down = inInventory && code > 0 && com.mojang.blaze3d.platform.InputConstants.isKeyDown(code);
            if (down && !heldInScreen) ClientPlayNetworking.send(new Backpacks.OpenBackpackPayload());
            heldInScreen = down;
        });
    }
}
