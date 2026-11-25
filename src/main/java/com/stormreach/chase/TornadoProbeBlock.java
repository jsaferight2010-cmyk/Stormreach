package com.stormreach.chase;

import com.stormreach.progress.StormAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tornado Probe: a squat, weighted instrument barrel (too heavy and anchored for any tornado to move). Set it down in
 * a tornado's path; when the tornado passes over it, it records the strongest wind it felt. Its light turns red when
 * it has a reading; right-click to read it (which also logs it in your Storm Chaser's Logbook).
 */
public class TornadoProbeBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RECORDED = BooleanProperty.create("recorded");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public TornadoProbeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RECORDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, RECORDED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TornadoProbeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        return level.isClientSide() || type != ChaseRegistry.PROBE_ENTITY ? null
                : (l, p, st, be) -> ((TornadoProbeBlockEntity) be).serverTick((net.minecraft.server.level.ServerLevel) l, p, st);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof TornadoProbeBlockEntity probe)) return InteractionResult.SUCCESS;
        if (probe.peakMph() < 40) {
            player.sendOverlayMessage(Component.translatable("block.stormreach.tornado_probe.armed").withStyle(ChatFormatting.GRAY));
        } else {
            var ef = com.stormreach.sim.EFScale.fromGustMph(probe.peakMph());
            player.sendSystemMessage(Component.translatable("block.stormreach.tornado_probe.reading", Math.round(probe.peakMph()),
                    ef == null ? "-" : ef.name(), Math.round(probe.closest()), probe.day()).withStyle(ChatFormatting.GOLD));
            if (player instanceof ServerPlayer sp && probe.peakMph() >= 65) {
                StormAdvancements.award(sp, "probe_hit");
                if (probe.peakMph() >= 166) StormAdvancements.award(sp, "probe_violent");
                ChaserLog.recordProbe(sp, probe);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level l, BlockPos p, Direction d) {
        return l.getBlockEntity(p) instanceof TornadoProbeBlockEntity probe ? (int) Math.min(15, probe.peakMph() / 15) : 0;
    }
}
