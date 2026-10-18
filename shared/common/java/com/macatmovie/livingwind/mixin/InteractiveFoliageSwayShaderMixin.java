package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.InteractiveFoliageWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Patches IF's shared sway GLSL only if its 2.2.0 source matches checked patterns. */
@Pseudo
@Mixin(targets = {
    "net.karto.mc2.mc2_interactivefoliage.gpu.LegacyTerrainShader",
    "net.karto.mc2.mc2_interactivefoliage.gpu.IrisFoliageShaders"
}, remap = false)
public abstract class InteractiveFoliageSwayShaderMixin {
    @Inject(method = "readSway()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void livingwind$patchSway(CallbackInfoReturnable<String> ci) {
        ci.setReturnValue(InteractiveFoliageWindCompat.patchSway(ci.getReturnValue()));
    }
}
