package com.stormreach.client.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/** One row in a Stormreach JEI category: what goes in, what comes out, and a short note on how. */
public record JeiRow(List<Part> inputs, List<Part> outputs, List<Component> notes) {
    /** An item stack, or a fluid amount in millibuckets. */
    public record Part(ItemStack stack, Fluid fluid, int mb) {
        public static Part of(ItemLike item) { return new Part(new ItemStack(item), null, 0); }
        public static Part of(ItemStack stack) { return new Part(stack, null, 0); }
        public static Part fluid(Fluid f, int mb) { return new Part(ItemStack.EMPTY, f, mb); }
    }

    public static JeiRow of(List<Part> in, List<Part> out, Component... notes) {
        return new JeiRow(in, out, List.of(notes));
    }
}
