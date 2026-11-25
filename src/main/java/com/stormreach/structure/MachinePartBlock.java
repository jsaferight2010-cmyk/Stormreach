package com.stormreach.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/** An invisible cell of a {@link Multiblock} machine: DY blocks above its controller. The controller draws the model. */
public class MachinePartBlock extends Block {
    public enum Shape implements StringRepresentable {
        POLE, THIN, MAST, BOX, NONE;

        @Override public String getSerializedName() { return name().toLowerCase(); }
    }

    public static final IntegerProperty DY = IntegerProperty.create("dy", 1, 24);
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    private static final VoxelShape POLE = Block.box(5, 0, 5, 11, 16, 11), THIN = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5),
            MAST = Block.box(3, 0, 3, 13, 16, 13), BOX = Block.box(1, 0, 1, 15, 16, 15), PICK = Block.box(6, 0, 6, 10, 16, 10);

    public MachinePartBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(DY, 1).setValue(SHAPE, Shape.POLE));
    }

    public static BlockState state(int dy, Shape shape) {
        return StructureRegistry.MACHINE_PART.defaultBlockState().setValue(DY, dy).setValue(SHAPE, shape);
    }

    public static BlockPos controllerOf(BlockState s, BlockPos pos) { return pos.below(s.getValue(DY)); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(DY, SHAPE); }

    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (s.getValue(SHAPE)) {
            case POLE -> POLE; case THIN -> THIN; case MAST -> MAST; case BOX -> BOX; case NONE -> PICK;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return s.getValue(SHAPE) == Shape.NONE ? Shapes.empty() : getShape(s, level, pos, ctx);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos c = controllerOf(s, pos);
        BlockState cs = level.getBlockState(c);
        if (!(cs.getBlock() instanceof Multiblock)) return InteractionResult.PASS;
        return cs.useWithoutItem(level, player, hit.withPosition(c));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState s, Player player) {
        BlockPos c = controllerOf(s, pos);
        if (!level.isClientSide() && level.getBlockState(c).getBlock() instanceof Multiblock) level.destroyBlock(c, !player.isCreative(), player);
        return super.playerWillDestroy(level, pos, s, player);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState s, boolean includeData) {
        BlockState cs = level.getBlockState(controllerOf(s, pos));
        return cs.getBlock() instanceof Multiblock ? new ItemStack(cs.getBlock()) : ItemStack.EMPTY;
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState n, RandomSource r) {
        BlockState cs = level.getBlockState(controllerOf(s, pos));
        return cs.getBlock() instanceof Multiblock m && m.partHeight(cs) >= s.getValue(DY) ? s : Blocks.AIR.defaultBlockState();
    }
}
