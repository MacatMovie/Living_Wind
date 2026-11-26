package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.WindblownWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional shader-source safeguard: leave Windblown's underwater current independent. */
@Pseudo
@Mixin(targets = "dev.windblown.client.ShaderPatches", remap = false)
public abstract class WindblownShaderMixin {
    @Inject(method = "code()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void livingwind$aquaticShader(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(WindblownWindCompat.patchAquaticShader(cir.getReturnValue()));
    }
}
