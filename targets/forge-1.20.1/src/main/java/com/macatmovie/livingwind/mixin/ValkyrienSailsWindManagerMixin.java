package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.ValkyrienSailsWindCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only Valkyrien Sails wind INPUTS, never its sailing or flag code. */
@Pseudo
@Mixin(targets = "com.quintonc.vs_sails.wind.WindManager", remap = false)
public abstract class ValkyrienSailsWindManagerMixin {
    @Inject(method = "getWindDirection(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;)F",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void livingwind$direction(Level level, Vec3 pos, CallbackInfoReturnable<Float> cir) {
        Float result = ValkyrienSailsWindCompat.direction(level, pos);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "getWindStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void livingwind$strength(Level level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Float result = ValkyrienSailsWindCompat.strength(level, pos);
        if (result != null) cir.setReturnValue(result);
    }
}
