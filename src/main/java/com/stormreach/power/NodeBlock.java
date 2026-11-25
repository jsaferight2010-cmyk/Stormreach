package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** A block whose block entity is a {@link NodeEntity}; ticks it on the server. */
public abstract class NodeBlock extends Block implements EntityBlock {
    protected NodeBlock(Properties p) {
        super(p);
    }

    protected abstract Supplier<? extends BlockEntityType<? extends NodeEntity>> type();

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type().get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> t) {
        return level.isClientSide() || t != type().get() ? null : (l, p, st, be) -> ((NodeEntity) be).serverTick((ServerLevel) l, p, st);
    }
}
