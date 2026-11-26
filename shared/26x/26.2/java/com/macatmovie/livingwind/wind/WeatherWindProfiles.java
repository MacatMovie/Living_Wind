package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.config.WindConfig;

/**
 * Optional weather-specific strength ranges, evaluated BEFORE the existing global scale and
 * gust contribution. The old simulation is completely untouched when the feature is disabled.
 *
 * A fluctuation of 0 uses each range's midpoint. At 1, the normal atmospheric variation
 * selects a position inside the configured range. Min > max is handled safely without
 * changing user config values. Rain and thunder fade continuously between the ranges.
 */
public final class WeatherWindProfiles {
    private WeatherWindProfiles() {}

    public static float position(float ordinaryWind, float ordinaryMidpoint, float fluctuation) {
        float span = Math.max(0.20f, ordinaryMidpoint);
        return clamp(0.5f + (ordinaryWind - ordinaryMidpoint) * clamp(fluctuation, 0f, 1f) / span);
    }

    public static float sample(float position, float rain, float thunder) {
        float clear = inRange(position, WindConfig.CLEAR_WEATHER_MIN_STRENGTH.get(), WindConfig.CLEAR_WEATHER_MAX_STRENGTH.get());
        float wet = inRange(position, WindConfig.RAIN_WEATHER_MIN_STRENGTH.get(), WindConfig.RAIN_WEATHER_MAX_STRENGTH.get());
        float storm = inRange(position, WindConfig.THUNDER_WEATHER_MIN_STRENGTH.get(), WindConfig.THUNDER_WEATHER_MAX_STRENGTH.get());
        return interpolate(clear, wet, storm, rain, thunder);
    }

    /** Pure math entry point also used by the standalone validation tests. */
    public static float interpolate(float clear, float rainValue, float thunderValue, float rain, float thunder) {
        float wet = lerp(clamp(rain), clear, rainValue);
        return lerp(clamp(thunder), wet, thunderValue);
    }

    public static float inRange(float position, double first, double second) {
        float low = (float) Math.min(first, second);
        float high = (float) Math.max(first, second);
        return lerp(clamp(position), low, high);
    }

    public static float clamp(float value) { return clamp(value, 0f, 1f); }

    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }

    private static float lerp(float amount, float start, float end) {
        return start + amount * (end - start);
    }
}
