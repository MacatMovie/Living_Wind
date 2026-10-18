package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Optional loader bridge for wind instruments mounted on moving Sable plots.
 * Ordinary blocks have an identity transform. No Sable classes are referenced here,
 * so Forge 1.20.1 and NeoForge installations without Sable remain independent. */
public final class InstrumentSpatialContext {
    @FunctionalInterface
    public interface Resolver {
        Frame resolve(Level level, BlockPos position, float partialTick);
    }

    /** Two world-space horizontal components of each local axis. A 3-D pose is
     * projected onto the instrument's horizontal rotation plane. */
    public record Frame(BlockPos worldPosition, boolean moving,
                        double rightX, double rightZ, double forwardX, double forwardZ) {
        public static Frame stationary(BlockPos position) {
            return new Frame(position, false, 1.0, 0.0, 0.0, 1.0);
        }

        /** Living Wind bearings indicate where the air travels TO: 0=S, 90=W. */
        public float localBearing(float worldBearing) {
            if (!moving) return WindMath.normalizeDegrees(worldBearing);
            double dx = WindMath.windX(1.0f, worldBearing);
            double dz = WindMath.windZ(1.0f, worldBearing);
            double localX = dx * rightX + dz * rightZ;
            double localZ = dx * forwardX + dz * forwardZ;
            // A vertical instrument can be perpendicular to horizontal wind;
            // hold the previous behavior rather than producing NaN/undefined yaw.
            if (localX * localX + localZ * localZ < 1.0e-7) {
                return WindMath.normalizeDegrees(worldBearing);
            }
            return WindMath.normalizeDegrees((float) Math.toDegrees(Math.atan2(-localX, localZ)));
        }
    }

    private static volatile Resolver client = (level, position, partial) -> Frame.stationary(position);
    private static volatile Resolver server = (level, position, partial) -> Frame.stationary(position);

    private InstrumentSpatialContext() {}

    public static void setClientResolver(Resolver resolver) { client = resolver; }
    public static void setServerResolver(Resolver resolver) { server = resolver; }

    public static Frame at(Level level, BlockPos position, float partialTick) {
        return (level.isClientSide ? client : server).resolve(level, position, partialTick);
    }
}
