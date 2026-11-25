package com.stormreach.power;

import com.stormreach.registry.ModFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Water storage. The Rain Barrel (4 buckets) fills from the sky when it rains on it, faster in a thunderstorm, and
 * a thunderstorm's rain is Stormwater. Set it on a Cistern (32 buckets) and it drains into it. Both:
 * - use a bucket for a bucket of water (or Stormwater), a glass bottle for a water bottle;
 * - pour in water or Stormwater buckets;
 * - feed Sprinklers next to them, take water from an Electric Pump, and are read by comparators.
 */
public class TankBlock extends Block implements EntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    public static final BooleanProperty STORM = BooleanProperty.create("storm");
    public final int capacity;
    public final boolean catchesRain;
    private final VoxelShape shape;

    public TankBlock(Properties p, int capacity, boolean catchesRain, VoxelShape shape) {
        super(p);
        this.capacity = capacity;
        this.catchesRain = catchesRain;
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(STORM, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LEVEL, STORM); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return shape; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Entity(pos, state); }
    @Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos, Direction d) {
        return level.getBlockEntity(pos) instanceof Entity t ? (int) Math.ceil(t.water() * 15.0 / t.waterCapacity()) : 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> t) {
        return level.isClientSide() || t != PowerRegistry.TANK_ENTITY ? null : (l, p, st, be) -> ((Entity) be).serverTick((ServerLevel) l, p, st);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Entity tank)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        ItemStack out = null;
        if (stack.is(Items.BUCKET) && tank.water() >= 1000) {
            boolean storm = tank.isStormwater();
            tank.drain(1000);
            out = new ItemStack(storm ? ModFluids.STORMWATER_BUCKET : Items.WATER_BUCKET);
        } else if (stack.is(Items.GLASS_BOTTLE) && tank.water() >= 250) {
            tank.drain(250);
            out = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        } else if ((stack.is(Items.WATER_BUCKET) || stack.is(ModFluids.STORMWATER_BUCKET)) && tank.waterCapacity() - tank.water() >= 1000) {
            tank.fill(1000, stack.is(ModFluids.STORMWATER_BUCKET));
            out = new ItemStack(Items.BUCKET);
        } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, out));
            level.playSound(null, pos, out.is(Items.BUCKET) ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1f, 1f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Entity t) {
            player.sendOverlayMessage(Component.translatable("block.stormreach.tank.reading", t.water() / 1000.0, t.waterCapacity() / 1000,
                    Component.translatable(t.isStormwater() ? "block.stormreach.tank.stormwater" : "block.stormreach.tank.water")).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    public static class Entity extends BlockEntity implements WaterTank {
        private int water;
        private boolean storm;

        public Entity(BlockPos pos, BlockState state) {
            super(PowerRegistry.TANK_ENTITY, pos, state);
        }

        TankBlock block() { return (TankBlock) getBlockState().getBlock(); }
        @Override public int water() { return water; }
        @Override public int waterCapacity() { return block().capacity; }
        @Override public boolean isStormwater() { return storm && water > 0; }

        @Override
        public int fill(int mb, boolean stormwater) {
            int n = Math.max(0, Math.min(mb, waterCapacity() - water));
            if (n == 0) return 0;
            storm = water == 0 ? stormwater : storm && stormwater;
            water += n;
            changed();
            return n;
        }

        @Override
        public int drain(int mb) {
            int n = Math.max(0, Math.min(mb, water));
            if (n == 0) return 0;
            water -= n;
            changed();
            return n;
        }

        private void changed() {
            setChanged();
            if (level == null || level.isClientSide()) return;
            int vis = water == 0 ? 0 : 1 + Math.min(3, water * 4 / waterCapacity());
            BlockState s = getBlockState();
            if (s.getValue(LEVEL) != vis || s.getValue(STORM) != isStormwater())
                level.setBlock(worldPosition, s.setValue(LEVEL, vis).setValue(STORM, isStormwater()), Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, s.getBlock());
        }

        void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
            if (block().catchesRain && level.isRainingAt(pos.above())) fill(level.isThundering() ? 60 : 25, level.isThundering());
            // a barrel drains into a tank below it
            if (water > 0 && level.getBlockEntity(pos.below()) instanceof WaterTank below) {
                boolean st = isStormwater();
                drain(below.fill(Math.min(250, water), st));
            }
        }

        @Override
        protected void saveAdditional(ValueOutput out) {
            super.saveAdditional(out);
            out.putInt("Water", water);
            out.putBoolean("Storm", storm);
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            water = in.getIntOr("Water", 0);
            storm = in.getBooleanOr("Storm", false);
        }
    }
}
