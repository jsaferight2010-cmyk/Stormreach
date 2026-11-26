package com.stormreach.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.stormreach.StormreachConfig;
import com.stormreach.weather.StormWeather;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * /stormreach thunderhead [minutes]  - start a Thunderhead
 * /stormreach calm                   - clear the weather
 * /stormreach status                 - is the player standing in a storm zone right now?
 * /stormreach forecast               - current weather and wind direction
 * /stormreach reload                 - re-read config/stormreach.properties
 */
public final class StormreachCommand {
    private StormreachCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stormreach")
                // 6.0: status and forecast are for everyone on a server; changing the weather or reloading stays op-only
                .then(Commands.literal("thunderhead").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> thunderhead(ctx.getSource(), StormreachConfig.hornStormMinutes))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 120))
                                .executes(ctx -> thunderhead(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "minutes")))))
                .then(Commands.literal("calm").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ctx.getSource().getServer().setWeatherParameters(6000, 0, false, false);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.stormreach.calm"), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    ServerLevel level = src.getLevel();
                    boolean zone = StormWeather.isStormZone(level.getBiome(net.minecraft.core.BlockPos.containing(src.getPosition())));
                    String weather = level.isThundering() ? "thunderhead" : level.isRaining() ? "gale" : "clear";
                    src.sendSuccess(() -> Component.translatable("command.stormreach.status",
                            Component.translatable("command.stormreach.weather." + weather),
                            Component.translatable(zone ? "gui.yes" : "gui.no")), false);
                    return 1;
                }))
                .then(Commands.literal("forecast").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> StormWeather.forecast(ctx.getSource().getLevel()), false);
                    return 1;
                }))
                .then(Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    StormreachConfig.load();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.stormreach.reloaded"), true);
                    return 1;
                })));
    }

    private static int thunderhead(CommandSourceStack source, int minutes) {
        source.getServer().setWeatherParameters(0, minutes * 60 * 20, true, true);
        source.sendSuccess(() -> Component.translatable("command.stormreach.thunderhead", minutes), true);
        return 1;
    }
}
