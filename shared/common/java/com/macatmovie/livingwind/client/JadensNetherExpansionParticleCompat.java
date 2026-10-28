package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Jaden's Nether Expansion uses fixed horizontal movement for Black Snow (Forge 1.20.1)
 * and Lodestone black flakes/cold fog (NeoForge 1.21.1). Replace, never add to, the
 * hardcoded movement at Particle.move(), where both variants ultimately pass through.
 * No JNE or Lodestone classes are linked, so the integration stays fully optional.
 */
public final class JadensNetherExpansionParticleCompat {
    private JadensNetherExpansionParticleCompat() {}

    public static boolean isJneAtmosphericParticle(@Nullable ResourceLocation id) {
        if (id == null || !"netherexp".equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "black_flake", "black_flake_world", "cold_fog" -> true;
            default -> false;
        };
    }

    /** Return NaN when no override is appropriate; generic wind then follows its old path. */
    public static double replacementX(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, true);
    }

    public static double replacementZ(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, false);
    }

    private static double replacement(@Nullable ResourceLocation id, Particle particle, boolean xAxis) {
        if (!isJneAtmosphericParticle(id) || !ClientWindState.hasServerState()
                || ClientWindState.dimensionWindDisabled()
                || !ParticleWindFilter.allowsJneSteering(id, particle)
                || !(particle instanceof WindParticleAccess access)) return Double.NaN;

        double px = access.livingwind$getPositionX();
        double py = access.livingwind$getPositionY();
        double pz = access.livingwind$getPositionZ();
        if (ParticleWaterProtection.isSubmerged(px, py, pz)) return 0.0;

        float strength = ClientWindState.strengthAt(px, py, pz);
        float exposure = WindShelter.weatherExposureAt(px, py, pz);
        // Keep the glacier's heavy drifting atmosphere without inheriting the 1.20.1
        // particle's unbounded cumulative fixed acceleration (up to many blocks/tick).
        // Lodestone flakes/fog have different original travel speeds; keep that ratio.
        float baseSpeed = "cold_fog".equals(id.getPath()) ? 0.62f
            : "black_flake_world".equals(id.getPath()) ? 0.46f : 0.42f;
        float speed = baseSpeed * Math.min(2.25f, Math.max(0.0f, strength / 1.35f)) * exposure;
        return xAxis ? WindMath.windX(speed, ClientWindState.directionDegrees())
            : WindMath.windZ(speed, ClientWindState.directionDegrees());
    }
}
