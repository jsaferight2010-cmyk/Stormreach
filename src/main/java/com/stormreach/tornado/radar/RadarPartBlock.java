package com.stormreach.tornado.radar;

import com.stormreach.tornado.WeatherItems;
import com.stormreach.tornado.WeatherUi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One cell of the Doppler radar structure. It knows its offset from the base centre, so it can find the controller:
 * using any part opens the radar, breaking any part takes the whole radar down (and drops one radar), and a part
 * whose controller is gone removes itself.
 */
public class RadarPartBlock extends Block {
    public static final EnumProperty<RadarLayout.Part> PART = EnumProperty.create("part", RadarLayout.Part.class);
    public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2);
    public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 6);
    public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2);
    private static final VoxelShape MAST = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 8, 16);

    public RadarPartBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(PART, RadarLayout.Part.BASE).setValue(OX, 1).setValue(OY, 0).setValue(OZ, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PART, OX, OY, OZ); }

    public static BlockState stateFor(RadarLayout.Cell c) {
        return RadarContent.RADAR_PART.defaultBlockState().setValue(PART, c.part())
                .setValue(OX, c.offset().getX() + 1).setValue(OY, c.offset().getY()).setValue(OZ, c.offset().getZ() + 1);
    }

    public static BlockPos controllerOf(BlockState s, BlockPos pos) {
        BlockPos base = pos.offset(-(s.getValue(OX) - 1), -s.getValue(OY), -(s.getValue(OZ) - 1));
        return base.offset(RadarLayout.CONTROLLER);
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        // the dish space is drawn by the controller's renderer; base and mast have block models
        return s.getValue(PART) == RadarLayout.Part.DISH ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (s.getValue(PART)) {
            case BASE -> BASE;
            case MAST -> MAST;
            case DISH -> Shapes.block();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockState(controllerOf(s, pos)).is(WeatherItems.TORNADO_RADAR))
            WeatherUi.open(sp, com.stormreach.tornado.InstrumentBlock.Kind.TORNADO_RADAR.ordinal(), controllerOf(s, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState s, Player player) {
        BlockPos c = controllerOf(s, pos);
        if (!level.isClientSide() && level.getBlockState(c).is(WeatherItems.TORNADO_RADAR)) level.destroyBlock(c, !player.isCreative(), player);
        return super.playerWillDestroy(level, pos, s, player);
    }

    /** 6.0: removed by anything but a player (explosion, tornado, command): take the radar down too, dropping it once. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState s, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        BlockPos c = controllerOf(s, pos);
        if (level.getBlockState(c).is(WeatherItems.TORNADO_RADAR)) level.destroyBlock(c, true);
        super.affectNeighborsAfterRemoval(s, level, pos, moved);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState n, RandomSource r) {
        // a part without its controller goes away (the removal spreads cell to cell through neighbour updates)
        return level.getBlockState(controllerOf(s, pos)).is(WeatherItems.TORNADO_RADAR) ? s : Blocks.AIR.defaultBlockState();
    }
}
