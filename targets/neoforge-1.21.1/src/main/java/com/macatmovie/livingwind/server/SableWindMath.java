package com.macatmovie.livingwind.server;

/** Pure, bounded force calculations, with no Minecraft or Sable dependencies. */
public final class SableWindMath {
    private SableWindMath() {}

    public static double projectedArea(double hx, double hy, double hz, double dx, double dy, double dz) {
        return 4.0 * (hx * hy * Math.abs(dz) + hy * hz * Math.abs(dx) + hx * hz * Math.abs(dy));
    }

    public static double pressure(double coefficient, double multiplier, double windStrength,
                                  double area, double catchRatio, double forceCap) {
        if (!(coefficient > 0) || !(multiplier > 0) || !(windStrength > 0)
                || !(area > 0) || !(catchRatio > 0) || !(forceCap > 0)) return 0;
        double result = coefficient * multiplier * windStrength * windStrength * area * catchRatio;
        return Double.isFinite(result) ? Math.min(result, forceCap) : forceCap;
    }

    public static double windX(double directionDegrees) {
        return -Math.sin(Math.toRadians(directionDegrees));
    }

    public static double windZ(double directionDegrees) {
        return Math.cos(Math.toRadians(directionDegrees));
    }
}
