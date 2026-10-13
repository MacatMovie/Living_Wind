package com.macatmovie.livingwind.api;

import com.macatmovie.livingwind.server.WindSavedData;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.util.Mth;

/** Lightweight server-side query API for future integrations such as windmills or mob rules. */
public final class WindApi {
    private WindApi() {}
    public static WindSnapshot get(ServerLevel level) {
        return WindSavedData.get(level).snapshot(level.getGameTime());
    }

    /** Atmospheric wind with the configured Overworld altitude multiplier applied at {@code pos}. */
    public static WindSnapshot getAt(ServerLevel level, BlockPos pos) {
        WindSnapshot base = get(level);
        float multiplier = altitudeMultiplier(level, pos.getY());
        return new WindSnapshot(base.strength() * multiplier, base.directionDegrees(), base.turbulence(),
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
