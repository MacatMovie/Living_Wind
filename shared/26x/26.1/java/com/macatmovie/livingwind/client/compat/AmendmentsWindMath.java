package com.macatmovie.livingwind.client.compat;

/** Dependency-free projection and bounded impulse for Amendments pendulums. */
public final class AmendmentsWindMath {
    private AmendmentsWindMath() {}

    /** Positive motion is around the axis supplied by Amendments' animation. */
    public static double projection(double windX, double windZ, double axisX, double axisZ) {
        double length = Math.hypot(axisX, axisZ);
        if (length < 1.0e-6 || !Double.isFinite(length)) return 0.0;
        // Amendments' positive pendulum angle moves opposite to the vector
        // cross-product used here, so negate it to swing WITH the wind.
        double component = (windZ * axisX - windX * axisZ) / length;
        return Double.isFinite(component) ? Math.max(-1.0, Math.min(1.0, component)) : 0.0;
    }

    /** One gentle angular-velocity impulse per client tick, NOT a render-angle override. */
    public static double impulse(double strength, double multiplier, double exposure,
                                 double projectedWind, double flutter) {
        if (!Double.isFinite(strength) || !Double.isFinite(multiplier) ||
                !Double.isFinite(exposure) || !Double.isFinite(projectedWind)) return 0.0;
        if (strength <= 0 || multiplier <= 0 || exposure <= 0) return 0.0;
        double localWind = Math.min(3.0, Math.max(0.0, strength * multiplier * exposure));
        double variability = 0.90 + 0.10 * Math.sin(flutter);
        return Math.max(-0.16, Math.min(0.16, 0.075 * localWind *
                Math.max(-1.0, Math.min(1.0, projectedWind)) * variability));
    }
}
