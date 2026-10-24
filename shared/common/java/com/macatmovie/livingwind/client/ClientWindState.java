package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.network.WindSyncPayload;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.util.Mth;

/**
 * Thread-readable client state. Particle hooks only read primitive volatile fields here and never
 * touch level/chunk state, keeping the per-particle path small and friendlier to async particle mods.
 */
public final class ClientWindState {
    private static volatile float targetStrength;
    private static volatile float targetDirection;
    private static volatile float currentStrength;
    private static volatile float currentDirection;
    private static volatile float turbulence;
    private static volatile float directionInstability;
    private static volatile float gustStrength;
    private static volatile float exposure = 1.0f;
    private static volatile float altitudeMultiplier = 1.0f;
    private static volatile boolean altitudeScalingEnabled = true;
    private static volatile int altitudeMinY = 64;
    private static volatile int altitudeMaxY = 192;
    private static volatile float altitudeMinMultiplier = 1.0f;
    private static volatile float altitudeMaxMultiplier = 1.35f;
    private static volatile boolean windyPeriod;
    private static volatile boolean stormFront;
    private static volatile boolean createWindmillsEnabled;
    private static volatile boolean highSeasWindEnabled;
    private static volatile boolean smallShipsWindEnabled;
    private static volatile boolean hasServerState;

    private ClientWindState() {}

    public static void accept(WindSyncPayload payload) {
        targetStrength = payload.strength();
        targetDirection = WindMath.normalizeDegrees(payload.directionDegrees());
        turbulence = payload.turbulence();
        directionInstability = payload.directionInstability();
        gustStrength = payload.gustStrength();
        windyPeriod = payload.windyPeriod();
        stormFront = payload.stormFront();
        createWindmillsEnabled = payload.createWindmillsEnabled();
        smallShipsWindEnabled = payload.smallShipsWindEnabled();

        altitudeScalingEnabled = payload.altitudeScalingEnabled();
        altitudeMinY = payload.altitudeMinY();
        altitudeMaxY = payload.altitudeMaxY();
        altitudeMinMultiplier = payload.altitudeMinMultiplier();
        altitudeMaxMultiplier = payload.altitudeMaxMultiplier();
        if (!hasServerState) {
            currentStrength = targetStrength;
            currentDirection = targetDirection;
        }
        hasServerState = true;
    }

    public static void tick() {
        if (!hasServerState) return;
        currentStrength = Mth.lerp(0.10f, currentStrength, targetStrength);
        float delta = WindMath.shortestDeltaDegrees(currentDirection, targetDirection);
        currentDirection = WindMath.normalizeDegrees(currentDirection + delta * 0.10f);
    }

    public static void reset() {
        targetStrength = currentStrength = 0.0f;
        targetDirection = currentDirection = 0.0f;
        turbulence = directionInstability = gustStrength = 0.0f;
        exposure = 1.0f;
        altitudeMultiplier = 1.0f;
        windyPeriod = stormFront = createWindmillsEnabled = highSeasWindEnabled = smallShipsWindEnabled = false;
        hasServerState = false;
    }

    public static void setExposure(float value) { exposure = Mth.clamp(value, 0.0f, 1.0f); }
    public static void updateAltitude(int y, boolean overworld) {
        if (!altitudeScalingEnabled || !overworld) {
            altitudeMultiplier = 1.0f;
            return;
        }
        if (altitudeMaxY <= altitudeMinY) {
            altitudeMultiplier = altitudeMaxMultiplier;
            return;
        }
        float progress = Mth.clamp((y - altitudeMinY) / (float) (altitudeMaxY - altitudeMinY), 0.0f, 1.0f);
        altitudeMultiplier = Mth.lerp(progress, altitudeMinMultiplier, altitudeMaxMultiplier);
    }
    public static float strength() { return currentStrength; }
    public static float targetStrength() { return targetStrength; }
    public static float directionDegrees() { return currentDirection; }
    public static float targetDirectionDegrees() { return targetDirection; }
    public static float turbulence() { return turbulence; }
    public static float directionInstability() { return directionInstability; }
    public static float gustStrength() { return gustStrength; }
    public static float exposure() { return exposure; }
    public static float altitudeMultiplier() { return altitudeMultiplier; }
    public static boolean windyPeriod() { return windyPeriod; }
    public static boolean stormFront() { return stormFront; }
    public static boolean createWindmillsEnabled() { return hasServerState && createWindmillsEnabled; }
    public static boolean hasServerState() { return hasServerState; }
    public static boolean highSeasWindEnabled() { return hasServerState && highSeasWindEnabled; }
    public static boolean smallShipsWindEnabled() { return hasServerState && smallShipsWindEnabled; }
    // Maintained as a platform-neutral no-op in Forge. NeoForge's synced flag is
    // consumed through a small method in its platform-specific networking code.
    public static void setHighSeasWindEnabled(boolean enabled) { highSeasWindEnabled = enabled; }
    public static float altitudeStrength() { return currentStrength * altitudeMultiplier; }

    /** Sample altitude at a moving instrument's actual world Y rather than at
     * the observing player. Static effects retain their existing cached value. */
    public static float strengthAtAltitude(int y, boolean overworld) {
        if (!altitudeScalingEnabled || !overworld) return currentStrength;
        if (altitudeMaxY <= altitudeMinY) return currentStrength * altitudeMaxMultiplier;
        float progress = Mth.clamp((y - altitudeMinY) / (float) (altitudeMaxY - altitudeMinY), 0.0f, 1.0f);
        return currentStrength * Mth.lerp(progress, altitudeMinMultiplier, altitudeMaxMultiplier);
    }
    public static float localStrength() { return altitudeStrength() * exposure; }
    public static float globalWindX() { return WindMath.windX(currentStrength, currentDirection); }
    public static float globalWindZ() { return WindMath.windZ(currentStrength, currentDirection); }
    public static float altitudeWindX() { return WindMath.windX(altitudeStrength(), currentDirection); }
    public static float altitudeWindZ() { return WindMath.windZ(altitudeStrength(), currentDirection); }
    public static float localWindX() { return WindMath.windX(localStrength(), currentDirection); }
    public static float localWindZ() { return WindMath.windZ(localStrength(), currentDirection); }
}
