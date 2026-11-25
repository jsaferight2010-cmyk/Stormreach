package com.stormreach.chase;

import com.stormreach.sim.Rainfall;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rain Gauge: a graduated glass tube that fills with rain (faster in heavier rain and thunderstorms) and slowly
 * evaporates in dry weather. Right-click to read it in millimetres; sneak + right-click to empty it. A comparator
 * reads its level (0-8), so it can switch redstone when a storm has dropped enough rain.
 */
public class RainGaugeBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
    public static final double MM_PER_LEVEL = 6.25;
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 14, 11);

    public RainGaugeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    /** Chance per random tick that the gauge rises a level, from the rain intensity there. */
    public static float fillChance(Level level, BlockPos pos) {
        if (!level.isRainingAt(pos.above())) return 0f;
        return (float) Math.min(1.0, Rainfall.intensity(true, level.isThundering()) * 0.9);
    }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
        int lv = s.getValue(LEVEL);
        float fill = fillChance(level, pos);
        if (fill > 0 && lv < 8 && r.nextFloat() < fill) level.setBlock(pos, s.setValue(LEVEL, lv + 1), Block.UPDATE_ALL);
        else if (fill == 0 && lv > 0 && !level.isRaining() && r.nextInt(6) == 0) level.setBlock(pos, s.setValue(LEVEL, lv - 1), Block.UPDATE_ALL);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState s) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int lv = s.getValue(LEVEL);
        if (player.isSecondaryUseActive()) {
            level.setBlock(pos, s.setValue(LEVEL, 0), Block.UPDATE_ALL);
            player.sendOverlayMessage(Component.translatable("block.stormreach.rain_gauge.emptied").withStyle(ChatFormatting.AQUA));
        } else {
            player.sendOverlayMessage(Component.translatable("block.stormreach.rain_gauge.reading",
                    String.format(java.util.Locale.ROOT, "%.1f", lv * MM_PER_LEVEL)).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState s, Level l, BlockPos p, Direction d) { return Math.min(15, s.getValue(LEVEL) * 2); }
}
