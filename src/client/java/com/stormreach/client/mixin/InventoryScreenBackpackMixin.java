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
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);          // slot well
        g.fill(x, y, x + 17, y + 1, 0xFF373737);           // dark top and left edges
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF); // light bottom and right edges
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }
}
