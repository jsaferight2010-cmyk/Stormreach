package com.stormreach.backpack;

import com.stormreach.Stormreach;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The worn backpack. It lives in the player's BODY equipment slot (unused by players in vanilla), so the game itself
 * saves it, syncs it to everyone who can see you (for the model on your back) and drops it on death. It is not the
 * chestplate slot: armour and a backpack are worn together. A slot for it appears in the inventory screen, just
 * above the offhand slot; sneak + use with a backpack in hand also puts it on.
 */
public final class BackpackEquipment {
    private BackpackEquipment() {}

    public static final EquipmentSlot SLOT = EquipmentSlot.BODY;
    /** Stands for "the worn backpack" wherever a backpack menu stores which inventory slot it came from. */
    public static final int WORN = -2;
    public static final Identifier EMPTY_ICON = Stormreach.id("container/slot/backpack");

    public static ItemStack worn(Player p) {
        return p.getItemBySlot(SLOT);
    }

    public static boolean isBackpack(ItemStack s) {
        return s.getItem() instanceof BackpackItem;
    }

    /** Swaps the stack in the player's hand with the worn backpack. */
    public static void swapFromHand(Player p, net.minecraft.world.InteractionHand hand) {
        ItemStack held = p.getItemInHand(hand);
        ItemStack old = worn(p);
        p.setItemSlot(SLOT, held.copy());
        p.setItemInHand(hand, old);
    }

    /** A one-slot view of the worn backpack, for the inventory screen. */
    public static final class WornContainer implements Container {
        private final Player player;

        public WornContainer(Player player) { this.player = player; }

        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return worn(player).isEmpty(); }
        @Override public ItemStack getItem(int i) { return worn(player); }
        @Override public ItemStack removeItem(int i, int n) {
            ItemStack s = worn(player);
            if (s.isEmpty() || n <= 0) return ItemStack.EMPTY;
            player.setItemSlot(SLOT, ItemStack.EMPTY);
            return s;
        }
        @Override public ItemStack removeItemNoUpdate(int i) { return removeItem(i, 1); }
        @Override public void setItem(int i, ItemStack s) { player.setItemSlot(SLOT, s); }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player p) { return p == player; }
        @Override public boolean canPlaceItem(int i, ItemStack s) { return isBackpack(s); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public void clearContent() { player.setItemSlot(SLOT, ItemStack.EMPTY); }
    }

    /** The inventory-screen slot. */
    public static final class WornSlot extends Slot {
        public WornSlot(Player player, int x, int y) {
            super(new WornContainer(player), 0, x, y);
        }

        @Override public boolean mayPlace(ItemStack s) { return isBackpack(s); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public Identifier getNoItemIcon() { return EMPTY_ICON; }
    }
}
