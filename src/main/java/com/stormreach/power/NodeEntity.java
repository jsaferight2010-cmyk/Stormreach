package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Shared plumbing for the Live Wire block entities: an SU buffer that saves with the block, and a server tick. */
public abstract class NodeEntity extends BlockEntity implements PowerNode {
    protected int stored;
    private final int capacity;
    private final Role role;

    protected NodeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Role role, int capacity) {
        super(type, pos, state);
        this.role = role;
        this.capacity = capacity;
    }

    @Override public Role role() { return role; }
    @Override public int stored() { return stored; }
    @Override public int capacity() { return capacity; }

    @Override
    public int insert(int amount) {
        int n = Math.max(0, Math.min(amount, capacity - stored));
        if (n > 0) { stored += n; setChanged(); }
        return n;
    }

    @Override
    public int extract(int amount) {
        int n = Math.max(0, Math.min(amount, stored));
        if (n > 0) { stored -= n; setChanged(); }
        return n;
    }

    /** Spends {@code amount} if it is all there; false (and nothing spent) otherwise. */
    public boolean spend(int amount) {
        if (stored < amount) return false;
        stored -= amount;
        setChanged();
        return true;
    }

    public abstract void serverTick(ServerLevel level, BlockPos pos, BlockState state);

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("SU", stored);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stored = Math.min(capacity, in.getIntOr("SU", 0));
    }
}
