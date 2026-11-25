package com.stormreach.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A backpack's contents as a fixed-size list, read from and written back to the item's container component. */
public final class BackpackStorage {
    public final NonNullList<ItemStack> items;

    private BackpackStorage(int size) {
        items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public static BackpackStorage read(ItemStack backpack, int size) {
        BackpackStorage s = new BackpackStorage(size);
        ItemContainerContents c = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> tmp = NonNullList.withSize(Math.max(size, c.size()), ItemStack.EMPTY);
        c.copyInto(tmp);
        for (int i = 0; i < tmp.size(); i++) {
            if (i < size) s.items.set(i, tmp.get(i));
            else if (!tmp.get(i).isEmpty()) {
                // never delete items if a pack somehow shrank: pack overflow into free slots
                ItemStack rest = s.insert(tmp.get(i));
                if (!rest.isEmpty()) s.items.add(rest);
            }
        }
        return s;
    }

    public void write(ItemStack backpack) {
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** Merges into matching stacks, then empty slots. Returns what didn't fit. */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof BackpackItem) return stack;
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }

    /** Merges partial stacks and sorts by item id, then by count (largest first). Never changes the item total. */
    public static void sort(NonNullList<ItemStack> list) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack s : list) {
            if (s.isEmpty()) continue;
            ItemStack rest = s.copy();
            for (ItemStack m : merged) {
                if (rest.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(m, rest) && m.getCount() < m.getMaxStackSize()) {
                    int move = Math.min(rest.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(move);
                    rest.shrink(move);
                }
            }
            if (!rest.isEmpty()) merged.add(rest);
        }
        merged.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> -s.getCount()));
        for (int i = 0; i < list.size(); i++) list.set(i, i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
    }
}
