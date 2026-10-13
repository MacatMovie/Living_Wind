package com.macatmovie.livingwind.wind;

import net.minecraft.util.Mth;

public final class WindMath {
    private WindMath() {}

    public static float normalizeDegrees(float degrees) {
        float d = degrees % 360.0f;
        return d < 0.0f ? d + 360.0f : d;
    }

    public static float shortestDeltaDegrees(float from, float to) {
        return Mth.wrapDegrees(to - from);
    }

    public static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(current + amount, target);
        return Math.max(current - amount, target);
    }

    public static float approachDegrees(float current, float target, float amount) {
        float delta = shortestDeltaDegrees(current, target);
        if (Math.abs(delta) <= amount) return normalizeDegrees(target);
        return normalizeDegrees(current + Math.copySign(amount, delta));
    }

    /**
     * Visual particle drift adjustment, not an atmospheric-strength multiplier.
     * At local wind 2.0 it is 1.60x, tapering toward 1.0x in calm air.
     * The server weather, wind-frequency distribution and sound thresholds stay unchanged.
     */
    public static float visualDriftBoost(float strength) {
        return 1.0f + 0.60f * (float)Math.pow(Mth.clamp(strength / 2.0f, 0.0f, 1.0f), 1.10);
    }

    // Direction convention follows Minecraft yaw: 0=south (+Z), 90=west (-X).
    public static float windX(float strength, float directionDegrees) {
        return (float) (-Math.sin(Math.toRadians(directionDegrees)) * strength);
    }

    public static float windZ(float strength, float directionDegrees) {
        return (float) (Math.cos(Math.toRadians(directionDegrees)) * strength);
    }
}
