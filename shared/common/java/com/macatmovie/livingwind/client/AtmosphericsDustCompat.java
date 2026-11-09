package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.wind.WindMath;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Atmospherics 2.6.5 (NeoForge 1.21.1) already gives all seven desert dust
 * layers their own per-tick velocity from DryWindField. Adding the generic
 * Living Wind offset creates two conflicting wind directions. Replace their
 * movement instead, preserving Atmospherics' individual speeds, turbulence,
 * textures, spawn frequency, and storm settings. Its Forge 1.20.1 build does
 * not register these dust types; no external classes are linked here.
 */
public final class AtmosphericsDustCompat {
    private AtmosphericsDustCompat() {}

    public static boolean isDustParticle(@Nullable ResourceLocation id) {
        if (id == null || !"atmospherics".equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "desert_dust", "desert_dust_ground", "desert_sandstorm",
                 "desert_sandstorm_ground", "desert_sandstorm_feet",
                 "desert_sandstorm_haze", "desert_sandstorm_surface" -> true;
            default -> false;
        };
    }

    /**
     * Atmospherics spawns its desert dust even in perfectly calm conditions.
     * Thin out ONLY its seven airborne dust layers, not unrelated weather/visuals.
     * At 0.5 Living Wind strength keep ~10% of native density, rising smoothly
     * to the original Atmospherics density by 1.0 and remaining full above it.
     * This runs at creation, so rejected dust never consumes a render/tick slot.
     */
    public static boolean shouldSpawn(@Nullable ResourceLocation id, double x, double y, double z) {
        if (!isDustParticle(id) || !ParticleWindFilter.allowsAtmosphericsDustSteering(id, null))
            return true;
        if (!ClientWindState.hasServerState() || ClientWindState.dimensionWindDisabled())
            return false;
        float strength = ClientWindState.strengthAt(x, y, z);
        if (strength < 0.5f) return false;
        if (strength >= 1.0f) return true;
        float density = 0.10f + (strength - 0.5f) * 1.80f;
        return ThreadLocalRandom.current().nextFloat() < density;
    }

    /** NaN means use the particle's original motion and the normal filter path. */
    public static double replacementX(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, true);
    }

    public static double replacementZ(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, false);
    }

    private static double replacement(@Nullable ResourceLocation id, Particle particle, boolean xAxis) {
        if (!ClientWindState.hasServerState() || ClientWindState.dimensionWindDisabled()
                || !ParticleWindFilter.allowsAtmosphericsDustSteering(id, particle)
                || !(particle instanceof WindParticleAccess access)) return Double.NaN;

        double x = access.livingwind$getPositionX();
        double y = access.livingwind$getPositionY();
        double z = access.livingwind$getPositionZ();
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return 0.0;

        // DesertDustParticle updates its velocity *immediately* before move().
        // Use that exact layer's motion magnitude so ground dust, storm clouds,
        // and haze retain their different native speeds and slight fluctuations.
        double nativeSpeed = Math.hypot(access.livingwind$getVelocityX(), access.livingwind$getVelocityZ());
        if (!Double.isFinite(nativeSpeed)) return Double.NaN;

        float strength = ClientWindState.strengthAt(x, y, z);
        float exposure = WindShelter.weatherExposureAt(x, y, z);
        // Around 0.8 Living Wind strength preserves Atmospherics' own pace;
        // calm weather eases the dust down and gusts produce visibly faster drift.
        double speed = nativeSpeed * Math.min(2.5f, Math.max(0.0f, strength / 0.8f)) * exposure;
        float direction = ClientWindState.directionDegrees();
        return xAxis ? WindMath.windX((float) speed, direction) : WindMath.windZ((float) speed, direction);
    }
}
