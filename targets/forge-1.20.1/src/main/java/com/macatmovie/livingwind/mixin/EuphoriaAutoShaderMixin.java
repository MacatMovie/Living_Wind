package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.shader.LivingWindShaderAutoPatcher;
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
 * before the Minecraft renderer loads them. Patch installed shader archives
 * in that safe startup window, and after EP regenerates a pack later on.
 * No replacement ZIP, resource pack or extra mod is required.
 */
@Pseudo
@Mixin(targets = "com.euphoriapatches.euphoria_patcher.EuphoriaPatcher", remap = false)
public abstract class EuphoriaAutoShaderMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0, remap = false)
    private void livingwind$afterEuphoriaStartup(CallbackInfo ci) {
        livingwind$patch();
    }

    @Inject(method = "completeShaderPatching", at = @At("RETURN"), require = 0, remap = false)
    private void livingwind$afterEuphoriaRegeneration(@Coerce Object info, Path temporaryDirectory,
        CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) livingwind$patch();
    }

    private static void livingwind$patch() {
        try {
            // Euphoria's own shaderpack directory is authoritative (launchers
            // can use custom game paths). Reflect so the dependency stays optional.
            Class<?> ep = Class.forName("com.euphoriapatches.euphoria_patcher.EuphoriaPatcher");
            Object location = ep.getField("shaderpacks").get(null);
            if (location instanceof Path path) LivingWindShaderAutoPatcher.patchInstalledPacks(path);
        } catch (ReflectiveOperationException | LinkageError ex) {
            System.err.println("[Living Wind] Optional automatic Euphoria shader patch unavailable: " + ex);
        }
    }
}
