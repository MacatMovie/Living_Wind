package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.GrassierGrassWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replace the shared direction input before the GPU uniforms and CPU particle state are calculated. */
@Pseudo
@Mixin(targets="com.leonardoinc22.shortgrass.client.render.GrassShaderUniforms", remap=false)
public abstract class GrassierGrassShaderWindMixin {
    @ModifyArg(method="write", at=@At(value="INVOKE", target="Ljava/lang/Math;toRadians(D)D"), index=0, require=0, remap=false)
    private static double livingwind$direction(double nativeDegrees) {
        if (!GrassierGrassWindCompat.active()) return nativeDegrees;
        // Grassier Grass uses cos(angle) for X and sin(angle) for Z.
        return Math.toDegrees(Math.atan2(GrassierGrassWindCompat.directionZ(), GrassierGrassWindCompat.directionX()));
    }

    // Detached blades also read these getters, potentially before the first GPU frame.
    @Inject(method="windX()F", at=@At("RETURN"), cancellable=true, require=0, remap=false)
    private static void livingwind$windX(CallbackInfoReturnable<Float> cir) {
        if (GrassierGrassWindCompat.active()) cir.setReturnValue(GrassierGrassWindCompat.directionX());
    }
    @Inject(method="windZ()F", at=@At("RETURN"), cancellable=true, require=0, remap=false)
    private static void livingwind$windZ(CallbackInfoReturnable<Float> cir) {
        if (GrassierGrassWindCompat.active()) cir.setReturnValue(GrassierGrassWindCompat.directionZ());
    }
    @ModifyConstant(method="updateWindAnimation", constant=@Constant(floatValue=0.005f), require=0, remap=false)
    private static float livingwind$waveScrollSpeed(float original) {
        return GrassierGrassWindCompat.noiseScrollCoefficient(original);
    }
}
