package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.wind.EntityWindMotion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server-authoritative mob / projectile push. Players use local client prediction instead. */
@Mixin(Entity.class)
public abstract class EntityWindMovementMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void livingwind$applyEntityWind(CallbackInfo ci) {
        EntityWindMotion.tickServer((Entity) (Object) this);
    }
}
