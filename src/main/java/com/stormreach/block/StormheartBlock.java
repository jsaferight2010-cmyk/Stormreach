package com.stormreach.block;

import com.stormreach.StormreachConfig;
import com.stormreach.blight.Stormblight;
import com.stormreach.entity.StormFront;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * Stormheart: a seed of the Stormblight.
 *  - Use a Storm Cell on it (or hit it with lightning, or power it with redstone) to wake it.
 *    An awake heart pulses outward, turning blocks and mobs into Stormreach ones in a growing sphere
 *    (up to the radius set in the config). When it finishes, the area becomes Stormreach Coast.
 *  - Use a Thunderhead Horn on it to release a Storm Front: a travelling storm that converts the land it passes over.
 *  - Use Salt on an awake heart to calm it again.
 */
public class StormheartBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** Grows from 0 to 31; the conversion radius is 2 + stage * 2 blocks (capped by the config). */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 31);

    public StormheartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false).setValue(STAGE, 0));
    }

    public static int radius(BlockState state) {
        return Math.min(StormreachConfig.heartMaxRadius, 2 + state.getValue(STAGE) * 2);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, STAGE);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (StormreachConfig.heartActivatesOnPlace && level instanceof ServerLevel server) activate(server, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !state.getValue(ACTIVE) && server.hasNeighborSignal(pos)) activate(server, pos, state);
    }

    public static void activate(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(ACTIVE) || StormreachConfig.spreadMode == StormreachConfig.SpreadMode.OFF) return;
        level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
        level.playSound(null, pos, com.stormreach.registry.ModSfx.MAGIC_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
        level.playSound(null, pos, com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.BLOCKS, 1.0f, 1.4f);
        level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.8, 0.8, 0.8, 0.3);
        level.scheduleTick(pos, state.getBlock(), 10);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (stack.is(ModItems.STORM_CELL) && !state.getValue(ACTIVE)) {
            if (StormreachConfig.spreadMode == StormreachConfig.SpreadMode.OFF) {
                player.sendOverlayMessage(Component.translatable("message.stormreach.blight_off"));
                return InteractionResult.FAIL;
            }
            activate(server, pos, state);
            stack.consume(1, player);
            player.sendOverlayMessage(Component.translatable("message.stormreach.heart_awake").withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(ModItems.THUNDERHEAD_HORN)) {
            if (!StormreachConfig.stormFront) {
                player.sendOverlayMessage(Component.translatable("message.stormreach.front_off"));
                return InteractionResult.FAIL;
            }
            StormFront front = ModEntities.STORM_FRONT.create(server, EntitySpawnReason.TRIGGERED);
            if (front == null) return InteractionResult.FAIL;
            front.snapTo(pos.getX() + 0.5, pos.getY() + 12, pos.getZ() + 0.5, 0f, 0f);
            server.addFreshEntity(front);
            server.playSound(null, pos, com.stormreach.registry.ModSfx.EVENT_HORN, SoundSource.BLOCKS, 3.0f, 0.6f);
            stack.consume(1, player);
            player.sendOverlayMessage(Component.translatable("message.stormreach.front_released").withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(ModItems.SALT) && state.getValue(ACTIVE)) {
            server.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
            server.playSound(null, pos, com.stormreach.registry.ModSfx.MAGIC_DISPEL, SoundSource.BLOCKS, 1.2f, 0.8f);
            stack.consume(1, player);
            player.sendOverlayMessage(Component.translatable("message.stormreach.heart_calmed"));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(state.getValue(ACTIVE)
                    ? Component.translatable("message.stormreach.heart_status", radius(state), StormreachConfig.heartMaxRadius)
                    : Component.translatable("message.stormreach.heart_dormant"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        if (StormreachConfig.spreadMode == StormreachConfig.SpreadMode.OFF) {
            // Paused by the config: check back later so it resumes if the Stormblight is turned on again.
            level.scheduleTick(pos, this, 200);
            return;
        }
        int radius = radius(state);
        boolean grown = radius >= StormreachConfig.heartMaxRadius;

        // Convert a batch of random spots inside the sphere, weighted toward its growing edge.
        int attempts = 40 + StormreachConfig.spreadSpeed * 40;
        for (int i = 0; i < attempts; i++) {
            double r = radius * Math.sqrt(random.nextDouble());
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * random.nextDouble() - 1);
            BlockPos target = pos.offset((int) Math.round(r * Math.sin(phi) * Math.cos(theta)),
                    (int) Math.round(r * Math.cos(phi) * 0.6), (int) Math.round(r * Math.sin(phi) * Math.sin(theta)));
            if (level.isLoaded(target)) Stormblight.convertBlock(level, target, random);
        }
        if (random.nextInt(3) == 0) Stormblight.convertOneMob(level, new AABB(pos).inflate(radius), random);

        level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.15);
        if (random.nextInt(8) == 0) {
            level.playSound(null, pos, com.stormreach.registry.ModSfx.MAGIC_HUM, SoundSource.BLOCKS, 1.0f, 0.5f + random.nextFloat() * 0.3f);
        }

        if (!grown && random.nextInt(Math.max(1, 12 - StormreachConfig.spreadSpeed)) == 0) {
            BlockState next = state.setValue(STAGE, Math.min(31, state.getValue(STAGE) + 1));
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            if (radius(next) >= StormreachConfig.heartMaxRadius) {
                Stormblight.blightBiome(level, pos, StormreachConfig.heartMaxRadius);
                level.playSound(null, pos, com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.WEATHER, 4.0f, 0.7f);
            }
        }
        // Once fully grown it keeps tidying up its area, but more slowly.
        level.scheduleTick(pos, this, grown ? 60 : 20);
    }
}
