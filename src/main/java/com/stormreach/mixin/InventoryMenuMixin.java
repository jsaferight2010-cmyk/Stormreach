package com.stormreach.mixin;

import com.stormreach.backpack.BackpackEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the worn-backpack slot to the player's inventory (above the offhand slot) and handles shift-clicking it. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    protected InventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void stormreach$addBackpackSlot(Inventory inv, boolean active, Player owner, CallbackInfo ci) {
        this.addSlot(new BackpackEquipment.WornSlot(owner, 77, 44));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void stormreach$quickMoveBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= slots.size()) return;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return;
        ItemStack stack = slot.getItem();
        if (slot instanceof BackpackEquipment.WornSlot) {
            // take the backpack off into the main inventory, then the hotbar
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, false)) { cir.setReturnValue(ItemStack.EMPTY); return; }
            slot.setByPlayer(ItemStack.EMPTY);
            cir.setReturnValue(copy);
            return;
        }
        if (index >= InventoryMenu.INV_SLOT_START && index < InventoryMenu.USE_ROW_SLOT_END && BackpackEquipment.isBackpack(stack)) {
            for (Slot s : slots) {
                if (s instanceof BackpackEquipment.WornSlot worn && !worn.hasItem()) {
                    ItemStack copy = stack.copy();
                    worn.setByPlayer(stack.split(1));
                    slot.setChanged();
                    cir.setReturnValue(copy);
                    return;
                }
            }
        }
    }
}
