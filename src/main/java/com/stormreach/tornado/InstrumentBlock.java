package com.stormreach.tornado;

import com.stormreach.sim.EFScale;
import com.stormreach.sim.WindField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Meteorological instruments. Right-click any of them for a reading; several also give a redstone signal:
 * anemometer (wind speed / 10 mph), storm detector (storm nearby), tornado radar (tornado within 256 blocks).
 * A tornado siren sounds while powered.
 */
public class InstrumentBlock extends Block {
    public enum Kind { ANEMOMETER, THERMOMETER, WEATHER_STATION, STORM_DETECTOR, TORNADO_RADAR, TORNADO_SIREN, WEATHER_MONITOR, BAROMETER, WIND_VANE }

    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private final Kind kind;

    public InstrumentBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 5);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int power = switch (kind) {
            case ANEMOMETER -> Math.min(15, (int) (Instruments.windMph(level, pos)[1] / 10));
            case STORM_DETECTOR -> level.isThundering() ? 15 : (TornadoManager.nearest(level, Vec3.atCenterOf(pos), 512) != null ? 15 : level.isRaining() ? 4 : 0);
            case TORNADO_RADAR -> {
                TornadoEntity t = TornadoManager.nearest(level, Vec3.atCenterOf(pos), 256);
                yield t == null ? 0 : Math.max(1, 15 - (int) (t.position().distanceTo(Vec3.atCenterOf(pos)) / 18));
            }
            case TORNADO_SIREN -> level.getBestNeighborSignal(pos);
            default -> 0;
        };
        if (state.getValue(POWER) != power) level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
        if (kind == Kind.TORNADO_SIREN && power > 0 && level.getGameTime() % 60 < 20) {
            level.playSound(null, pos, WeatherSounds.SIREN, SoundSource.BLOCKS, 6.0f, 1.0f);
        }
        level.scheduleTick(pos, this, kind == Kind.TORNADO_SIREN ? 20 : 40);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind == Kind.ANEMOMETER || kind == Kind.STORM_DETECTOR || kind == Kind.TORNADO_RADAR;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return isSignalSource(state) ? state.getValue(POWER) : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // its own screen; sneak + use prints the old one-line reading in chat
        if (level instanceof ServerLevel server) {
            if (player.isSecondaryUseActive() || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) report(kind, server, pos, player);
            else WeatherUi.open(sp, kind.ordinal(), pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable("instrument.stormreach." + key, args);
    }

    public static void report(Kind kind, ServerLevel level, BlockPos pos, Player player) {
        double[] wind = Instruments.windMph(level, pos);
        double t = Instruments.temperatureC(level, pos);
        double rh = Instruments.humidity(level, pos);
        switch (kind) {
            case ANEMOMETER -> player.sendSystemMessage(line("anemometer", Math.round(wind[0]), Math.round(wind[1]),
                    Math.round(EFScale.mphToKmh(wind[1])), Instruments.beaufort(wind[0])).withStyle(ChatFormatting.AQUA));
            case WIND_VANE -> player.sendSystemMessage(line("vane", Instruments.windFrom()).withStyle(ChatFormatting.AQUA));
            case BAROMETER -> player.sendSystemMessage(line("barometer", String.format("%.1f", Instruments.pressureHpa(level, pos)),
                    String.format("%.2f", Instruments.pressureHpa(level, pos) * 0.02953)).withStyle(ChatFormatting.AQUA));
            case THERMOMETER -> player.sendSystemMessage(line("thermometer", String.format("%.1f", t), String.format("%.1f", t * 9 / 5 + 32),
                    String.format("%.1f", Instruments.dewPointC(t, rh)), Math.round(rh)).withStyle(ChatFormatting.AQUA));
            case STORM_DETECTOR -> {
                TornadoEntity n = TornadoManager.nearest(level, Vec3.atCenterOf(pos), 512);
                player.sendSystemMessage(line(level.isThundering() ? "detector.storm" : level.isRaining() ? "detector.rain" : "detector.clear").withStyle(ChatFormatting.AQUA));
                if (n != null) player.sendSystemMessage(line("detector.tornado", Instruments.bearing(Vec3.atCenterOf(pos), n.position())).withStyle(ChatFormatting.RED));
            }
            case TORNADO_RADAR -> radar(level, Vec3.atCenterOf(pos), player, 1024, true);
            case TORNADO_SIREN -> player.sendSystemMessage(line("siren").withStyle(ChatFormatting.GRAY));
            case WEATHER_STATION -> station(level, pos, player, wind, t, rh, false);
            case WEATHER_MONITOR -> {
                boolean linked = nearby(level, pos, kindBlock(Kind.WEATHER_STATION)) && nearby(level, pos, kindBlock(Kind.TORNADO_RADAR));
                station(level, pos, player, wind, t, rh, linked);
                if (linked) radar(level, Vec3.atCenterOf(pos), player, 2048, true);
                else player.sendSystemMessage(line("monitor.unlinked").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** True when a Weather Station and a Doppler radar are within 8 blocks of a Weather Monitor. */
    public static boolean monitorLinked(ServerLevel level, BlockPos pos) {
        return nearby(level, pos, kindBlock(Kind.WEATHER_STATION)) && nearby(level, pos, kindBlock(Kind.TORNADO_RADAR));
    }

    private static Block kindBlock(Kind k) {
        return switch (k) {
            case WEATHER_STATION -> WeatherItems.WEATHER_STATION;
            case TORNADO_RADAR -> WeatherItems.TORNADO_RADAR;
            default -> WeatherItems.ANEMOMETER;
        };
    }

    private static boolean nearby(ServerLevel level, BlockPos pos, Block block) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -4, -8), pos.offset(8, 4, 8))) if (level.getBlockState(p).is(block)) return true;
        return false;
    }

    static void station(ServerLevel level, BlockPos pos, Player player, double[] wind, double t, double rh, boolean advanced) {
        player.sendSystemMessage(line("station.header").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.sendSystemMessage(line("station.now", Component.translatable(level.isThundering() ? "weather.stormreach.thunderstorm"
                : level.isRaining() ? "weather.stormreach.rain" : "weather.stormreach.clear")));
        player.sendSystemMessage(line("station.wind", Math.round(wind[0]), Math.round(wind[1]), Instruments.windFrom()));
        player.sendSystemMessage(line("station.air", String.format("%.1f", t), Math.round(rh), String.format("%.1f", Instruments.dewPointC(t, rh)),
                String.format("%.1f", Instruments.pressureHpa(level, pos))));
        player.sendSystemMessage(line("station.forecast", Instruments.forecast(level)));
        if (advanced) player.sendSystemMessage(line("station.risk", Instruments.stormRisk(level, pos)).withStyle(ChatFormatting.YELLOW));
    }

    /** Doppler-style radar readout: what a radar actually sees is the rotation couplet aloft, not the EF rating. */
    public static void radar(ServerLevel level, Vec3 from, Player player, double range, boolean detailed) {
        boolean any = false;
        for (TornadoEntity t : TornadoManager.live(level)) {
            double d = t.position().distanceTo(from);
            if (d > range) continue;
            any = true;
            WindField f = t.windField();
            double couplet = EFScale.msToMph(f.vMax * 2); // gate-to-gate velocity difference across the vortex
            double motion = EFScale.msToMph(Math.hypot(f.translateX, f.translateZ));
            String heading = Instruments.compass(Math.toDegrees(Math.atan2(f.translateX, -f.translateZ)));
            MutableComponent c = line("radar.contact", Instruments.bearing(from, t.position()), Math.round(d), Math.round(couplet),
                    Math.round(motion), heading);
            if (detailed) {
                EFScale est = t.estimatedRating();
                c.append(line("radar.detail", est == null ? "EF-U" : est.name(), Math.round(t.visualWidth()),
                        Component.translatable("tornado.stormreach.morph." + t.morphology().name().toLowerCase()),
                        t.subvortices() > 0 ? Component.translatable("tornado.stormreach.multivortex") : Component.literal("")));
            }
            player.sendSystemMessage(c.withStyle(ChatFormatting.RED));
        }
        if (!any) player.sendSystemMessage(line("radar.none").withStyle(ChatFormatting.GREEN));
    }
}
