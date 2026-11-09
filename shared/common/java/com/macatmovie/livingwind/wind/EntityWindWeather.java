package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Extra gameplay-only multiplier for optional entity wind. Never modifies the
 * actual wind snapshot, public Wind API, ambient particles, or sailing integrations.
 * The caller gates this behind Entity Wind and the individual entity-type toggles.
 */
public final class EntityWindWeather {
    private EntityWindWeather() {}

    // Only accessed from the server entity tick thread. One weather/config sample
    // per world per tick keeps the extra cost negligible even with many mobs.
    private static final Map<ServerLevel, CachedMultiplier> SERVER_CACHE = new WeakHashMap<>();

    private static final class CachedMultiplier {
        long tick = Long.MIN_VALUE;
        double value = 1.0;
    }

    /** Vanilla weather fade levels provide smooth transitions, including partial storms. */
    public static double serverMultiplier(ServerLevel level) {
        CachedMultiplier cached = SERVER_CACHE.computeIfAbsent(level, unused -> new CachedMultiplier());
        long tick = level.getGameTime();
        if (cached.tick != tick) {
            cached.value = interpolate(
                WindConfig.ENTITY_WIND_CLEAR_WEATHER_MULTIPLIER.get(),
                WindConfig.ENTITY_WIND_RAIN_MULTIPLIER.get(),
                WindConfig.ENTITY_WIND_THUNDER_MULTIPLIER.get(),
                level.getRainLevel(1.0f), level.getThunderLevel(1.0f)
            );
            cached.tick = tick;
        }
        return cached.value;
    }

    /** Pure calculation shared with client-predicted player and ridden-boat movement. */
    public static double interpolate(double clear, double rain, double thunder, float rainLevel, float thunderLevel) {
        double rainFade = Math.max(0.0, Math.min(1.0, rainLevel));
        double thunderFade = Math.max(0.0, Math.min(1.0, thunderLevel));
        double wet = clear + (rain - clear) * rainFade;
        return wet + (thunder - wet) * thunderFade;
    }
}
