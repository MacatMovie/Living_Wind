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
}
