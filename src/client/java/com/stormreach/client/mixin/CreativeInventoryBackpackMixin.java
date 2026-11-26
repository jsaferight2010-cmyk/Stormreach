package com.stormreach.client.mixin;

import com.stormreach.backpack.BackpackEquipment;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * The creative inventory tab lays out the survival inventory's slots by index and would put the worn-backpack slot
 * on top of a hotbar slot; give it its own spot right of the armour slots instead.
 * Optional: if this can't apply, the game still runs.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryBackpackMixin {
    @ModifyArgs(method = "selectTab", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void stormreach$hideBackpackSlot(Args args) {
        Slot target = args.get(0);
        if (target instanceof BackpackEquipment.WornSlot) {
            // 6.0: a real slot in the creative inventory tab, right of the armour slots
            args.set(2, SLOT_X);
            args.set(3, SLOT_Y);
        }
    }

    private static final int SLOT_X = 127, SLOT_Y = 20;

    @org.spongepowered.asm.mixin.injection.Inject(method = "extractBackground", at = @At("TAIL"))
    private void stormreach$backpackSlotFrame(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float pt,
                                              org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (!self.isInventoryOpen()) return;
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) (Object) this;
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                net.minecraft.resources.Identifier.withDefaultNamespace("container/slot"),
                a.stormreach$left() + SLOT_X - 1, a.stormreach$top() + SLOT_Y - 1, 18, 18);
    }
}
