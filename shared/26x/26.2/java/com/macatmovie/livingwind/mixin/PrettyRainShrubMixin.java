package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Pretty Rain compatibility. Pretty Rain resets its rolling shrub's X/Z velocity at the
 * end of every tick, so the generic particle hook cannot keep its visible rolling direction aligned
 * on its own. This post-tick hook rotates that velocity to Living Wind's direction while preserving
 * Pretty Rain's exact horizontal speed.
 */
@Pseudo
@Mixin(targets = "com.leclowndu93150.particlerain.particle.ShrubParticle", remap = false)
public abstract class PrettyRainShrubMixin {

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void livingwind$orientRollingShrub(CallbackInfo ci) {
        WindParticleAccess access = (WindParticleAccess) (Object) this;
        if (!ParticleWindFilter.usesPrettyRainDirectionOnlyCompat(access.livingwind$getParticleType())) return;

        ParticleWindFilter.orientPrettyRainDirectionOnly(access);
    }
}
