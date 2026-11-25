package com.stormreach.tornado.radar;

import com.stormreach.tornado.WeatherItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Building, checking and removing the Doppler radar structure. */
public final class RadarStructure {
    private RadarStructure() {}

    /** Cells that block placement at this base centre (empty means it fits). */
    public static List<BlockPos> blocked(Level level, BlockPos baseCentre) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : RadarLayout.footprint(baseCentre)) {
            BlockState s = level.getBlockState(p);
            if (!s.canBeReplaced() || !level.isInWorldBounds(p)) out.add(p);
        }
        return out;
    }

    /** Builds the structure. The caller has checked {@link #blocked}. */
    public static void build(Level level, BlockPos baseCentre) {
        // parts first without shape updates (they would see no controller yet and remove themselves), then the controller
        for (RadarLayout.Cell c : RadarLayout.parts()) level.setBlock(baseCentre.offset(c.offset()), RadarPartBlock.stateFor(c), 2 | 16);
        level.setBlock(baseCentre.offset(RadarLayout.CONTROLLER), WeatherItems.TORNADO_RADAR.defaultBlockState(), 3);
    }

    /** Removes the parts around a controller (no drops: the controller drops the radar). */
    public static void removeParts(Level level, BlockPos controller) {
        BlockPos base = controller.subtract(RadarLayout.CONTROLLER);
        for (RadarLayout.Cell c : RadarLayout.parts()) {
            BlockPos p = base.offset(c.offset());
            if (level.getBlockState(p).is(RadarContent.RADAR_PART)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /** True if a complete radar stands with its controller here. */
    public static boolean complete(Level level, BlockPos controller) {
        if (!level.getBlockState(controller).is(WeatherItems.TORNADO_RADAR)) return false;
        BlockPos base = controller.subtract(RadarLayout.CONTROLLER);
        for (RadarLayout.Cell c : RadarLayout.parts()) if (!level.getBlockState(base.offset(c.offset())).is(RadarContent.RADAR_PART)) return false;
        return true;
    }
}
