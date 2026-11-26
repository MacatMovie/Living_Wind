package com.macatmovie.livingwind.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Optional accessor for Particle Rain 4.x weather particle settings. */
@Pseudo
@Mixin(targets = "pigcart.particlerain.config.ParticleData", remap = false)
public interface OfficialParticleRainDataAccess {
    @Accessor(value = "id", remap = false)
    String livingwind$getWeatherParticleId();
}
