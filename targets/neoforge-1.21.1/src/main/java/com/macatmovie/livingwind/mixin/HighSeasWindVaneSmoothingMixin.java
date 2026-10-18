package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ClientWindState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The High Seas vane already interpolates angles safely through 360 degrees,
 * but its stock response rate is considerably faster than Living Wind's vane.
 * Retain the original interpolator and simply soften its per-frame response
 * when Living Wind supplies High Seas weather. No High Seas class is bundled.
 */
@Pseudo
@Mixin(targets = "com.maxenonyme.highseas.client.WindVaneAngle", remap = false)
public abstract class HighSeasWindVaneSmoothingMixin {
    @ModifyArg(
            method = "update(Lcom/maxenonyme/highseas/block/entity/WindVaneBlockEntity;)F",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;exp(D)D", remap = false),
            index = 0, require = 0, remap = false)
    private static double livingwind$softenHighSeasVane(double exponent) {
        // Stock High Seas: exp(-14 * dt). With compatibility enabled, use an
        // effective response about 4.2/s to give a noticeably smoother turn.
        // When integration is disabled, keep exact original High Seas motion.
        return ClientWindState.highSeasWindEnabled() ? exponent * 0.30d : exponent;
    }
}
