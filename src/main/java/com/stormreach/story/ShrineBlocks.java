package com.stormreach.story;

import com.stormreach.labyrinth.LabyrinthRegistry;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.realm.RealmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Hollow Shrine's ritual, in three stages:
 * 1. Four Relic Pedestals each take one relic gathered elsewhere (riddles on the pedestals say which).
 * 2. With every pedestal lit, lightning must strike the four Storm Conduits in the order of their notches.
 * 3. The Weathered Journal is laid on the Hollow Altar, and Cyrrus rises.
 */
public final class ShrineBlocks {
    private ShrineBlocks() {}

    static Item relicFor(int relic) {
        return switch (relic) {
            case 1 -> LabyrinthRegistry.HEART_OF_THE_LABYRINTH;
            case 2 -> RealmItems.TEMPEST_EYE;
            case 3 -> com.stormreach.forest.ForestRegistry.GILDED_STORMFRUIT;
            default -> StoryRegistry.HOLLOW_REMNANT;
        };
    }

    public static class Pedestal extends Block {
        public static final IntegerProperty RELIC = IntegerProperty.create("relic", 1, 4);
        public static final BooleanProperty LIT = BooleanProperty.create("lit");

        public Pedestal(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(RELIC, 1).setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(RELIC, LIT);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (state.getValue(LIT) || !stack.is(relicFor(state.getValue(RELIC)))) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level instanceof ServerLevel server) {
                stack.consume(1, player);
                server.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                server.playSound(null, pos, com.stormreach.registry.ModSfx.MAGIC_ACTIVATE, SoundSource.BLOCKS, 1.2f, 0.6f);
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.02);
                player.sendSystemMessage(Component.translatable("block.stormreach.relic_pedestal.accepted").withStyle(ChatFormatting.DARK_AQUA));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable(state.getValue(LIT) ? "block.stormreach.relic_pedestal.lit"
                        : "block.stormreach.relic_pedestal.riddle" + state.getValue(RELIC)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Conduit extends Block {
        public static final IntegerProperty ORDER = IntegerProperty.create("order", 1, 4);
        public static final BooleanProperty STRUCK = BooleanProperty.create("struck");

        public Conduit(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(ORDER, 1).setValue(STRUCK, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(ORDER, STRUCK);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) player.sendSystemMessage(Component.translatable("block.stormreach.storm_conduit.notches", state.getValue(ORDER)).withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
    }

    /** Lightning hook: strike conduits in order 1, 2, 3, 4 once all pedestals around them are lit. */
    public static void onLightning(ServerLevel level, BlockPos hit) {
        for (BlockPos p : BlockPos.betweenClosed(hit.offset(-1, -2, -1), hit.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(p);
            if (!s.is(StoryRegistry.STORM_CONDUIT) || s.getValue(Conduit.STRUCK)) continue;
            BlockPos at = p.immutable();
            if (!allPedestalsLit(level, at)) {
                playerMsg(level, at, "block.stormreach.storm_conduit.unready");
                return;
            }
            int order = s.getValue(Conduit.ORDER);
            int done = countStruck(level, at);
            if (order == done + 1) {
                level.setBlock(at, s.setValue(Conduit.STRUCK, true), Block.UPDATE_ALL);
                level.playSound(null, at, com.stormreach.registry.ModSfx.MAGIC_CHIME, SoundSource.BLOCKS, 2f, 0.5f + order * 0.2f);
                playerMsg(level, at, order == 4 ? "block.stormreach.storm_conduit.complete" : "block.stormreach.storm_conduit.right");
            } else {
                // wrong order: every conduit goes dark again
                for (BlockPos q : BlockPos.betweenClosed(at.offset(-24, -6, -24), at.offset(24, 6, 24))) {
                    BlockState qs = level.getBlockState(q);
                    if (qs.is(StoryRegistry.STORM_CONDUIT) && qs.getValue(Conduit.STRUCK)) level.setBlock(q, qs.setValue(Conduit.STRUCK, false), Block.UPDATE_ALL);
                }
                level.playSound(null, at, com.stormreach.registry.ModSfx.CREATURE_TITAN_BLAST, SoundSource.BLOCKS, 1.5f, 0.5f);
                playerMsg(level, at, "block.stormreach.storm_conduit.wrong");
            }
            return;
        }
    }

    private static void playerMsg(ServerLevel level, BlockPos at, String key) {
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(at)) < 64 * 64) p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_AQUA));
    }

    static boolean allPedestalsLit(ServerLevel level, BlockPos near) {
        int lit = 0, total = 0;
        for (BlockPos q : BlockPos.betweenClosed(near.offset(-24, -6, -24), near.offset(24, 6, 24))) {
            BlockState s = level.getBlockState(q);
            if (s.is(StoryRegistry.RELIC_PEDESTAL)) { total++; if (s.getValue(Pedestal.LIT)) lit++; }
        }
        return total >= 4 && lit == total;
    }

    static int countStruck(ServerLevel level, BlockPos near) {
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(near.offset(-24, -6, -24), near.offset(24, 6, 24))) {
            BlockState s = level.getBlockState(q);
            if (s.is(StoryRegistry.STORM_CONDUIT) && s.getValue(Conduit.STRUCK)) n++;
        }
        return n;
    }

    public static class Altar extends Block {
        public Altar(Properties p) {
            super(p);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(StoryRegistry.WEATHERED_JOURNAL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!allPedestalsLit(server, pos)) {
                player.sendSystemMessage(Component.translatable("block.stormreach.hollow_altar.pedestals").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (countStruck(server, pos) < 4) {
                player.sendSystemMessage(Component.translatable("block.stormreach.hollow_altar.conduits").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (!server.getEntitiesOfClass(CyrrusBoss.class, new AABB(pos).inflate(128)).isEmpty()) return InteractionResult.FAIL;
            stack.consume(1, player);
            CyrrusBoss boss = StoryRegistry.CYRRUS.create(server, EntitySpawnReason.EVENT);
            if (boss == null) return InteractionResult.FAIL;
            boss.snapTo(pos.getX() + 0.5, pos.getY() - 4, pos.getZ() + 0.5, 0, 0);
            boss.beginCinematic(pos);
            server.addFreshEntity(boss);
            if (player instanceof ServerPlayer sp) StormAdvancements.award(sp, "shrine_ritual");
            return InteractionResult.SUCCESS_SERVER;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) player.sendSystemMessage(Component.translatable("block.stormreach.hollow_altar.hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResult.SUCCESS;
        }
    }
}
