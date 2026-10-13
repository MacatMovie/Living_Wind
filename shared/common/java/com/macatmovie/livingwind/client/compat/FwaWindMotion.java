package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional FWA animation math. Never accesses FWA classes and never walks the world.
 * Rotations are in DEGREES for FWA chains/lanterns and RADIANS for FWA bells.
 * FWA remains responsible for all rendering, chain-length limits and bell ringing.
 */
public final class FwaWindMotion {
    public record Angles(float x, float z, float spin) {}
    public enum ObjectType { CHAIN, LANTERN, BELL }

    // FWA can replace an animation object following block/chunk updates. A per-instance
    // smoother starts at native FWA angles and briefly looks like wind has vanished.
    // Retain the swing state by position and animation type across those replacements.
    private record MotionKey(long pos, ObjectType type) {}
    private static final class MotionEntry {
        final Smoother motion = new Smoother();
        volatile long lastSeenNs;
        MotionEntry(long now) { lastSeenNs = now; }
    }
    private static final ConcurrentHashMap<MotionKey, MotionEntry> MOTIONS = new ConcurrentHashMap<>();
    private static final int MAX_MOTIONS = 512;
    private static final long UNUSED_NANOS = 30_000_000_000L;
    private static volatile long lastPruneNs;

    public static Smoother smootherFor(BlockPos pos, ObjectType type) {
        long now = System.nanoTime();
        MotionKey key = new MotionKey(pos.asLong(), type);
        MotionEntry entry = MOTIONS.computeIfAbsent(key, k -> new MotionEntry(now));
        // Reuse the phase and angle filter even if FWA reconstructs its animation object.
        entry.lastSeenNs = now;
        // Clean infrequently; no world queries, and only short-lived per-block state.
        if (now - lastPruneNs > 10_000_000_000L && (MOTIONS.size() > MAX_MOTIONS || now - lastPruneNs > UNUSED_NANOS)) {
            lastPruneNs = now;
            MOTIONS.entrySet().removeIf(e -> now - e.getValue().lastSeenNs > UNUSED_NANOS);
            if (MOTIONS.size() > MAX_MOTIONS) {
                int extra = MOTIONS.size() - MAX_MOTIONS;
                for (MotionKey stale : MOTIONS.keySet()) {
                    if (extra-- <= 0) break;
                    MOTIONS.remove(stale);
                }
            }
        }
        return entry.motion;
    }

    public static void reset() { MOTIONS.clear(); lastPruneNs = 0L; }

    private FwaWindMotion() {}

    /**
     * Render-time low-pass filter, kept on each FWA animation instance rather than globally.
     * This prevents abrupt shelter-cache changes, packet updates, and the first appearance of
     * a block animation from snapping between FWA's original and Living Wind's orientations.
     * It does not query the world or allocate during rendering.
     */
    public static final class Smoother {
        private boolean initialized;
        private long previousNs;
        private long previousPhaseNs;
        private boolean phaseInitialized;
        private double phase;
        private float x, z, spin;

        /**
         * Integrate phase incrementally. Never multiply a massive absolute nanoTime by a
         * changing frequency: doing so made tiny changes in wind strength jump the phase,
         * which visibly snapped lanterns/chains back and forth.
         */
        public float pulse(float offset, float radiansPerSecond) {
            return pulse(offset, radiansPerSecond, System.nanoTime());
        }

        public float pulse(float offset, float radiansPerSecond, long nowNs) {
            if (!phaseInitialized) {
                phaseInitialized = true;
                previousPhaseNs = nowNs;
                phase = offset;
            } else {
                double seconds = Math.max(0.0, Math.min((nowNs - previousPhaseNs) * 1e-9, 0.10));
                previousPhaseNs = nowNs;
                phase = (phase + radiansPerSecond * seconds) % (Math.PI * 2.0);
            }
            return (float)Math.sin(phase);
        }

        /** Start at FWA's own initial pose rather than instantly snapping to a wind lean. */
        public void begin(Angles original) {
            if (initialized) return;
            initialized = true;
            previousNs = System.nanoTime();
            x = original.x(); z = original.z(); spin = original.spin();
        }

        public Angles update(Angles target) {
            return update(target, System.nanoTime());
        }

        /** Package-independent clock overload for deterministic validation. */
        public Angles update(Angles target, long nowNs) {
            if (!initialized) {
                initialized = true;
                previousNs = nowNs;
                x = target.x(); z = target.z(); spin = target.spin();
                return target;
            }
            double seconds = (nowNs - previousNs) * 1e-9;
            previousNs = nowNs;
            // A long rendering gap should not preserve a stale pose on reappearance.
            if (seconds > 1.5 || seconds < 0) {
                x = target.x(); z = target.z(); spin = target.spin();
            } else {
                // 0.22-second response constant: noticeable but not sluggish.
                double alpha = 1.0 - Math.exp(-Math.min(seconds, 0.10) / 0.22);
                x += (target.x() - x) * (float)alpha;
                z += (target.z() - z) * (float)alpha;
                spin += (target.spin() - spin) * (float)alpha;
            }
            return new Angles(x, z, spin);
        }

        public void reset() { initialized = false; phaseInitialized = false; }
    }

    public static Angles swinging(BlockPos pos, float originalX, float originalZ, float originalSpin, ObjectType type, Smoother motion) {
        if (!ClientWindConfig.FWA_WIND_COMPAT.get() || !ClientWindState.hasServerState()) {
            return new Angles(originalX, originalZ, originalSpin);
        }
        float multiplier = (type == ObjectType.CHAIN
            ? ClientWindConfig.FWA_CHAIN_INTENSITY.get() : ClientWindConfig.FWA_LANTERN_INTENSITY.get()).floatValue();
        float impact = ClientWindConfig.FWA_WIND_IMPACT.get().floatValue();
        float wind = localWind(pos);
        // Zero global impact means entirely stock FWA animation, not a reduced stock sway.
        if (impact <= 0.0f) return new Angles(originalX, originalZ, originalSpin);
        if (multiplier <= 0.0f) return new Angles(originalX * 0.15f, originalZ * 0.15f, originalSpin * 0.15f);

        // FWA already supplies smooth position-unique oscillations. Retain some of this
        // secondary motion, while the dominant lean follows Living Wind's real direction.
        float magnitude = Mth.clamp(wind / 2.0f, 0.0f, 1.2f);
        float secondary = 0.14f + magnitude * 0.35f;
        float gust = Math.min(0.6f, ClientWindState.gustStrength() * 0.22f);
        // Global FWA impact defaults to 6x the original directional lean. Limit only
        // the FINAL result, avoiding the old 35/28-degree cap swallowing the increase.
        // Deliberately capped to keep the attached geometry reasonably legible.
        float amplitude = (type == ObjectType.LANTERN ? 20.0f : 15.0f)
            * multiplier * impact * (float)Math.pow(magnitude, 1.12) * (1.0f + gust);
        amplitude = Mth.clamp(amplitude, 0.0f, type == ObjectType.LANTERN ? 65.0f : 55.0f);

        // Chain and attached lanterns at the same X/Z share the same wind pulse phase.
        // Original FWA oscillations remain unique to each object (for natural variation).
        float phaseOffset = (pos.getX() * 0.16f + pos.getZ() * 0.19f);
        float pulse = 0.67f + 0.33f * motion.pulse(phaseOffset, 0.85f + magnitude * 0.85f);
        float wx = WindMath.windX(1.0f, ClientWindState.directionDegrees());
        float wz = WindMath.windZ(1.0f, ClientWindState.directionDegrees());
        // FWA uses standard right-handed JOML rotations on a downward-hanging model:
        // positive X-rotation moves the lower tip toward -Z, while positive Z moves it
        // toward +X. Hence NEGATE the Z wind component for X-tilt, and use positive
        // X wind component for Z-tilt. The old signs bent against the wind.
        float x = originalX * secondary - wz * amplitude * pulse;
        float z = originalZ * secondary + wx * amplitude * pulse;
        float spin = originalSpin * (0.18f + magnitude * 0.35f);
        return new Angles(x, z, spin);
    }

    /** Modify only FWA's idle rocking; its placement and actual bell-ring paths are untouched. */
    public static float bellIdle(BlockPos pos, float original, float signedWindProjection, Smoother motion) {
        if (!ClientWindConfig.FWA_WIND_COMPAT.get() || !ClientWindState.hasServerState()) return original;
        float multiplier = ClientWindConfig.FWA_BELL_INTENSITY.get().floatValue();
        float impact = ClientWindConfig.FWA_WIND_IMPACT.get().floatValue();
        if (impact <= 0.0f) return original;
        float wind = localWind(pos);
        float magnitude = Mth.clamp(wind / 2.0f, 0.0f, 1.2f);
        // Projection is calculated from FWA's facing and attachment geometry by the
        // bell Mixin. It is the displacement of the bell's tip for a positive 'rot'.
        float alongAxis = signedWindProjection;
        // Heavy bells respond only along their hinge, with much less motion than lanterns.
        float phaseOffset = pos.getX() * 0.21f + pos.getZ() * 0.17f;
        float oscillation = motion.pulse(phaseOffset, 0.75f + magnitude * 0.55f);
        // FWA bell rotations are RADIANS. Sixfold default response remains restrained
        // compared with chains/lanterns; clamp to protect bell mounts at extreme values.
        float windRock = alongAxis * multiplier * impact * magnitude * (0.024f + 0.018f * oscillation);
        windRock = Mth.clamp(windRock, -0.26f, 0.26f);
        return original * (0.22f + 0.45f * magnitude) + windRock;
    }

    private static float localWind(BlockPos pos) {
        if (!ClientWindState.hasServerState()) return 0.0f;
        float exposure = WindShelter.exposureForStaticAnimation(pos);
        return ClientWindState.altitudeStrength() * exposure;
    }
}
