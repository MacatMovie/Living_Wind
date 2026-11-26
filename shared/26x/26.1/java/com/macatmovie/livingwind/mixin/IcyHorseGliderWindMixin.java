package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.IcyHorseGliderWindCompat;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lower priority so Icy's Better Horses can update its stabilizer before we sample it. */
@Mixin(value = AbstractHorse.class, priority = 900)
public abstract class IcyHorseGliderWindMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void livingwind$afterHorseTick(CallbackInfo ci) {
        IcyHorseGliderWindCompat.tick((AbstractHorse) (Object) this);
    }
}
