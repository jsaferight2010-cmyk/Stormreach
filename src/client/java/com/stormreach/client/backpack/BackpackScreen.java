package com.stormreach.client.backpack;

import com.stormreach.backpack.BackpackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/** The chest-style backpack screen with page and sort buttons. */
public class BackpackScreen extends ContainerScreen {
    private Button prev, next, pageLabel, sort;

    public BackpackScreen(ChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private BackpackMenu bp() {
        return (BackpackMenu) menu;
    }

    private void press(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        int right = leftPos + imageWidth;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> press(BackpackMenu.BUTTON_PREV)).bounds(right + 2, topPos + 4, 14, 14).build());
        pageLabel = addRenderableWidget(Button.builder(Component.literal("1/1"), b -> {}).bounds(right + 2, topPos + 20, 30, 14).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> press(BackpackMenu.BUTTON_NEXT)).bounds(right + 18, topPos + 4, 14, 14).build());
        sort = addRenderableWidget(Button.builder(Component.translatable("gui.stormreach.backpack.sort"), b -> press(BackpackMenu.BUTTON_SORT))
                .bounds(right + 2, topPos + 36, 30, 14).build());
        pageLabel.active = false;
        refresh();
    }

    private void refresh() {
        if (prev == null) return;
        int pages = bp().pages();
        boolean paged = pages > 1;
        prev.visible = next.visible = pageLabel.visible = paged;
        prev.active = bp().page() > 0;
        next.active = bp().page() + 1 < pages;
        pageLabel.setMessage(Component.literal((bp().page() + 1) + "/" + pages));
        sort.visible = bp().canSort();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }
}
