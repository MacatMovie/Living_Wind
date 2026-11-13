package com.macatmovie.livingwind.api;

import com.macatmovie.livingwind.client.ClientWindState;

/**
 * Public client-side wind query API for optional visual integrations.
 * Mods should treat direction as degrees where 0 = +Z/south, 90 = -X/west, matching Minecraft yaw-style horizontal direction.
 */
public final class ClientWindApi {
    private ClientWindApi() {}

    /** Synchronized atmospheric strength before local altitude/exposure modifiers. */
    public static float globalStrength() { return ClientWindState.strength(); }

    /** Strength after altitude scaling but before shelter/exposure reduction. */
    public static float altitudeStrength() { return ClientWindState.altitudeStrength(); }

    /** Strength at the local player after altitude and shelter/exposure modifiers. */
    public static float localStrength() { return ClientWindState.localStrength(); }

    /** Position-aware strength (altitude + sampled biome), without position-specific shelter. */
    public static float strengthAt(double x, double y, double z) { return ClientWindState.strengthAt(x, y, z); }
    public static float windXAt(double x, double y, double z) { return ClientWindState.windXAt(x, y, z); }
    public static float windZAt(double x, double y, double z) { return ClientWindState.windZAt(x, y, z); }

    public static float directionDegrees() { return ClientWindState.directionDegrees(); }
    public static float globalWindX() { return ClientWindState.globalWindX(); }
    public static float globalWindZ() { return ClientWindState.globalWindZ(); }
    public static float localWindX() { return ClientWindState.localWindX(); }
    public static float localWindZ() { return ClientWindState.localWindZ(); }
    public static float turbulence() { return ClientWindState.turbulence(); }
    public static boolean windyPeriod() { return ClientWindState.windyPeriod(); }
    public static boolean stormFront() { return ClientWindState.stormFront(); }
    public static boolean hasWindState() { return ClientWindState.hasServerState(); }
}
