package com.stormreach.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a slot frame for the worn-backpack slot (the vanilla inventory picture has no slot there). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenBackpackMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void stormreach$backpackSlotFrame(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) this;
        int x = a.stormreach$left() + 76, y = a.stormreach$top() + 43;
        // 6.0: the game's own slot sprite, so the backpack slot looks like every other slot
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.resources.Identifier.withDefaultNamespace("container/slot"), x, y, 18, 18);
    }
}
