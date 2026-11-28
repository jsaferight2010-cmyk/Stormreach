package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The wiring: every power node touching the same run of Copper Cable (or touching the source directly) is on one
 * network. Generators and capacitors call {@link #share} every so often; nothing is stored in the cables.
 */
public final class PowerNet {
    public static final int MAX_CABLES = 1024;

    private PowerNet() {}

    /** Every other power node reachable from {@code origin} through adjacent blocks and connected cable. */
    public static List<PowerNode> reach(ServerLevel level, BlockPos origin) {
        Set<PowerNode> nodes = new LinkedHashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        int cables = 0;
        while (!queue.isEmpty() && cables < MAX_CABLES) {
            BlockPos at = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                if (!seen.add(n) || !level.isLoaded(n)) continue;
                if (level.getBlockState(n).getBlock() instanceof CopperCableBlock) { queue.add(n); cables++; continue; }
                BlockEntity be = level.getBlockEntity(n);
                if (be instanceof PowerNode p) nodes.add(p);
            }
        }
        BlockEntity self = level.getBlockEntity(origin);
        nodes.remove(self);
        return new ArrayList<>(nodes);
    }

    /** Number of cable blocks in the run touching {@code origin} (for the Multimeter). */
    public static int cableCount(ServerLevel level, BlockPos origin) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin); seen.add(origin);
        int n = 0;
        while (!queue.isEmpty() && n < MAX_CABLES) {
            BlockPos at = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos p = at.relative(d);
                if (seen.add(p) && level.getBlockState(p).getBlock() instanceof CopperCableBlock) { queue.add(p); n++; }
            }
        }
        return n;
    }

    /** Pushes {@code src}'s charge out: consumers first, then (from a generator) storage. */
    public static void share(ServerLevel level, BlockPos pos, PowerNode src) {
        if (src.stored() <= 0) return;
        List<PowerNode> nodes = reach(level, pos);
        if (nodes.isEmpty()) return;
        Collections.shuffle(nodes, new java.util.Random(level.getGameTime()));
        push(src, nodes, PowerNode.Role.CONSUMER);
        if (src.role() == PowerNode.Role.GENERATOR) push(src, nodes, PowerNode.Role.STORAGE);
    }

    private static void push(PowerNode src, List<PowerNode> nodes, PowerNode.Role role) {
        List<PowerNode> targets = nodes.stream().filter(n -> n.role() == role).toList();
        for (int i = 0; i < targets.size() && src.stored() > 0; i++) {
            PowerNode n = targets.get(i);
            int fair = Math.max(1, src.stored() / (targets.size() - i));
            int moved = n.insert(Math.min(fair, n.capacity() - n.stored()));
            src.extract(moved);
        }
    }
}
