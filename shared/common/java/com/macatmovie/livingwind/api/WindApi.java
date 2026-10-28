package com.macatmovie.livingwind.api;

import com.macatmovie.livingwind.server.WindSavedData;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindBiomeModifiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.util.Mth;

/** Lightweight server-side query API for future integrations such as windmills or mob rules. */
public final class WindApi {
    private WindApi() {}
    public static WindSnapshot get(ServerLevel level) {
        if (WindDimensionBlacklist.contains(level)) return WindDimensionBlacklist.calm(level.getGameTime());
        return WindSavedData.get(level).snapshot(level.getGameTime());
    }

    /**
     * Position-aware wind: altitude plus built-in biome modifiers. The dimension-wide
     * {@link #get(ServerLevel)} reading remains unchanged for existing integrations.
     * Call from the server thread with a loaded position, never on async worker threads.
     */
    public static WindSnapshot getAt(ServerLevel level, BlockPos pos) {
        WindSnapshot base = get(level);
        float strength = base.strength() * altitudeMultiplier(level, pos.getY());
        if (level.dimension() == Level.NETHER && !WindDimensionBlacklist.contains(level)
                && level.getBiome(pos).is(WindBiomeModifiers.BLACK_ICE_GLACIERS)) {
            strength = WindBiomeModifiers.apply(strength, 1.0f);
        }
        return new WindSnapshot(strength, base.directionDegrees(), base.turbulence(),
            base.directionInstability(), base.gustStrength(), base.windyPeriod(), base.stormFront(), base.gameTime());
    }

    public static float altitudeMultiplier(ServerLevel level, int y) {
        if (!WindConfig.ALTITUDE_SCALING_ENABLED.get() || level.dimension() != Level.OVERWORLD) return 1.0f;
        int minY = WindConfig.ALTITUDE_MIN_Y.get();
        int maxY = WindConfig.ALTITUDE_MAX_Y.get();
        float min = WindConfig.ALTITUDE_MIN_MULTIPLIER.get().floatValue();
        float max = WindConfig.ALTITUDE_MAX_MULTIPLIER.get().floatValue();
        if (maxY <= minY) return max;
        float t = Mth.clamp((y - minY) / (float) (maxY - minY), 0.0f, 1.0f);
        return Mth.lerp(t, min, max);
    }
}
