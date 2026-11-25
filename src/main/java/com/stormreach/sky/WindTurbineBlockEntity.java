package com.stormreach.sky;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The turbine's block entity: it exists so the client can draw the spinning rotor; the wind sets the POWER state. */
public class WindTurbineBlockEntity extends BlockEntity {
    /** Client-side rotor angle (radians) and the game time it was last advanced. */
    public float spin;
    public long spunAt = -1;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(SkyRegistry.TURBINE_ENTITY, pos, state);
    }

    static void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
        double[] wind = com.stormreach.tornado.Instruments.windMph(level, pos.above());
        int power = WindTurbineBlock.powerFor(wind[0], level.canSeeSky(pos.above()));
        if (power != state.getValue(WindTurbineBlock.POWER)) level.setBlock(pos, state.setValue(WindTurbineBlock.POWER, power), Block.UPDATE_ALL);
    }
}
