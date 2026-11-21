package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.GrassierGrassWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grassier Grass 1.4.5 shares GrassShaderUniforms among its shaders, compute
 * animation and detached grass particles on all four Minecraft/loader targets.
 * Its Minecraft-typed updateFrameState argument differs in the Fabric JARs,
 * so target the stable method name and static self-invocation, not that descriptor.
 *
 * Keep its noise texture, shader maths, sky-light shelter, and entity-made
 * trails; only replace the inputs that the native animation already consumes.
 */
@Pseudo
@Mixin(targets = "com.leonardoinc22.shortgrass.client.render.GrassShaderUniforms", remap = false)
public abstract class GrassierGrassShaderWindMixin {
    @Shadow(remap = false) private static float currentWindX;
    @Shadow(remap = false) private static float currentWindZ;
    @Shadow(remap = false) private static float currentWindSpeed;

    // Use independent @ModifyArg hooks, not @ModifyArgs: Forge 1.20.1 can
    // fail loading synthetic Args$ subclasses generated for foreign mod targets.
    @ModifyArg(method = "updateFrameState", at = @At(value = "INVOKE",
            target = "Lcom/leonardoinc22/shortgrass/client/render/GrassShaderUniforms;updateWindAnimation(FFFF)V",
            remap = false), index = 1, require = 0, remap = false)
    private static float livingwind$feedGrassWindX(float nativeDirectionX) {
        return GrassierGrassWindCompat.active() ? GrassierGrassWindCompat.directionX() : nativeDirectionX;
    }

    @ModifyArg(method = "updateFrameState", at = @At(value = "INVOKE",
            target = "Lcom/leonardoinc22/shortgrass/client/render/GrassShaderUniforms;updateWindAnimation(FFFF)V",
            remap = false), index = 2, require = 0, remap = false)
    private static float livingwind$feedGrassWindZ(float nativeDirectionZ) {
        return GrassierGrassWindCompat.active() ? GrassierGrassWindCompat.directionZ() : nativeDirectionZ;
    }

    @ModifyArg(method = "updateFrameState", at = @At(value = "INVOKE",
            target = "Lcom/leonardoinc22/shortgrass/client/render/GrassShaderUniforms;updateWindAnimation(FFFF)V",
            remap = false), index = 3, require = 0, remap = false)
    private static float livingwind$feedGrassStrength(float nativeStrength) {
        return GrassierGrassWindCompat.active() ? GrassierGrassWindCompat.bladeWindStrength() : nativeStrength;
    }

    // The target copies its own (original) locals into the shared fields after
    // invoking updateWindAnimation. Override that last copy as well, or the
    // shader and flying grass particles would disagree with the noise scroll.
    @Inject(method = "updateFrameState", at = @At("RETURN"), require = 0, remap = false)
    private static void livingwind$shareGrassWind(CallbackInfo ci) {
        if (!GrassierGrassWindCompat.active()) return;
        currentWindX = GrassierGrassWindCompat.directionX();
        currentWindZ = GrassierGrassWindCompat.directionZ();
        currentWindSpeed = GrassierGrassWindCompat.bladeWindStrength();
    }

    // 0.005f is Grassier Grass 1.4.5's dedicated noise-scroll coefficient.
    // Multiply this instead of the strength parameter to avoid unintentionally
    // altering blade bending, flutter/plant bobbing or detached blade density.
    @ModifyConstant(method = "updateWindAnimation", constant = @Constant(floatValue = 0.005f),
            require = 0, remap = false)
    private static float livingwind$waveScrollSpeed(float original) {
        return GrassierGrassWindCompat.noiseScrollCoefficient(original);
    }
}
