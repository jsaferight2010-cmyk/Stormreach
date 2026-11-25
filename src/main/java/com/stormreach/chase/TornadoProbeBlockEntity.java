package com.stormreach.chase;

import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** What a Tornado Probe has recorded: its peak wind (mph), the closest the core came (blocks), and on which day. */
public class TornadoProbeBlockEntity extends BlockEntity {
    private float peakMph;
    private float closest = 9999;
    private long day;
    private final java.util.Set<java.util.UUID> logged = new java.util.HashSet<>();

    public TornadoProbeBlockEntity(BlockPos pos, BlockState state) {
        super(ChaseRegistry.PROBE_ENTITY, pos, state);
    }

    public float peakMph() { return peakMph; }
    public float closest() { return closest; }
    public long day() { return day; }

    /** Each player logs a probe's reading once. */
    public boolean firstReadBy(java.util.UUID player) { return logged.add(player); }

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 4 != 0) return;
        Vec3 c = Vec3.atCenterOf(pos);
        for (TornadoEntity t : TornadoManager.live(level)) {
            double d = Math.hypot(t.getX() - c.x, t.getZ() - c.z);
            if (d > 120) continue;
            double mph = t.windMphAt(c.add(0, 2, 0));
            if (mph > peakMph) {
                peakMph = (float) mph;
                day = level.getOverworldClockTime() / 24000L + 1;
                setChanged();
            }
            if (d < closest) { closest = (float) d; setChanged(); }
            if (peakMph >= 65 && !state.getValue(TornadoProbeBlock.RECORDED)) {
                level.setBlock(pos, state.setValue(TornadoProbeBlock.RECORDED, true), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("Peak", peakMph);
        out.putFloat("Closest", closest);
        out.putLong("Day", day);
        out.putString("Logged", String.join(",", logged.stream().map(Object::toString).toList()));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        peakMph = in.getFloatOr("Peak", 0);
        closest = in.getFloatOr("Closest", 9999);
        day = in.getLongOr("Day", 0);
        logged.clear();
        for (String s : in.getStringOr("Logged", "").split(",")) {
            try { if (!s.isEmpty()) logged.add(java.util.UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }
}
