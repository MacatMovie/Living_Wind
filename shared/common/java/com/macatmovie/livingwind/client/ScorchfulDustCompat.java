package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Scorchful sandstorm dust steering (Fabric 1.20.1/1.21.1,
 * NeoForge 1.21.1). Scorchful creates dust with a fixed horizontal speed
 * along its X axis. Replace the horizontal movement, rather than adding
 * another wind acceleration, and leave its gravity, fog, storms, spawning,
 * scale, color and collision to Scorchful.
 *
 * No Scorchful classes are linked: the registered particle ID is sufficient.
 */
public final class ScorchfulDustCompat {
    private ScorchfulDustCompat() {}

    public static boolean isSandstormDust(@Nullable ResourceLocation id) {
        return id != null && "scorchful".equals(id.getNamespace())
                && "dust_grain".equals(id.getPath());
    }

    /** NaN = retain Scorchful's own movement. */
    public static double replacementX(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, true);
    }

    /** NaN = retain Scorchful's own movement. */
    public static double replacementZ(@Nullable ResourceLocation id, Particle particle) {
        return replacement(id, particle, false);
    }

    private static double replacement(@Nullable ResourceLocation id, Particle particle, boolean xAxis) {
        if (!isSandstormDust(id)
                || !(particle instanceof WindParticleAccess access)
                || !access.livingwind$isScorchfulStormDust()
                || !ParticleWindFilter.allowsScorchfulDustSteering(id, particle)) return Double.NaN;

        double x = access.livingwind$getPositionX();
        double y = access.livingwind$getPositionY();
        double z = access.livingwind$getPositionZ();
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return 0.0;

        // Scorchful configures native dust speed (usually -1.0 on X). Preserve
        // this magnitude, including the player's Scorchful settings and its
        // normal per-tick drag, while substituting Living Wind's heading.
        double nativeSpeed = Math.hypot(access.livingwind$getVelocityX(), access.livingwind$getVelocityZ());
        if (!Double.isFinite(nativeSpeed)) return Double.NaN;
        double strength = ClientWindState.strengthAt(x, y, z);
        double exposure = WindShelter.weatherExposureAt(x, y, z);
        double speed = nativeSpeed * Math.min(2.5, Math.max(0.0, strength / 0.8))
                * exposure * ParticleWindFilter.scorchfulDustMultiplier()
                * ParticleWindFilter.biomeParticleWeatherParticleResponse(id);
        float heading = ClientWindState.directionDegrees();
        return xAxis ? WindMath.windX((float) speed, heading) : WindMath.windZ((float) speed, heading);
    }
}
