package com.stormreach.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.Supplier;

/**
 * Storm Capacitor: a bank of glass Leyden jars that stores 100,000 SU. Lightning striking within 24 blocks fills half
 * of it at once. It feeds consumers on its network, and comparators read how full it is; the jars glow as it fills.
 */
public class StormCapacitorBlock extends NodeBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);

    public StormCapacitorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override protected Supplier<? extends BlockEntityType<? extends NodeEntity>> type() { return () -> PowerRegistry.CAPACITOR_ENTITY; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LEVEL); }
    @Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos, Direction d) {
        return level.getBlockEntity(pos) instanceof Entity be ? (int) Math.ceil(be.stored() * 15.0 / be.capacity()) : 0;
    }

    public static class Entity extends NodeEntity {
        public static final int CAPACITY = 100_000;

        public Entity(BlockPos pos, BlockState state) {
            super(PowerRegistry.CAPACITOR_ENTITY, pos, state, Role.STORAGE, CAPACITY);
        }

        @Override
        public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 10 != 0) return;
            PowerNet.share(level, pos, this);
            int vis = stored == 0 ? 0 : 1 + Math.min(3, stored * 4 / CAPACITY);
            if (state.getValue(LEVEL) != vis) level.setBlock(pos, state.setValue(LEVEL, vis), Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }

        /** Lightning struck at {@code hit}: every capacitor within 24 blocks soaks up half its capacity. */
        public static void onLightning(ServerLevel level, BlockPos hit) {
            int cx = hit.getX() >> 4, cz = hit.getZ() >> 4;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof Entity cap && be.getBlockPos().distSqr(hit) <= 24 * 24) {
                        cap.insert(CAPACITY / 2);
                        BlockPos p = be.getBlockPos();
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.3);
                        level.playSound(null, p, com.stormreach.registry.ModSfx.BLOCK_CAPACITOR_CHARGE, SoundSource.BLOCKS, 1.2f, 1.0f);
                    }
                }
            }
        }
    }
}
