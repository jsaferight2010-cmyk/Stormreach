package com.stormreach.tornado.radar;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The Doppler radar's footprint, relative to the centre of its base: a 3x3 concrete pad, a three-block mast, the
 * pedestal (the controller block, four up) and the space the dish sweeps through (3x3 at heights 4 to 6).
 */
public final class RadarLayout {
    private RadarLayout() {}

    public static final int WIDTH = 3, HEIGHT = 7;
    /** Controller (pedestal) offset from the base centre. */
    public static final BlockPos CONTROLLER = new BlockPos(0, 4, 0);

    public enum Part implements net.minecraft.util.StringRepresentable {
        BASE, MAST, DISH;

        @Override
        public String getSerializedName() { return name().toLowerCase(); }
    }

    public record Cell(BlockPos offset, Part part) {}

    /** Every non-controller cell of the structure. */
    public static List<Cell> parts() {
        List<Cell> out = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) out.add(new Cell(new BlockPos(x, 0, z), Part.BASE));
        for (int y = 1; y <= 3; y++) out.add(new Cell(new BlockPos(0, y, 0), Part.MAST));
        for (int y = 4; y <= 6; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (y == 4 && x == 0 && z == 0) continue; // the controller
            out.add(new Cell(new BlockPos(x, y, z), Part.DISH));
        }
        return out;
    }

    /** Every cell the structure occupies, controller included. */
    public static List<BlockPos> footprint(BlockPos baseCentre) {
        List<BlockPos> out = new ArrayList<>();
        for (Cell c : parts()) out.add(baseCentre.offset(c.offset()));
        out.add(baseCentre.offset(CONTROLLER));
        return out;
    }
}
