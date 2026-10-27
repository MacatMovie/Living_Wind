package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.shader.LivingWindEuphoriaUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Euphoria Patcher 1.10.x calls EuphoriaUniforms.declareAll once for every
 * Iris/Oculus uniform holder. Add our uniforms AFTER Euphoria adds its own.
 *
 * Optional target, no link-time reference to an Euphoria class. Leaving
 * Euphoria uninstalled must not affect base Living Wind or Minecraft startup.
 * @Coerce permits the third-party UniformDeclarer parameter without a hard dep.
 */
@Pseudo
@Mixin(targets = "com.euphoriapatches.euphoria_patcher.integration.uniforms.EuphoriaUniforms", remap = false)
public abstract class EuphoriaUniformsMixin {
    @Inject(method = "declareAll", at = @At("TAIL"), require = 0, remap = false)
    private static void livingwind$registerUniforms(@Coerce Object declarer, CallbackInfo ci) {
        LivingWindEuphoriaUniforms.register(declarer);
    }
}
