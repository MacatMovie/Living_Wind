package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamic Surroundings' SceneWind is the single horizontal wind source for BOTH
 * its custom particles (SmokePuff, SteamPuff, FlameSpark, waterfall mist, etc.)
 * and the vanilla/modded particles marked wind_blown by its Particle mixin.
 *
 * Living Wind already applies a single shelter-aware movement offset through
 * Particle.move. Suppress ONLY that second SceneWind movement source when the
 * Living Wind generic particle system is enabled and has a server wind state.
 * Do not change Dynamic Surroundings' WindService: its biome audio, weather,
 * rain/fog systems and independent particle animations must keep functioning.
 *
 * No Dynamic Surroundings classes are linked. The soft target is optional on
 * both Forge 1.20.1 and NeoForge 1.21.1, including DS versions without SceneWind.
 */
@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.runtime.weather.SceneWind", remap = false)
public abstract class DynamicSurroundingsParticleWindMixin {
    @Inject(method = "x(DD)F", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void livingwind$suppressDSParticleWindX(double x, double z,
                                                              CallbackInfoReturnable<Float> cir) {
        if (ParticleWindFilter.controlsDynamicSurroundingsParticleWind()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "z(DD)F", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void livingwind$suppressDSParticleWindZ(double x, double z,
                                                              CallbackInfoReturnable<Float> cir) {
        if (ParticleWindFilter.controlsDynamicSurroundingsParticleWind()) {
            cir.setReturnValue(0.0F);
        }
    }
}
