package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.SmallShipsWindBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep Small Ships' client wind indicators and effects aligned with its server physics. */
@Pseudo
@Mixin(targets = "com.talhanation.smallships.client.wind.ClientWindManager", remap = false)
public abstract class SmallShipsClientWindManagerMixin {
    @Inject(method = "getWind()Lcom/talhanation/smallships/world/wind/Wind;", at = @At("HEAD"),
            cancellable = true, remap = false, require = 0)
    private static void livingwind$getWind(CallbackInfoReturnable<Object> cir) {
        Object wind = SmallShipsWindBridge.clientWind();
        if (wind != null) cir.setReturnValue(wind);
    }

    @Inject(method = "getDirection(F)F", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void livingwind$getDirection(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (SmallShipsWindBridge.clientActive()) cir.setReturnValue(SmallShipsWindBridge.clientDirection());
    }

    @Inject(method = "getStrength(F)F", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void livingwind$getStrength(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (SmallShipsWindBridge.clientActive()) cir.setReturnValue(SmallShipsWindBridge.clientStrength());
    }
}
