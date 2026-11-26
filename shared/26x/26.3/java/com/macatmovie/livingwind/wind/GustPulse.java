package com.macatmovie.livingwind.wind;

/** Immutable snapshot of a natural gust, also used for overlapping cluster tails. */
public record GustPulse(long startTick, long endTick, float magnitude, float directionOffset,
                        int shape, float attackFraction, float releaseFraction) {
    public float envelope(long tick) {
        return envelope(tick, startTick, endTick, shape, attackFraction, releaseFraction);
    }

    public float contribution(long tick) { return magnitude * envelope(tick); }

    public static float envelope(long tick, long start, long end, int shape, float attack, float release) {
        if (tick < start || tick >= end || end <= start) return 0.0f;
        float t = (tick - start) / (float) (end - start);
        if (shape == 0) return (float) Math.sin(Math.PI * t);
        float a = clamp01(t / Math.max(0.01f, attack));
        float r = clamp01((1.0f - t) / Math.max(0.01f, release));
        return (a * a * (3.0f - 2.0f * a)) * (r * r * (3.0f - 2.0f * r));
    }

    /** Preserve the stronger pulse without summing all simultaneous peaks linearly. */
    public static float blend(float strongest, float total) {
        if (total <= 0.0f) return 0.0f;
        return Math.min(strongest + Math.max(0.0f, total - strongest) * 0.25f, strongest * 1.35f);
    }

    private static float clamp01(float v) { return Math.max(0.0f, Math.min(1.0f, v)); }
}
