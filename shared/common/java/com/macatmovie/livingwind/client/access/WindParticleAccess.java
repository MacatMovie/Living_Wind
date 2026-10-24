package com.macatmovie.livingwind.client.access;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface WindParticleAccess {
    void livingwind$setParticleType(@Nullable ResourceLocation id);
    @Nullable ResourceLocation livingwind$getParticleType();

    // Particle position fields are protected; provide access through the existing mixin.
    double livingwind$getPositionX();
    double livingwind$getPositionY();
    double livingwind$getPositionZ();

    double livingwind$getVelocityX();
    double livingwind$getVelocityZ();
    void livingwind$setHorizontalVelocity(double x, double z);

    // A few bytes per particle, not a global per-particle map: retain the most recent
    // CONFIRMED sheltered position during brief cache misses (also safe with Async Particles).
    int livingwind$getLastShelteredTick();
    int livingwind$getLastShelteredX();
    int livingwind$getLastShelteredY();
    int livingwind$getLastShelteredZ();
    void livingwind$rememberSheltered(int tick, int x, int y, int z);

    // Reuse the identical exposure for X and Z of a single particle.move() call.
    // Position-aware so a second move after collision gets its own measurement.
    float livingwind$cachedExposure(int tick, double x, double y, double z);
    void livingwind$storeExposure(int tick, double x, double y, double z, float exposure);
}
