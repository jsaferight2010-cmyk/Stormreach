package com.stormreach.blight;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.HeartFrontier;
import com.stormreach.block.StormheartBlock;
import com.stormreach.particle.ModParticles;
import com.stormreach.power.PowerNet;
import com.stormreach.power.PowerNode;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 6.0: the Stormheart's overcharge. Once an awake heart has grown to its normal limit, feeding it storm power (SU, from
 * a Storm Capacitor or generators on Copper Cable touching it) pushes the Stormblight further, with no limit but the
 * world border (or {@code heartPoweredMaxRadius}). The spread is a managed frontier: a ring that moves out a block at a
 * time over loaded land. Where the ring passes chunks nobody has loaded, they are remembered and converted when they
 * load, so the blight never leaves holes. Cut the power and it stops where it is; Salt still calms the heart.
 */
public class StormheartBlockEntity extends BlockEntity {
    private static final int MAX_PENDING = 4096;

    private int frontier;              // 0 until the heart first overcharges
    private double progress;
    private boolean powered;
    private final java.util.LinkedHashSet<Long> pending = new java.util.LinkedHashSet<>();

    public StormheartBlockEntity(BlockPos pos, BlockState state) {
        super(HeartRegistry.HEART_ENTITY, pos, state);
    }

    public int frontier() { return frontier; }
    public boolean powered() { return powered; }
    public int pendingChunks() { return pending.size(); }

    /** Takes {@code amount} SU from the network around the heart: storage first, then generators. All or nothing. */
    static boolean drawPower(ServerLevel level, BlockPos pos, int amount) {
        List<PowerNode> net = PowerNet.reach(level, pos);
        int available = 0;
        for (PowerNode n : net) if (n.role() != PowerNode.Role.CONSUMER) available += n.stored();
        if (available < amount) return false;
        int need = amount;
        for (PowerNode.Role role : new PowerNode.Role[]{PowerNode.Role.STORAGE, PowerNode.Role.GENERATOR}) {
            for (PowerNode n : net) {
                if (need <= 0) return true;
                if (n.role() == role) need -= n.extract(need);
            }
        }
        return need <= 0;
    }

    public static void tickFrom(ServerLevel level, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be instanceof StormheartBlockEntity h) serverTick(level, pos, state, h);
    }

    static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StormheartBlockEntity be) {
        if (level.getGameTime() % 20 != Math.floorMod(pos.hashCode(), 20)) return;   // once a second, staggered
        cycle(level, pos, state, be);
    }

    /** One second of the overcharge. Public for GameTests. */
    public static void cycle(ServerLevel level, BlockPos pos, BlockState state, StormheartBlockEntity be) {
        boolean grown = StormheartBlock.radius(state) >= StormreachConfig.heartMaxRadius;
        if (!state.getValue(StormheartBlock.ACTIVE) || !grown || !StormreachConfig.heartPoweredSpread
                || StormreachConfig.spreadMode == StormreachConfig.SpreadMode.OFF) {
            be.setPowered(false);
            return;
        }
        if (!drawPower(level, pos, StormreachConfig.heartPowerDraw)) { be.setPowered(false); return; }
        be.setPowered(true);
        if (be.frontier < StormreachConfig.heartMaxRadius) be.frontier = StormreachConfig.heartMaxRadius;
        be.advance(level, pos, level.getRandom());
        be.catchUp(level, pos, level.getRandom());
        be.setChanged();
        // the overcharge is visible and audible: arcs off the heart and a deeper hum
        level.sendParticles(ModParticles.STORM_SPARK, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 14, 0.6, 0.6, 0.6, 0.35);
        if (level.getRandom().nextInt(4) == 0) level.playSound(null, pos, ModSfx.MAGIC_HUM, SoundSource.BLOCKS, 1.4f, 0.35f);
    }

    private void setPowered(boolean p) {
        if (powered != p) { powered = p; setChanged(); }
    }

    /** Samples the ring and moves it out once it has been covered. */
    private void advance(ServerLevel level, BlockPos pos, RandomSource rnd) {
        int columns = HeartFrontier.columnsPerCycle(StormreachConfig.spreadSpeed);
        for (int i = 0; i < columns; i++) {
            double r = HeartFrontier.sampleRadius(frontier, Math.sqrt(rnd.nextDouble()));
            double a = rnd.nextDouble() * Math.PI * 2;
            int x = pos.getX() + (int) Math.round(Math.cos(a) * r), z = pos.getZ() + (int) Math.round(Math.sin(a) * r);
            convertColumn(level, x, z, rnd, true);
        }
        if (rnd.nextInt(3) == 0) {
            double a = rnd.nextDouble() * Math.PI * 2;
            BlockPos at = pos.offset((int) (Math.cos(a) * frontier), 0, (int) (Math.sin(a) * frontier));
            if (level.isLoaded(at)) Stormblight.convertOneMob(level, new AABB(at).inflate(12, 24, 12), rnd);
        }
        progress += columns;
        while (progress >= HeartFrontier.samplesPerStep(frontier) && HeartFrontier.mayGrow(frontier, StormreachConfig.heartPoweredMaxRadius)) {
            progress -= HeartFrontier.samplesPerStep(frontier);
            frontier++;
            if (frontier % 8 == 0) blightRing(level, pos);
        }
    }

    /** Converts the surface of one column (if loaded), or remembers its chunk for later. */
    private void convertColumn(ServerLevel level, int x, int z, RandomSource rnd, boolean remember) {
        BlockPos probe = new BlockPos(x, worldPosition.getY(), z);
        if (!level.getWorldBorder().isWithinBounds(probe)) return;
        if (!level.hasChunk(x >> 4, z >> 4)) {
            if (remember && pending.size() < MAX_PENDING) pending.add(ChunkPos.pack(x >> 4, z >> 4));
            return;
        }
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int dy = -4; dy <= 0; dy++) Stormblight.convertBlock(level, new BlockPos(x, top + dy, z), rnd);
    }

    /** The biome follows the frontier: every 8 blocks of growth the ring's loaded chunks become storm biome. */
    private void blightRing(ServerLevel level, BlockPos pos) {
        java.util.LinkedHashSet<Long> chunks = new java.util.LinkedHashSet<>();
        int steps = Math.max(8, (int) (2 * Math.PI * frontier / 12));
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int x = pos.getX() + (int) Math.round(Math.cos(a) * (frontier - 4)), z = pos.getZ() + (int) Math.round(Math.sin(a) * (frontier - 4));
            long c = ChunkPos.pack(x >> 4, z >> 4);
            chunks.add(c);
        }
        for (long c : chunks) {
            int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
            if (level.hasChunk(cx, cz)) Stormblight.blightChunk(level, new ChunkPos(cx, cz));
            else if (pending.size() < MAX_PENDING) pending.add(c);
        }
    }

    /** Remembered chunks that have loaded since: convert the part of them inside the frontier. */
    private void catchUp(ServerLevel level, BlockPos pos, RandomSource rnd) {
        if (pending.isEmpty()) return;
        int handled = 0;
        var it = pending.iterator();
        int checked = 0;
        while (it.hasNext() && handled < 2 && checked < 32) {
            long c = it.next();
            checked++;
            int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
            if (!level.hasChunk(cx, cz)) continue;
            it.remove();
            handled++;
            ChunkPos cp = new ChunkPos(cx, cz);
            for (int i = 0; i < 48; i++) {
                int x = cp.getMinBlockX() + rnd.nextInt(16), z = cp.getMinBlockZ() + rnd.nextInt(16);
                double d = Math.hypot(x - pos.getX(), z - pos.getZ());
                if (d <= frontier) convertColumn(level, x, z, rnd, false);
            }
            if (Math.hypot(cp.getMiddleBlockX() - pos.getX(), cp.getMiddleBlockZ() - pos.getZ()) <= frontier) Stormblight.blightChunk(level, cp);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("frontier", frontier);
        out.putDouble("progress", progress);
        out.putBoolean("powered", powered);
        int[] packed = new int[pending.size() * 2];
        int i = 0;
        for (long c : pending) { packed[i++] = ChunkPos.getX(c); packed[i++] = ChunkPos.getZ(c); }
        out.putIntArray("pending", packed);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        frontier = in.getIntOr("frontier", 0);
        progress = in.getDoubleOr("progress", 0);
        powered = in.getBooleanOr("powered", false);
        pending.clear();
        in.getIntArray("pending").ifPresent(a -> {
            for (int i = 0; i + 1 < a.length && pending.size() < MAX_PENDING; i += 2) pending.add(ChunkPos.pack(a[i], a[i + 1]));
        });
    }
}
