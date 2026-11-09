package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.shader.LivingWindShaderBridge;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Iris hook: apply GLSL only to the in-memory IncludeGraph source. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.include.IncludeGraph", remap = false)
public abstract class IrisIncludeGraphShaderMixin {
    @Inject(method = "readFile(Ljava/nio/file/Path;)Ljava/lang/String;", at = @At("RETURN"),
            cancellable = true, require = 0, remap = false)
    private static void livingwind$patchShaderSource(Path path, CallbackInfoReturnable<String> cir) {
        String original = cir.getReturnValue();
        String patched = LivingWindShaderBridge.patchLoadedSource(path, original);
        if (patched != original) cir.setReturnValue(patched);
    }
}
