package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.AlekiShipsTelltaleMotion;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional, visual-only sloop flag smoothing; no dependency on Nifty Ships. */
@Pseudo
@Mixin(targets = "com.alekiponi.alekiships.client.model.entity.SloopEntityModel", remap = false)
public abstract class AlekiShipsTelltaleMixin {
    @Inject(method = "animateTelltail(Lcom/alekiponi/alekiships/common/entity/vehicle/SloopEntity;FLnet/minecraft/client/model/geom/ModelPart;[Lnet/minecraft/client/model/geom/ModelPart;I)V",
            at = @At("TAIL"), remap = false, require = 0)
    private static void livingwind$smoothFlag(@Coerce Object boat, float partialTicks,
                                               ModelPart flag, ModelPart[] sections, int animationTick,
                                               CallbackInfo ci) {
        AlekiShipsTelltaleMotion.smooth(boat, partialTicks, flag, sections);
    }
}
