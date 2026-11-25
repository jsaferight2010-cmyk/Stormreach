package com.stormreach.tornado.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Exists so the client can draw the radar's rotating dish. It stores nothing: the dish angle comes from the game time. */
public class DopplerRadarBlockEntity extends BlockEntity {
    public DopplerRadarBlockEntity(BlockPos pos, BlockState state) {
        super(RadarContent.RADAR_ENTITY, pos, state);
    }
}
