package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.AlekiShipsWindBridge;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Nifty Ships wind provider wrapper, without modifying their registry. */
@Pseudo
@Mixin(targets = "com.alekiponi.alekiships.wind.WindModels", remap = false)
public abstract class AlekiShipsWindModelsMixin {
    @Inject(method = "get(Lnet/minecraft/world/level/Level;)Lcom/alekiponi/alekiships/wind/WindModel;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void livingwind$wrapWindModel(Level level, CallbackInfoReturnable<Object> cir) {
        Object original = cir.getReturnValue();
        Object wrapped = AlekiShipsWindBridge.wrap(level, original);
        if (wrapped != original) cir.setReturnValue(wrapped);
    }
}
