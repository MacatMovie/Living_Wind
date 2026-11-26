package com.macatmovie.livingwind.compat;

/** Pure math for bounded wind-to-kinetic-RPM conversion; no Create dependency. */
public final class CreateWindmillScaling {
    private CreateWindmillScaling() {}

    /** A very faint but real kinetic rotation in perfectly still air. */
    public static final float CALM_RPM = 0.1f;
    private static final float CALM_BLEND_WIND = 0.1f;

    /** 0 wind -> minimum factor; 2 wind -> maximum factor. Normal winds are near vanilla speed. */
    public static float multiplier(float windStrength, float minimum, float maximum) {
        float t = Math.max(0f, Math.min(1f, windStrength / 2f));
        return minimum + (maximum - minimum) * t;
    }

    /** An RPM quantization step prevents tiny fluctuations from rebuilding kinetic networks. */
    public static float targetRpm(float vanillaRpm, float windStrength, float minimum, float maximum,
                                  float rpmStep) {
        if (vanillaRpm <= 0f) return 0f; // Unassembled Create windmills still stop.
        if (windStrength <= 0f) return CALM_RPM;

        float target = vanillaRpm * multiplier(windStrength, minimum, maximum);
        // Smoothly fade from exactly 0.1 RPM at calm to the normal wind-driven
        // curve. This avoids an abrupt 0.1 -> 1 RPM jump just above zero wind.
        if (windStrength < CALM_BLEND_WIND) {
            float blend = windStrength / CALM_BLEND_WIND;
            blend = blend * blend * (3f - 2f * blend); // smoothstep, zero slope at both ends
            target = CALM_RPM + (target - CALM_RPM) * blend;
        }

        // Sub-1 RPM speeds require fine quantization: the ordinary 1 RPM step
        // must not clamp a virtually stationary windmill to 1 RPM.
        float step = target < 1f ? 0.1f : Math.max(0.25f, rpmStep);
        return Math.max(CALM_RPM, Math.round(target / step) * step);
    }

    /** Limit step size on each update rather than instantly jumping to storm RPM. */
    public static float approachRpm(float current, float target, float maxStep, float minDelta) {
        // The normal 1 RPM change threshold would strand the bearing around
        // 1 RPM instead of letting it settle at the 0.1 RPM calm target.
        float threshold = target < 1f ? Math.min(0.05f, minDelta) : Math.max(0.01f, minDelta);
        if (Math.abs(target - current) < threshold) return current;
        float delta = Math.max(-Math.max(0.25f, maxStep), Math.min(Math.max(0.25f, maxStep), target - current));
        return current + delta;
    }
}
