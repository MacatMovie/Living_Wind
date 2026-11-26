package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional bridge for the original Particle Rain v4.x (PigCart), not the older Pretty Rain fork.
 *
 * In v4, registered particle IDs no longer identify most weather effects: its
 * pigcart.particlerain.particle.CustomParticle instances use WindManager, which
 * calls ParticleRain.getWind at spawn time and during particle movement.
 * Replace ONLY the wind vector, preserving Particle Rain's per-particle strengths,
 * weather rendering, spawning, collision, animations, and particle motion system.
 *
 * Soft target/no direct reference: loading Living Wind without Particle Rain is safe.
 */
@Pseudo
@Mixin(targets = "pigcart.particlerain.ParticleRain", remap = false)
public abstract class OfficialParticleRainWindMixin {
    @Inject(method = "getWind(DDD)Lorg/joml/Vector3f;", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void livingwind$syncOfficialWeatherWind(double x, double y, double z,
                                                            CallbackInfoReturnable<Vector3f> cir) {
        if (!ParticleWindFilter.drivesOfficialParticleRain()) return;
        cir.setReturnValue(ParticleWindFilter.officialParticleRainWind(x, y, z));
    }
}
