package com.stormreach.signs;

import com.stormreach.sim.StormSigns;
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
 * 6.0 stage 10: a Weatherglass (a storm glass: a sealed tube of camphor and brine, like Fitzroy's). Unlike the barometer, which reads the
 * sky as it is, the glass feels weather that hasn't arrived yet: crystals grow in it as rain nears and fill it in a
 * thunderstorm. A comparator reads 0 (settled) to 15 (thunderstorm); see {@link StormSigns#glassLevel}.
 * Only the Overworld has weather to feel; anywhere else it stays clear.
 */
public class WeatherglassBlock extends Block {
    public static final IntegerProperty READING = IntegerProperty.create("reading", 0, 15);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 14, 11);
    private static final int CHECK_INTERVAL = 100;

    public WeatherglassBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(READING, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(READING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The glass's reading for this level right now. */
    public static int read(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return 0;
        var w = level.getServer().getWeatherData();
        return StormSigns.glassLevel(w.isRaining(), w.isThundering(), w.getRainTime(), w.getThunderTime(), w.getClearWeatherTime());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int reading = read(level);
        if (state.getValue(READING) != reading) level.setBlock(pos, state.setValue(READING, reading), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(READING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            int r = read(sl);
            String key = r >= 15 ? "thunderstorm" : r >= 11 ? "thunder_near" : r >= 10 ? "raining" : r >= 5 ? "rain_soon" : r >= 1 ? "rain_coming" : "settled";
            player.sendOverlayMessage(Component.translatable("block.stormreach.weatherglass." + key).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }
}
