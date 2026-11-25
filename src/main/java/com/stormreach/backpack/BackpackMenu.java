package com.stormreach.backpack;

import com.stormreach.sim.BackpackTier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The backpack screen's menu: a chest-style grid showing one page (up to 54 slots) of the pack. Page changes
 * use vanilla menu button clicks, so no custom packets are needed. Duplication guards:
 * the open backpack's own inventory slot is locked (no clicking, dragging, number-key or offhand swaps),
 * backpacks can't be put inside backpacks, the menu closes if the pack leaves its slot, and every change is
 * written to the item immediately.
 */
public class BackpackMenu extends ChestMenu {
    public static final int BUTTON_PREV = 0, BUTTON_NEXT = 1, BUTTON_SORT = 2;
    private final PageView view;
    private final ItemStack stack;
    private final int lockedInventorySlot;
    private final Player owner;
    private final DataSlot page = DataSlot.standalone();
    private final DataSlot pages = DataSlot.standalone();
    private final DataSlot sortable = DataSlot.standalone();

    /** Client constructor. */
    public BackpackMenu(MenuType<?> type, int id, Inventory inv, int rows) {
        this(type, id, inv, new PageView(null, NonNullList.withSize(rows * 9, ItemStack.EMPTY), rows * 9), rows, ItemStack.EMPTY, -1, false);
    }

    private BackpackMenu(MenuType<?> type, int id, Inventory inv, PageView view, int rows, ItemStack stack, int lockedSlot, boolean canSort) {
        super(type, id, inv, view, rows);
        this.view = view;
        this.stack = stack;
        this.owner = inv.player;
        this.lockedInventorySlot = lockedSlot;
        addDataSlot(page);
        addDataSlot(pages);
        addDataSlot(sortable);
        page.set(0);
        pages.set(BackpackTier.pages(view.capacity));
        sortable.set(canSort ? 1 : 0);
        for (int i = 0; i < rows * 9; i++) {
            Slot old = slots.get(i);
            Slot s = new Slot(view, old.getContainerSlot(), old.x, old.y) {
                @Override public boolean mayPlace(ItemStack s) { return view.exists(getContainerSlot()) && !(s.getItem() instanceof BackpackItem); }
                @Override public boolean mayPickup(Player p) { return view.exists(getContainerSlot()); }
                @Override public boolean isActive() { return view.exists(getContainerSlot()); }
            };
            s.index = i;
            slots.set(i, s);
        }
        if (lockedSlot >= 0) {
            for (int i = rows * 9; i < slots.size(); i++) {
                Slot old = slots.get(i);
                if (old.container == inv && old.getContainerSlot() == lockedSlot) {
                    Slot locked = new Slot(inv, lockedSlot, old.x, old.y) {
                        @Override public boolean mayPickup(Player p) { return false; }
                        @Override public boolean mayPlace(ItemStack s) { return false; }
                    };
                    locked.index = i;
                    slots.set(i, locked);
                }
            }
        }
    }

    public static BackpackMenu server(MenuType<?> type, int id, Inventory inv, ItemStack stack, int slot, int capacity, boolean canSort) {
        BackpackStorage storage = BackpackStorage.read(stack, capacity);
        int rows = BackpackTier.rows(capacity);
        PageView view = new PageView(stack, storage.items, capacity);
        return new BackpackMenu(type, id, inv, view, rows, stack, slot, canSort);
    }

    public boolean isFor(ItemStack s) {
        return s == stack;
    }

    public int page() { return page.get(); }
    public int pages() { return Math.max(1, pages.get()); }
    public boolean canSort() { return sortable.get() == 1; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_PREV && page.get() > 0) page.set(page.get() - 1);
        else if (id == BUTTON_NEXT && page.get() + 1 < pages()) page.set(page.get() + 1);
        else if (id == BUTTON_SORT && canSort()) { BackpackStorage.sort(view.backing); view.setChanged(); }
        else return false;
        view.page = page.get();
        broadcastChanges();
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (lockedInventorySlot >= 0) {
            if (input == ContainerInput.SWAP && (button == lockedInventorySlot || (button == 40 && lockedInventorySlot == 40))) return;
            if (slotIndex >= 0 && slotIndex < slots.size()) {
                Slot s = slots.get(slotIndex);
                if (s.container == player.getInventory() && s.getContainerSlot() == lockedInventorySlot) return;
            }
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (stack.isEmpty() && lockedInventorySlot == -1) return true; // client side
        if (lockedInventorySlot == BackpackEquipment.WORN) return player.isAlive() && BackpackEquipment.worn(player) == stack;
        return player.isAlive() && player.getInventory().getItem(lockedInventorySlot) == stack;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot from = slots.get(index);
        if (from.getItem().getItem() instanceof BackpackItem && index >= view.getContainerSize()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!stack.isEmpty() && !player.level().isClientSide()) {
            if (BackpackEnchants.level(player.level(), stack, BackpackEnchants.ORGANIZATION) > 0) BackpackStorage.sort(view.backing);
            view.setChanged();
        }
    }

    /** One page of the pack, exposed as a plain container. */
    static final class PageView implements Container {
        final ItemStack stack;
        final NonNullList<ItemStack> backing;
        final int capacity;
        int page;
        private final int visible;

        PageView(ItemStack stack, NonNullList<ItemStack> backing, int capacity) {
            this.stack = stack;
            this.backing = backing;
            this.capacity = capacity;
            this.visible = BackpackTier.rows(capacity) * 9;
        }

        boolean exists(int i) {
            return stack == null || BackpackTier.backingIndex(capacity, page, i) >= 0;
        }

        private int idx(int i) {
            return stack == null ? i : BackpackTier.backingIndex(capacity, page, i);
        }

        @Override public int getContainerSize() { return visible; }
        @Override public boolean isEmpty() { return backing.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int i) { int k = idx(i); return k < 0 || k >= backing.size() ? ItemStack.EMPTY : backing.get(k); }
        @Override public ItemStack removeItem(int i, int n) {
            int k = idx(i);
            if (k < 0) return ItemStack.EMPTY;
            ItemStack r = backing.get(k).split(n);
            setChanged();
            return r;
        }
        @Override public ItemStack removeItemNoUpdate(int i) {
            int k = idx(i);
            if (k < 0) return ItemStack.EMPTY;
            ItemStack r = backing.get(k);
            backing.set(k, ItemStack.EMPTY);
            return r;
        }
        @Override public void setItem(int i, ItemStack s) {
            int k = idx(i);
            if (k < 0) return;
            backing.set(k, s);
            setChanged();
        }
        @Override public void setChanged() {
            if (stack != null && !stack.isEmpty()) new BackpackWriter(backing).write(stack);
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public boolean canPlaceItem(int i, ItemStack s) { return exists(i) && !(s.getItem() instanceof BackpackItem); }
        @Override public void clearContent() { backing.clear(); setChanged(); }
    }

    private record BackpackWriter(NonNullList<ItemStack> items) {
        void write(ItemStack stack) {
            stack.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(items));
        }
    }

    @SuppressWarnings("unused")
    private static Container dummy(int n) {
        return new SimpleContainer(n);
    }
}
