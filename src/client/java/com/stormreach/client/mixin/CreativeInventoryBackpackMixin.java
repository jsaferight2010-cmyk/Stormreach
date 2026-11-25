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
 * on top of a hotbar slot; move it off-screen instead (in Creative, sneak + use or the backpack key still work).
 * Optional: if this can't apply, the game still runs.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryBackpackMixin {
    @ModifyArgs(method = "selectTab", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void stormreach$hideBackpackSlot(Args args) {
        Slot target = args.get(0);
        if (target instanceof BackpackEquipment.WornSlot) {
            args.set(2, -2000);
            args.set(3, -2000);
        }
    }
}
