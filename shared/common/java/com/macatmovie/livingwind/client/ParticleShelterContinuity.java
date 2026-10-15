package com.macatmovie.livingwind.client;

/**
 * Tiny per-particle shelter continuity rule. No world queries, allocations, global caches,
 * or thread affinity: inputs are immutable snapshots from the particle and WindShelter.
 *
 * Unknown shelter (-1) is NOT proof of outdoor wind. For up to 6 active ticks, a
 * particle that was confirmed indoors can cross into an unsampled adjacent block
 * without receiving a one-frame outdoor gust. Brief moderate readings near a wall or
 * opening also require 4 ticks of separation from confirmed shelter to take effect.
 * A confirmed exposed reading (>= 0.65) is always applied immediately.
 */
public final class ParticleShelterContinuity {
    private static final int MISS_GRACE_TICKS = 6;
    private static final int PARTIAL_GRACE_TICKS = 3;
    private static final float SHELTER_THRESHOLD = 0.20f;
    private static final float CLEARLY_EXPOSED = 0.65f;

    private ParticleShelterContinuity() {}

    /** measured: -1 for unknown, otherwise adjusted exposure [0, 1]. */
    public static float resolve(float measured, int now, int lastShelteredTick,
                                int x, int y, int z, int shelteredX, int shelteredY, int shelteredZ) {
        if (measured >= 0.0f && measured < SHELTER_THRESHOLD) return 0.0f;
        if (measured >= CLEARLY_EXPOSED) return measured;

        // The age and distance checks keep genuinely outdoor particles fully responsive.
        if (lastShelteredTick >= 0 && now >= lastShelteredTick
                && now - lastShelteredTick <= MISS_GRACE_TICKS
                && Math.abs(x - shelteredX) <= 1
                && Math.abs(y - shelteredY) <= 2
                && Math.abs(z - shelteredZ) <= 1) {
            if (measured < 0.0f) return 0.0f;
            if (now - lastShelteredTick <= PARTIAL_GRACE_TICKS) return 0.0f;
        }
        return measured < 0.0f ? 1.0f : measured;
    }
}
