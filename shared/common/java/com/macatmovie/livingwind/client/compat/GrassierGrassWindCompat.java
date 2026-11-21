package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.util.Mth;

/**
 * Optional bridge for Grassier Grass 1.4.5. Its original shader/compute path
 * remains responsible for the rolling noise field, local blade geometry,
 * sky-light shelter and interaction trails; we replace only the wind inputs.
 * The foreign mod's JAR/classes are NOT linked or bundled here.
 */
public final class GrassierGrassWindCompat {
    private GrassierGrassWindCompat() {}

    /** Use stock Grassier Grass before the first server sync and when disabled. */
    public static boolean active() {
        return ClientWindConfig.GRASSIER_GRASS_WIND_COMPAT.get()
            && ClientWindState.hasServerState();
    }

    /** Grassier Grass uses effectiveWindSpeed / 100, typically around 1.0. */
    public static float bladeWindStrength() {
        if (ClientWindState.dimensionWindDisabled()) return 0.0f;
        float strength = ClientWindState.altitudeStrength()
            * ClientWindConfig.GRASSIER_GRASS_WIND_MULTIPLIER.get().floatValue();
        return Float.isFinite(strength) ? Mth.clamp(strength, 0.0f, 5.0f) : 0.0f;
    }

    public static float directionX() {
        return WindMath.windX(1.0f, ClientWindState.directionDegrees());
    }

    public static float directionZ() {
        return WindMath.windZ(1.0f, ClientWindState.directionDegrees());
    }

    /** Noise velocity only; the blade bend/flutter phases keep their strength. */
    public static float noiseScrollCoefficient(float nativeCoefficient) {
        if (!active()) return nativeCoefficient;
        return nativeCoefficient * ClientWindConfig.GRASSIER_GRASS_WAVE_SPEED_MULTIPLIER.get().floatValue();
    }

    /**
     * Grassier Grass directly creates its own short-lived detached blade
     * particles. This is their *native* effective wind input (0..500), not an
     * additional Living Wind force; it controls both spawn rate and glide.
     */
    public static float detachedBladeWindSpeed(float nativeSpeed) {
        if (!active()) return nativeSpeed;
        return 100.0f * bladeWindStrength();
    }
}
