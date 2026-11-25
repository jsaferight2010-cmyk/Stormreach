package com.stormreach.realm;

import com.stormreach.StormreachConfig;
import com.stormreach.entity.TempestSovereign;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Sovereign Altar at the top of every Tempest Spire. During a thunderstorm it may stir on its own;
 * a Tempest Sigil forces a storm and a full ritual. Either way, the Tempest Sovereign descends at the end.
 */
public class SovereignAltarBlock extends Block {
    public static final IntegerProperty RITUAL = IntegerProperty.create("ritual", 0, 15);
    /** Natural awakenings skip the first part of the ritual. */
    private static final int NATURAL_START = 10;

    public SovereignAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RITUAL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RITUAL);
    }

    private static int stepTicks() {
        return Math.max(10, StormreachConfig.sigilRitualSeconds * 20 / 14);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(RealmItems.TEMPEST_SIGIL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (state.getValue(RITUAL) > 0) {
            player.sendOverlayMessage(msg("busy"));
            return InteractionResult.FAIL;
        }
        if (sovereignNear(server, pos)) {
            player.sendOverlayMessage(msg("already"));
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        if (server.dimension() == Level.OVERWORLD) {
            server.getServer().setWeatherParameters(0, 12000, true, true);
        }
        begin(server, pos, state, 1);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        player.sendOverlayMessage(msg(state.getValue(RITUAL) > 0 ? "busy" : (level.isThundering() ? "stirring" : "dormant")));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RITUAL) != 0 || !StormreachConfig.sovereignNaturalSpawns || !level.isThundering()) return;
        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 32.0, p -> !p.isSpectator()) == null) return;
        if (random.nextInt(100) >= StormreachConfig.sovereignSpawnChance || sovereignNear(level, pos)) return;
        begin(level, pos, state, NATURAL_START);
    }

    private void begin(ServerLevel level, BlockPos pos, BlockState state, int stage) {
        level.setBlock(pos, state.setValue(RITUAL, stage), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, stepTicks());
        level.playSound(null, pos, com.stormreach.registry.ModSfx.CREATURE_TITAN_ROAR, SoundSource.HOSTILE, 1.5f, 1.6f);
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < 64 * 64)) {
            p.sendSystemMessage(msg(stage == 1 ? "ritual_begins" : "awakens"));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(RITUAL);
        if (stage == 0) return;
        // Lightning walks around the spire, faster and closer as the ritual nears its end.
        double angle = stage * (Math.PI / 4);
        int r = Math.max(3, 9 - stage / 2);
        StormWeather.strikeVisual(level, ritualStrike(level, pos, angle, r));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20 + stage * 4, 0.4, 0.6, 0.4, 0.25);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10 + stage * 3, 1.5, 1.5, 1.5, 0.05);
        level.playSound(null, pos, com.stormreach.registry.ModSfx.MAGIC_HUM, SoundSource.BLOCKS, 1.5f, 0.5f + stage * 0.08f);
        if (stage < 15) {
            level.setBlock(pos, state.setValue(RITUAL, stage + 1), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, stepTicks());
            return;
        }
        level.setBlock(pos, state.setValue(RITUAL, 0), Block.UPDATE_ALL);
        TempestSovereign boss = RealmEntities.TEMPEST_SOVEREIGN.create(level, EntitySpawnReason.EVENT);
        if (boss == null) return;
        boss.snapTo(pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        boss.setHome(pos);
        level.addFreshEntity(boss);
        for (int i = 0; i < 4; i++) StormWeather.strikeVisual(level, pos.offset(random.nextInt(7) - 3, 1, random.nextInt(7) - 3));
        level.playSound(null, pos, com.stormreach.registry.ModSfx.CREATURE_TITAN_AMBIENT, SoundSource.HOSTILE, 3.0f, 1.3f);
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < 96 * 96)) {
            p.sendSystemMessage(msg("descends"));
        }
    }

    private static boolean sovereignNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(TempestSovereign.class, new AABB(pos).inflate(128.0)).isEmpty();
    }

    private static Component msg(String key) {
        return Component.translatable("block.stormreach.sovereign_altar." + key).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * Where the ritual's lightning lands at this step. The ring around the altar can hang over the spire's edge or
     * open water, so this walks the aim point in toward the altar until it finds ground, and as a last resort
     * strikes in mid-air beside the altar. (Before 5.0.1 a missing surface crashed the world tick.)
     */
    public static BlockPos ritualStrike(ServerLevel level, BlockPos altar, double angle, int radius) {
        for (int r = radius; r >= 1; r--) {
            BlockPos aim = altar.offset((int) Math.round(Math.cos(angle) * r), 4, (int) Math.round(Math.sin(angle) * r));
            BlockPos ground = StormWeather.surfaceBelow(level, aim, 24);
            if (ground != null) return ground;
        }
        return altar.offset((int) Math.round(Math.cos(angle) * radius), 1, (int) Math.round(Math.sin(angle) * radius));
    }
}
