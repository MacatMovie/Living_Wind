package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Converts Living Wind's position-aware server wind into Tumbleweed's existing
 * custom-wind velocity. The owning mixin writes the values to Tumbleweed's
 * vanilla synchronized entity data; clients run its original movement code.
 *
 * There is no static link to the optional Tumbleweed classes.
 */
public final class TumbleweedWindBridge {
    // Original Tumbleweed speed is sqrt(2) / 16 ~= 0.088 blocks per tick.
    private static final double SPEED_AT_STRENGTH_ONE = 0.088;
    private static final double MAX_SPEED = 0.55;

    private TumbleweedWindBridge() {}

    public static boolean allowed(Entity tumbleweed) {
        return tumbleweed != null && tumbleweed.level() instanceof ServerLevel server
            && !tumbleweed.isRemoved() && !WindDimensionBlacklist.contains(server);
    }

    /** Caller must be on the server tick thread, with a loaded position. */
    public static Vec3 velocity(Entity tumbleweed, double originalVariation, double multiplier) {
        if (!(tumbleweed.level() instanceof ServerLevel level)) return Vec3.ZERO;
        WindSnapshot wind = WindApi.getAt(level, tumbleweed.blockPosition());
        return fromWind(wind.windX(), wind.windZ(), originalVariation, multiplier);
    }

    /** Pure numerical conversion, also usable in isolated tests. */
    public static Vec3 fromWind(double x, double z, double originalVariation, double multiplier) {
        double strength = Math.hypot(x, z);
        if (!Double.isFinite(strength) || strength < 1.0e-5
            || !Double.isFinite(multiplier) || multiplier <= 0.0) return Vec3.ZERO;
        // Keep Tumbleweed's native +/- 5% individual variation, while letting
        // calm conditions naturally decay under its existing 0.98 friction.
        double variation = Double.isFinite(originalVariation)
            ? Math.max(0.9, Math.min(1.1, originalVariation)) : 1.0;
        // Strength 1 remains near the native roll speed. The requested stronger
        // response is quadratic: strength 2 is twice as fast as our former
        // linear response at strength 2 (0.352 vs 0.176 blocks/tick).
        double speed = Math.min(MAX_SPEED, SPEED_AT_STRENGTH_ONE * strength * strength * multiplier * variation);
        return new Vec3(x / strength * speed, 0.0, z / strength * speed);
    }
}
