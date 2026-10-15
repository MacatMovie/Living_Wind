package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.SwingingLanternsWindCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional Swinging Lanterns 2.0.4 integration. Runs after its cached wind exposure
 * update but before rendering/XPBD movement consumes the result. No physics overrides.
 */
@Pseudo
@Mixin(targets = "dev.rilith.swinginglanterns.client.render.SwingingLanternRenderer", remap = false)
public abstract class SwingingLanternsWindMixin {
    @Inject(method = "updateWindExposure(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;JDFFF)Ldev/rilith/swinginglanterns/client/render/SwingingLanternRenderer$ExposureState;",
            at = @At("RETURN"), require = 0, remap = false)
    private static void livingwind$replaceLanternWind(Level level, BlockPos pos,
            long gameTime, double renderTime, float smoothing, float rain, float thunder,
            CallbackInfoReturnable<Object> cir) {
        SwingingLanternsWindCompat.apply(level, pos, cir.getReturnValue());
    }
}
