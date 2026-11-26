package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.GrassierGrassWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The native effective wind speed is shared by GPU grass and detached blade spawning. */
@Pseudo
@Mixin(targets="com.leonardoinc22.shortgrass.config.GrassConfig", remap=false)
public abstract class GrassierGrassDetachedBladeMixin {
    @Inject(method="effectiveWindSpeed()F", at=@At("RETURN"), cancellable=true, require=0, remap=false)
    private static void livingwind$effectiveWind(CallbackInfoReturnable<Float> cir) {
        if (GrassierGrassWindCompat.active()) cir.setReturnValue(100.0f * GrassierGrassWindCompat.bladeWindStrength());
    }
}
