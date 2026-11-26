package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.shader.LivingWindEuphoriaUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Register shader uniforms when Iris/Oculus is present without Euphoria.
 * Optional targets; no hard Iris/Oculus references. */
@Pseudo
@Mixin(targets = "net.coderbot.iris.uniforms.CommonUniforms", remap = false)
public abstract class OculusCommonUniformsMixin {
    @Inject(method = "generalCommonUniforms", at = @At("TAIL"), remap = false, require = 0)
    private static void livingwind$uniforms(@Coerce Object uniforms, @Coerce Object notifier,
        @Coerce Object directives, CallbackInfo ci) {
        LivingWindEuphoriaUniforms.registerIris(uniforms);
    }
}
