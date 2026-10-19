package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.config.ClientWindConfig;

/**
 * Outdoor wind vector for Advanced Chimneys smoke. Called ONLY through its
 * optional smoke-particle mixin, so no code runs without Advanced Chimneys.
 * Particle threads only consult cached shelter/water data; never inspect chunks.
 */
public final class AdvancedChimneysSmokeWind {
    // World blocks per smoke tick per unit wind strength. Advanced Chimneys
    // smoke was visibly less wind-responsive than regular smoke at 0.075.
    // Double only this optional compatibility path, keeping chimney routing,
    // shelter, wind direction and vertical motion unchanged.
    private static final double SMOKE_SPEED_PER_STRENGTH = 0.15;

    private AdvancedChimneysSmokeWind() {}

    public static boolean active() {
        return ClientWindConfig.ADVANCED_CHIMNEYS_WIND_COMPAT.get()
            && ClientWindState.hasServerState();
    }

    /**
     * -1 means the async-safe shelter survey has not yet published this cell.
     * The smoke mixin remembers its last reading and uses the player's current
     * local exposure only on the very first miss, so newly emitted campfire
     * smoke doesn't inherit Advanced Chimneys' unrelated wind while queued.
     */
    public static float exposure(WindParticleAccess particle) {
        double x = particle.livingwind$getPositionX();
        double y = particle.livingwind$getPositionY();
        double z = particle.livingwind$getPositionZ();
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return 0.0f;
        return WindShelter.trackedParticleExposureAt(x, y, z);
    }

    public static float initialExposure() {
        return ClientWindState.exposure();
    }

    public static double targetX(float exposure) {
        return ClientWindState.altitudeWindX() * SMOKE_SPEED_PER_STRENGTH * exposure;
    }

    public static double targetZ(float exposure) {
        return ClientWindState.altitudeWindZ() * SMOKE_SPEED_PER_STRENGTH * exposure;
    }
}
