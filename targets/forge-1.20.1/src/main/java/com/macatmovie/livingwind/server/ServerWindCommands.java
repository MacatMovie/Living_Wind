package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.macatmovie.livingwind.LivingWind;

/**
 * Operator/debug commands. They push the wind simulation rather than permanently freezing it, so
 * normal random movement resumes automatically after the requested test input.
 */
@Mod.EventBusSubscriber(modid = LivingWind.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerWindCommands {
    private ServerWindCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("livingwind")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                    .executes(context -> status(context.getSource())))
                .then(Commands.literal("strength")
                    .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 2.5f))
                        .executes(context -> strength(
                            context.getSource(),
                            FloatArgumentType.getFloat(context, "value")
                        ))))
                .then(Commands.literal("direction")
                    .then(Commands.argument("degrees", FloatArgumentType.floatArg(0.0f, 360.0f))
                        .executes(context -> direction(
                            context.getSource(),
                            FloatArgumentType.getFloat(context, "degrees")
                        ))))
                .then(Commands.literal("gust")
                    .executes(context -> gust(context.getSource(), 0.45f, 7))
                    .then(Commands.argument("strength", FloatArgumentType.floatArg(0.0f, 2.0f))
                        .executes(context -> gust(
                            context.getSource(),
                            FloatArgumentType.getFloat(context, "strength"),
                            7
                        ))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                            .executes(context -> gust(
                                context.getSource(),
                                FloatArgumentType.getFloat(context, "strength"),
                                IntegerArgumentType.getInteger(context, "seconds")
                            )))))
                .then(Commands.literal("windyperiod")
                    .then(Commands.literal("start")
                        .executes(context -> windyPeriod(context.getSource(), 0.38f, 180))
                        .then(Commands.argument("boost", FloatArgumentType.floatArg(0.0f, 2.0f))
                            .executes(context -> windyPeriod(
                                context.getSource(),
                                FloatArgumentType.getFloat(context, "boost"),
                                180
                            ))
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 1200))
                                .executes(context -> windyPeriod(
                                    context.getSource(),
                                    FloatArgumentType.getFloat(context, "boost"),
                                    IntegerArgumentType.getInteger(context, "seconds")
                                )))))
                    .then(Commands.literal("stop")
                        .executes(context -> stopWindyPeriod(context.getSource()))))
                .then(Commands.literal("natural")
                    .executes(context -> natural(context.getSource())))
        );
    }

    private static int strength(CommandSourceStack source, float value) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        data.commandPushStrength(value, level.getGameTime());
        refreshAndSync(level, data);
        source.sendSuccess(() -> Component.literal(String.format(
            Locale.ROOT,
            "Living Wind strength forced to %.2f for 30 seconds. Gusts, windy periods and weather cannot override it.",
            value
        )), false);
        return 1;
    }

    private static int direction(CommandSourceStack source, float degrees) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        data.commandPushDirection(degrees, level.getGameTime());
        refreshAndSync(level, data);
        source.sendSuccess(() -> Component.literal(String.format(
            Locale.ROOT,
            "Living Wind direction pushed to %.1f degrees. Natural direction changes resume in ~30s.",
            degrees
        )), false);
        return 1;
    }

    private static int gust(CommandSourceStack source, float strength, int seconds) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        data.commandStartGust(strength, seconds * 20, level.getGameTime());
        refreshAndSync(level, data);
        source.sendSuccess(() -> Component.literal(String.format(
            Locale.ROOT,
            "Started Living Wind gust: +%.2f for %ds.", strength, seconds
        )), false);
        return 1;
    }

    private static int windyPeriod(CommandSourceStack source, float boost, int seconds) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        data.commandStartWindyPeriod(boost, seconds * 20, level.getGameTime());
        refreshAndSync(level, data);
        source.sendSuccess(() -> Component.literal(String.format(
            Locale.ROOT,
            "Started Living Wind windy period: +%.2f for %ds. Normal variation, weather and gusts continue.",
            boost, seconds
        )), false);
        return 1;
    }

    private static int stopWindyPeriod(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        data.commandStopWindyPeriod();
        refreshAndSync(level, data);
        source.sendSuccess(() -> Component.literal("Stopped the current Living Wind windy period for this day."), false);
        return 1;
    }

    private static int natural(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        data.commandReturnToNatural(level.getGameTime());
        refreshAndSync(level, data);
        source.sendSuccess(() -> Component.literal("Living Wind returned to natural simulation and requested a fresh random target."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WindSavedData data = WindSavedData.get(level);
        WindSnapshot snapshot = data.snapshot(level.getGameTime());
        long manualTicks = data.commandManualWindyTicksRemaining(level.getGameTime());
        long forcedTicks = data.commandForcedStrengthTicksRemaining(level.getGameTime());
        source.sendSuccess(() -> Component.literal(String.format(
            Locale.ROOT,
            "Living Wind | final %.2f @ %.1f° | base %.2f -> %.2f | turbulence %.2f | gust %.2f | windy %s | front %s | forced windy %.1fs | strength lock %s",
            snapshot.strength(), snapshot.directionDegrees(), data.commandBaseStrength(), data.commandTargetBaseStrength(),
            snapshot.turbulence(), snapshot.gustStrength(), snapshot.windyPeriod(), snapshot.stormFront(), manualTicks / 20.0f,
            forcedTicks > 0L ? String.format(Locale.ROOT, "%.2f (%.1fs remaining)", data.commandForcedStrength(), forcedTicks / 20.0f) : "OFF"
        )), false);
        return 1;
    }

    /** One immediate simulation refresh makes test commands visible without waiting for the next 2s sync. */
    private static void refreshAndSync(ServerLevel level, WindSavedData data) {
        data.tick(level);
        WindNetworking.sendToLevel(level, data.snapshot(level.getGameTime()));
    }
}
