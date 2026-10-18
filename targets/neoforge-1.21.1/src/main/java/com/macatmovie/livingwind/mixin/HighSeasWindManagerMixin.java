package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.HighSeasWindBridge;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional API boundary: intercept High Seas' central wind sample method.
 * The Vec3 overload delegates to this overload upstream, so it is covered too.
 * No High Seas code, resources or assets are included in Living Wind.
 */
@Pseudo
@Mixin(targets = "com.maxenonyme.highseas.wind.WindManager", remap = false)
public abstract class HighSeasWindManagerMixin {
    @Inject(method = "getWind(Lnet/minecraft/world/level/Level;DDD)Lcom/maxenonyme/highseas/wind/WindSample;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void livingwind$highSeasSample(Level level, double x, double y, double z,
            CallbackInfoReturnable<Object> cir) {
        Object sample = HighSeasWindBridge.override(level, x, y, z);
        if (sample != null) cir.setReturnValue(sample);
    }
}
