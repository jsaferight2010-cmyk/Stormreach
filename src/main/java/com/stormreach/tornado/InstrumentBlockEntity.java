package com.stormreach.tornado;

import com.stormreach.structure.StructureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 6.0: a data-less block entity for the instruments that are drawn as whole machines (the tornado siren on its pole,
 * the weather station mast). It exists so the client can attach a renderer; the readings still come from Instruments.
 * The client keeps the rotor and siren angles here between frames.
 */
public class InstrumentBlockEntity extends BlockEntity {
    public float spin, sirenYaw;
    public long animAt = -1;

    public InstrumentBlockEntity(BlockPos pos, BlockState state) {
        super(StructureRegistry.INSTRUMENT_ENTITY, pos, state);
    }
}
