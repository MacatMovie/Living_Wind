package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Optional Delight o' Flight integration for its Phantom Membrane Balloon.
 *
 * Delight o' Flight moves the balloon with setPos() rather than velocity, so
 * generic entity-wind impulses are never applied. This runs after the mod's
 * original tick, preserving its rise rate, inventory drops and 200-block
 * lifetime. Its vanilla entity-position updates synchronize the result to
 * clients; no client prediction or additional network payload is needed.
 *
 * No classes from Delight o' Flight are referenced at compile or runtime.
 */
public final class DelightOfFlightBalloonWindBridge {
    private static final int RISE_START_TICK = 10;
    private static final double DRIFT_PER_STRENGTH = 0.075D;
    private static final double MAX_DRIFT_PER_TICK = 0.18D;

    private DelightOfFlightBalloonWindBridge() {}

    public static void afterBalloonTick(Entity balloon, boolean enabled, double multiplier) {
        if (!enabled || multiplier <= 0.0D || balloon == null || balloon.isRemoved()
                || balloon.tickCount <= RISE_START_TICK
                || !(balloon.level() instanceof ServerLevel server)
                || WindDimensionBlacklist.contains(server)) return;

        WindSnapshot wind = WindApi.getAt(server, balloon.blockPosition());
        double driftX = wind.windX() * DRIFT_PER_STRENGTH * multiplier;
        double driftZ = wind.windZ() * DRIFT_PER_STRENGTH * multiplier;
        if (!Double.isFinite(driftX) || !Double.isFinite(driftZ)) return;

        double speed = Math.hypot(driftX, driftZ);
        if (speed < 0.0001D) return;
        if (speed > MAX_DRIFT_PER_TICK) {
            double factor = MAX_DRIFT_PER_TICK / speed;
            driftX *= factor;
            driftZ *= factor;
        }

        // The parent mod uses direct positioning to rise. Match its model while
        // preventing the new horizontal motion from clipping into solid walls.
        AABB nextBounds = balloon.getBoundingBox().move(driftX, 0.0D, driftZ);
        if (!server.noCollision(balloon, nextBounds)) return;

        double nextX = balloon.getX() + driftX;
        double nextZ = balloon.getZ() + driftZ;
        if (!server.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(nextX, balloon.getY(), nextZ))) return;
        balloon.setPos(nextX, balloon.getY(), nextZ);
    }
}
