package com.macatmovie.livingwind.wind;

public record WindSnapshot(
    float strength,
    float directionDegrees,
    float turbulence,
    float directionInstability,
    float gustStrength,
    boolean windyPeriod,
    boolean stormFront,
    long gameTime
) {
    public float windX() { return WindMath.windX(strength, directionDegrees); }
    public float windZ() { return WindMath.windZ(strength, directionDegrees); }
}
