package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.config.ClientWindConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AmbiMod 1.1.0's standalone ambient wind volume. Leave all other modes untouched. */
@Pseudo
@Mixin(targets="com.ambimod.handler.ClientTickHandler", remap=false)
public abstract class Ambi26LegacyWindMixin {
    @Inject(method="volumeFor(Lcom/ambimod/handler/AmbientMode;)F", at=@At("RETURN"), cancellable=true, require=0, remap=false)
    private static void livingwind$selectWind(@Coerce Object mode, CallbackInfoReturnable<Float> cir) {
        if (!(mode instanceof Enum<?> e) || !"WIND".equals(e.name())) return;
        ClientWindConfig.AmbiWindMode selected;
        try { selected = ClientWindConfig.AMBI_WIND_MODE.get(); }
        catch (IllegalStateException ignored) { selected = ClientWindConfig.AmbiWindMode.LIVING_WIND; }
        if (selected == ClientWindConfig.AmbiWindMode.LIVING_WIND) cir.setReturnValue(0.0f);
    }
}
