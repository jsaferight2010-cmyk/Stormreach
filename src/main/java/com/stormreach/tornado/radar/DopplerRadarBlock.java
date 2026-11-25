package com.stormreach.tornado.radar;

import com.stormreach.tornado.InstrumentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Doppler radar's controller: the pedestal on top of the mast. Its block entity draws the rotating dish (see
 * the client's DopplerRadarRenderer); everything else about it is the regular radar instrument (screen, redstone
 * signal, Weather Monitor link). Breaking it takes the whole structure down.
 */
public class DopplerRadarBlock extends InstrumentBlock implements EntityBlock {
    public DopplerRadarBlock(Properties p) {
        super(Kind.TORNADO_RADAR, p);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DopplerRadarBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState s, Player player) {
        if (!level.isClientSide()) RadarStructure.removeParts(level, pos);
        return super.playerWillDestroy(level, pos, s, player);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState n, RandomSource r) {
        // without the mast under it the pedestal falls apart too
        if (dir == Direction.DOWN && !n.is(RadarContent.RADAR_PART)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(s, level, ticks, pos, dir, nPos, n, r);
    }
}
