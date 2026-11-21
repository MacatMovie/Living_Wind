package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.GrassierGrassWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Grassier Grass creates GrassBladeParticle directly (no particle registry ID).
 * Its tick uses effectiveWindSpeed for the detached-blade spawn threshold,
 * count and initial glide speed; replace that single native input, not the
 * particle's movement, to avoid layering a second wind force.
 */
@Pseudo
@Mixin(targets = "com.leonardoinc22.shortgrass.client.render.GrassWindParticles", remap = false)
public abstract class GrassierGrassDetachedBladeMixin {
    // v1.4.5 tick(): the FIRST float local stored is effectiveWindSpeed.
    @ModifyVariable(method = "tick", at = @At(value = "STORE", ordinal = 0),
            ordinal = 0, require = 0, remap = false)
    private static float livingwind$detachedBladeWind(float nativeSpeed) {
        return GrassierGrassWindCompat.detachedBladeWindSpeed(nativeSpeed);
    }
}
