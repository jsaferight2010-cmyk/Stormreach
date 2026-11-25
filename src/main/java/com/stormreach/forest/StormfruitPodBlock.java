package com.stormreach.forest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Stormfruit: a glowing fruit that hangs beneath Elderstorm leaves and ripens over time. Right-click a ripe one to pick it. */
public class StormfruitPodBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
    private static final VoxelShape[] SHAPES = {Block.box(6, 10, 6, 10, 16, 10), Block.box(5, 8, 5, 11, 16, 11), Block.box(4, 6, 4, 12, 16, 12)};

    public StormfruitPodBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPES[s.getValue(AGE)];
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.LEAVES);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess t, BlockPos pos, Direction d, BlockPos np, BlockState n, RandomSource r) {
        return d == Direction.UP && !canSurvive(s, level, pos) ? Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState s) {
        return s.getValue(AGE) < 2;
    }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
        if (r.nextInt(5) == 0) level.setBlock(pos, s.setValue(AGE, s.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (s.getValue(AGE) < 2) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            popResource(server, pos, new ItemStack(ForestRegistry.STORMFRUIT, 1 + server.getRandom().nextInt(2)));
            server.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1f, 1.2f);
            server.setBlock(pos, s.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.stormreach.progress.StormAdvancements.award(sp, "pick_stormfruit");
        }
        return InteractionResult.SUCCESS;
    }
}
