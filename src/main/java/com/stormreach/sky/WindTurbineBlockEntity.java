package com.stormreach.sky;

import com.stormreach.power.PowerNet;
import com.stormreach.power.PowerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The turbine's block entity. The wind sets the POWER state (a redstone signal), and since 5.5 the turbine is also a
 * generator: 2 SU a tick per level of POWER (up to 30), sent along any Copper Cable it touches. The client keeps the
 * rotor angle here for the renderer.
 */
public class WindTurbineBlockEntity extends BlockEntity implements PowerNode {
    public static final int CAPACITY = 4000;
    /** Client-side rotor angle (radians) and the game time it was last advanced. */
    public float spin;
    public long spunAt = -1;
    private int stored, rate;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(SkyRegistry.TURBINE_ENTITY, pos, state);
    }

    @Override public Role role() { return Role.GENERATOR; }
    @Override public int stored() { return stored; }
    @Override public int capacity() { return CAPACITY; }
    @Override public int rate() { return rate; }
    @Override public int insert(int amount) { int n = Math.max(0, Math.min(amount, CAPACITY - stored)); stored += n; setChanged(); return n; }
    @Override public int extract(int amount) { int n = Math.max(0, Math.min(amount, stored)); stored -= n; setChanged(); return n; }

    static void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
        double[] wind = com.stormreach.tornado.Instruments.windMph(level, pos.above());
        int power = WindTurbineBlock.powerFor(wind[0], level.canSeeSky(pos.above()));
        if (power != state.getValue(WindTurbineBlock.POWER)) level.setBlock(pos, state.setValue(WindTurbineBlock.POWER, power), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof WindTurbineBlockEntity be) {
            be.rate = power * 2;
            be.insert(be.rate * 20);
            PowerNet.share(level, pos, be);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("SU", stored);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stored = in.getIntOr("SU", 0);
    }
}
