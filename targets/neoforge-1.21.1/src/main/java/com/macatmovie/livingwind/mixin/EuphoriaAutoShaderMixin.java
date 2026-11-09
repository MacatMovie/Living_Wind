package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.shader.LivingWindShaderBridge;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Euphoria 1.10.5's constructor finishes detecting/creating shader packs
 * before the renderer loads them. Restore only byte-verified old backups;
 * new GLSL changes are applied exclusively in memory by Iris/Oculus.
 */
@Pseudo
@Mixin(targets = "com.euphoriapatches.euphoria_patcher.EuphoriaPatcher", remap = false)
public abstract class EuphoriaAutoShaderMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0, remap = false)
    private void livingwind$afterEuphoriaStartup(CallbackInfo ci) {
        livingwind$recover();
    }

    @Inject(method = "completeShaderPatching", at = @At("RETURN"), require = 0, remap = false)
    private void livingwind$afterEuphoriaRegeneration(@Coerce Object info, Path temporaryDirectory,
        CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) livingwind$recover();
    }

    private static void livingwind$recover() {
        try {
            // Euphoria's own shaderpack directory is authoritative (launchers
            // can use custom game paths). Reflect so the dependency stays optional.
            Class<?> ep = Class.forName("com.euphoriapatches.euphoria_patcher.EuphoriaPatcher");
            Object location = ep.getField("shaderpacks").get(null);
            if (location instanceof Path path) LivingWindShaderBridge.recoverPreviouslyPatchedPacks(path);
        } catch (ReflectiveOperationException | LinkageError ex) {
            System.err.println("[Living Wind] Optional Euphoria shader recovery unavailable: " + ex);
        }
    }
}
