package com.stormreach.sky;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wind Turbine: a mast with a three-bladed rotor that turns in the wind and gives a redstone signal for how hard it is
 * blowing (about one level per 4 mph, so a gale gives full power). It must stand under open sky. Right-click to read
 * the wind speed.
 */
public class WindTurbineBlock extends Block implements EntityBlock, com.stormreach.structure.Multiblock {
    /** 6.0: a utility-scale turbine: a 16-block tower (parts 1..15 above the base) carrying a 13-block rotor. */
    public static final int TOWER = 15;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public WindTurbineBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // the whole tower must fit, or nothing is placed (the hologram shows red where it would not)
        if (!com.stormreach.structure.Multiblock.blocked(ctx.getLevel(), ctx.getClickedPos(), TOWER).isEmpty()) return null;
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override public int partHeight(BlockState s) { return TOWER; }

    @Override
    public com.stormreach.structure.MachinePartBlock.Shape partShape(BlockState s, int dy) {
        return dy == TOWER ? com.stormreach.structure.MachinePartBlock.Shape.BOX : com.stormreach.structure.MachinePartBlock.Shape.MAST;
    }

    @Override
    protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) com.stormreach.structure.Multiblock.buildParts(level, pos, s, this);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState s, ServerLevel level, BlockPos pos, boolean moved) {
        com.stormreach.structure.Multiblock.removeParts(level, pos, TOWER);
        super.affectNeighborsAfterRemoval(s, level, pos, moved);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override protected boolean isSignalSource(BlockState s) { return true; }
    @Override protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) { return s.getValue(POWER); }

    /** Redstone power for this wind: about a level per 4 mph, none when sheltered from the sky. */
    public static int powerFor(double mph, boolean openSky) {
        return openSky ? Math.max(0, Math.min(15, (int) Math.round(mph / 4.0))) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindTurbineBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        return level.isClientSide() || type != SkyRegistry.TURBINE_ENTITY ? null
                : (l, p, st, be) -> WindTurbineBlockEntity.serverTick((ServerLevel) l, p, st);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            double[] wind = com.stormreach.tornado.Instruments.windMph(server, pos.above());
            player.sendOverlayMessage(Component.translatable("block.stormreach.wind_turbine.reading", Math.round(wind[0]), Math.round(wind[1]), s.getValue(POWER))
                    .withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }
}
