package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.Supplier;

/** Arc Lamp: a caged electric lamp, as bright as a block can be, while it has power (1 SU a tick). */
public class ArcLampBlock extends NodeBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int USE = 1;

    public ArcLampBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected Supplier<? extends BlockEntityType<? extends NodeEntity>> type() { return () -> PowerRegistry.ARC_LAMP_ENTITY; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }

    public static class Entity extends NodeEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(PowerRegistry.ARC_LAMP_ENTITY, pos, state, Role.CONSUMER, 400);
        }

        @Override public int rate() { return USE; }

        @Override
        public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 10 != 0) return;
            boolean on = spend(USE * 10);
            if (state.getValue(LIT) != on) level.setBlock(pos, state.setValue(LIT, on), Block.UPDATE_ALL);
        }
    }
}
