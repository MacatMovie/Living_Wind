package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.config.ClientWindConfig;
import net.minecraft.util.Mth;

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
     * Resolve shelter at the SMOKE particle's own position, never at the player.
     * Uses the same bounded, worker-safe shelter cache and short grace period
     * as Living Wind's normal particles. Unknown cells default to outdoor wind,
     * not the observing player's indoor/outdoor state. A recently measured
     * sheltered particle briefly keeps that protection across cache misses.
     * No new world/block queries or cache budgets are introduced here.
     */
    public static float exposure(WindParticleAccess particle) {
        double x = particle.livingwind$getPositionX();
        double y = particle.livingwind$getPositionY();
        double z = particle.livingwind$getPositionZ();
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return 0.0f;

        float measured = WindShelter.trackedParticleExposureAt(x, y, z);
        int now = WindShelter.shelterTick();
        int bx = Mth.floor(x), by = Mth.floor(y), bz = Mth.floor(z);
        if (measured >= 0.0f && measured < 0.20f) {
            particle.livingwind$rememberSheltered(now, bx, by, bz);
        }
        float effective = ParticleShelterContinuity.resolve(
            measured, now, particle.livingwind$getLastShelteredTick(),
            bx, by, bz,
            particle.livingwind$getLastShelteredX(),
            particle.livingwind$getLastShelteredY(),
            particle.livingwind$getLastShelteredZ());
        return effective;
    }

    public static double targetX(float exposure) {
        return ClientWindState.altitudeWindX() * SMOKE_SPEED_PER_STRENGTH * exposure;
    }

    public static double targetZ(float exposure) {
        return ClientWindState.altitudeWindZ() * SMOKE_SPEED_PER_STRENGTH * exposure;
    }
}
