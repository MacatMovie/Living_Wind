package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import dev.nonamecrackers2.simpleclouds.api.SimpleCloudsAPI;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.api.common.world.ScAPICloudManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/** Optional Simple Clouds integration: sample local weather, never alter cloud movement. */
public final class SimpleCloudsWindCompat {
    private SimpleCloudsWindCompat() {}

    /** The class is referenced only when Simple Clouds is installed. */
    public static WindSnapshot atPlayer(ServerPlayer player, WindSnapshot wind) {
        var level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight()) return wind;

        ScAPICloudManager clouds = SimpleCloudsAPI.getApi().getCloudManager(level);
        if (clouds == null || clouds.shouldUseVanillaWeather()) return wind;

        float x = (float) player.getX();
        float z = (float) player.getZ();
        float y = (float) player.getY();
        var local = clouds.getCloudTypeAtPosition(x, z);
        if (local == null || local.getLeft() == null) return wind;

        WeatherType weather = local.getLeft().weatherType();
        // getRainLevel includes storm fade and altitude, avoiding wind bonuses outside rain cells.
        float rain = weather.includesRain() ? Mth.clamp(clouds.getRainLevel(x, y, z), 0.0f, 1.0f) : 0.0f;
        // The same weather-cell fade controls lightning-only storm areas too.
        float thunder = weather.includesThunder() ? Mth.clamp((0.7f - local.getRight()) / 0.1f, 0.0f, 1.0f) : 0.0f;
        float localBias = rain * WindConfig.RAIN_STRENGTH_BONUS.get().floatValue()
            + thunder * WindConfig.THUNDER_STRENGTH_BONUS.get().floatValue();
        // When Simple Clouds replaces vanilla weather, global rain/thunder bonuses do not
        // usually apply. Avoid accidentally stacking the same bonus when they do.
        float vanillaBias = (level.isRaining() ? WindConfig.RAIN_STRENGTH_BONUS.get().floatValue() : 0f)
            + (level.isThundering() ? WindConfig.THUNDER_STRENGTH_BONUS.get().floatValue() : 0f);
        float adjustment = (localBias - vanillaBias) * WindConfig.GLOBAL_STRENGTH_MULTIPLIER.get().floatValue();
        float limit = WindConfig.MAX_EFFECTIVE_STRENGTH.get().floatValue()
            * WindConfig.GLOBAL_STRENGTH_MULTIPLIER.get().floatValue();
        float strength = Mth.clamp(wind.strength() + adjustment, 0f, limit);
        return new WindSnapshot(strength, wind.directionDegrees(), wind.turbulence(),
            wind.directionInstability(), wind.gustStrength(), wind.windyPeriod(), wind.stormFront(), wind.gameTime());
    }
}
