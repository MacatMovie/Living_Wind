package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Optional, dependency-free directional movement for Biome Particle Weather.
 *
 * Versions in the 1.20.1 Impactful Weather source use impactfulweather:* IDs,
 * while the uploaded 5.3.1 (Fabric 1.21.1) build uses biomeparticleweather:*
 * and renamed particle types. Keep the compatibility restricted to actual
 * *moving weather* particles; do not steer splashes, surface ripples, fireflies,
 * lightning, or immobile fog/particle decals.
 *
 * Used from Particle.move's existing Living Wind hook because older weather
 * particles reset their velocities *inside* tick(), overriding any HEAD injection.
 * Replace horizontal movement, rather than adding a second generic wind push.
 */
public final class BiomeParticleWeatherWindCompat {
    private BiomeParticleWeatherWindCompat() {}

    /** Per-type movement (blocks/tick at Living Wind strength = 1). */
    private static float baseSpeed(@Nullable ResourceLocation id) {
        if (id == null) return Float.NaN;
        return switch (id.getNamespace()) {
            case "impactfulweather" -> switch (id.getPath()) {
                case "rain", "heavyrain", "heavyrainext" -> 0.45f;
                case "snow" -> 0.16f;
                case "blizzardsnow" -> 1.10f;
                case "sandmote", "redsandmote" -> 0.72f;
                case "tumblebush" -> 0.40f;
                default -> Float.NaN;
            };
            case "biomeparticleweather" -> switch (id.getPath()) {
                // Rain trails are separate 5-tick particles spawned by each
                // rain drop. They must share the parent's horizontal movement,
                // or a slanted shower visually splits into multiple droplets.
                case "rain", "rain_trail" -> 0.30f;
                case "snow" -> 0.15f;
                case "blizzard_snow" -> 0.90f;
                case "wind", "blizzard_wind" -> 0.60f;
                case "sand_mote", "red_sand_mote" -> 0.60f;
                case "tumble_bush" -> 0.38f;
                default -> Float.NaN;
            };
            default -> Float.NaN;
        };
    }

    public static boolean isWeatherParticle(@Nullable ResourceLocation id) {
        return !Float.isNaN(baseSpeed(id));
    }

    /** NaN means 'not handled'; the original Particle.move argument is preserved. */
    public static double replacementX(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, true);
    }

    public static double replacementZ(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, false);
    }

    private static double replacement(@Nullable ResourceLocation id, Particle particle, boolean xAxis) {
        if (!ParticleWindFilter.allowsBiomeParticleWeatherSteering(id, particle)) return Double.NaN;
        var access = (com.macatmovie.livingwind.client.access.WindParticleAccess) particle;
        double x = access.livingwind$getPositionX();
        double y = access.livingwind$getPositionY();
        double z = access.livingwind$getPositionZ();
        // Weather particles may touch liquid, caves or walls. No outdoor wind there.
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return 0.0;
        float exposure = WindShelter.weatherExposureAt(x, y, z);
        if (exposure <= 0.0f) return 0.0;
        float localWind = ClientWindState.strengthAt(x, y, z);
        float speed = baseSpeed(id) * ParticleWindFilter.biomeParticleWeatherMultiplier()
                * ParticleWindFilter.biomeParticleWeatherParticleResponse(id) * exposure;
        return xAxis
            ? WindMath.windX(localWind, ClientWindState.directionDegrees()) * speed
            : WindMath.windZ(localWind, ClientWindState.directionDegrees()) * speed;
    }
}
