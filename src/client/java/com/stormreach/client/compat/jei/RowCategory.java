package com.stormreach.client.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * A plain JEI category: up to four inputs on the left, an arrow, up to six outputs on the right, and the notes under
 * them (word-wrapped by JEI). Fluids show in their own slots in millibuckets.
 */
public class RowCategory implements IRecipeCategory<JeiRow> {
    public static final int WIDTH = 178;
    /** Fabric's fluid unit: 81000 droplets to the bucket. */
    static final long DROPLETS_PER_MB = 81;

    private final IRecipeType<JeiRow> type;
    private final Component title;
    private final IDrawable icon;
    private final int height;

    public RowCategory(IRecipeType<JeiRow> type, Component title, IGuiHelper gui, ItemLike icon, int height) {
        this.type = type;
        this.title = title;
        this.icon = gui.createDrawableItemLike(icon);
        this.height = height;
    }

    @Override public IRecipeType<JeiRow> getRecipeType() { return type; }
    @Override public Component getTitle() { return title; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return height; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, JeiRow row, IFocusGroup focuses) {
        for (int i = 0; i < Math.min(4, row.inputs().size()); i++) slot(b.addInputSlot(1 + i * 19, 1), row.inputs().get(i));
        int n = Math.min(6, row.outputs().size());
        int x0 = WIDTH - 1 - n * 19 + 1;
        for (int i = 0; i < n; i++) {
            IRecipeSlotBuilder s = b.addOutputSlot(x0 + i * 19, 1);
            slot(s, row.outputs().get(i));
            s.setOutputSlotBackground();
        }
    }

    private static void slot(IRecipeSlotBuilder s, JeiRow.Part p) {
        s.setStandardSlotBackground();
        if (p.fluid() != null) {
            s.setFluidRenderer(Math.max(1000, p.mb()) * DROPLETS_PER_MB, false, 16, 16);
            s.add(p.fluid(), p.mb() * DROPLETS_PER_MB);
        } else {
            s.add(p.stack());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, JeiRow row, IFocusGroup focuses) {
        int ins = Math.min(4, row.inputs().size());
        int outs = Math.min(6, row.outputs().size());
        if (ins > 0 && outs > 0) {
            int left = 1 + ins * 19, right = WIDTH - outs * 19;
            b.addRecipeArrowWidget().setPosition(left + Math.max(0, (right - left - 22) / 2), 1);
        }
        List<FormattedText> lines = new ArrayList<>(row.notes());
        if (!lines.isEmpty()) b.addText(lines, WIDTH - 2, height - 22).setPosition(1, 21).setColor(0xFF4A4A4A).setShadow(false);
    }
}
