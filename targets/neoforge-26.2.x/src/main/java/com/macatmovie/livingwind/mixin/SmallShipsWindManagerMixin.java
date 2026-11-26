package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.SmallShipsLevelAccess;
import com.macatmovie.livingwind.compat.SmallShipsWindBridge;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Small Ships 2.0 wind-provider replacement, never modifies ship physics. */
@Pseudo
@Mixin(targets = "com.talhanation.smallships.world.wind.WindManager", remap = false)
public abstract class SmallShipsWindManagerMixin implements SmallShipsLevelAccess {
    @Unique private ServerLevel livingwind$level;

    @Override
    public void livingwind$setServerLevel(ServerLevel level) { this.livingwind$level = level; }

    // A WindManager is SavedData, shared per dimension. Bind its world on retrieval
    // and on ticking so getWind() knows which Living Wind snapshot to query.
    @Inject(method = "get(Lnet/minecraft/server/level/ServerLevel;)Lcom/talhanation/smallships/world/wind/WindManager;",
            at = @At("RETURN"), remap = false, require = 0)
    private static void livingwind$bindWorld(ServerLevel level, CallbackInfoReturnable<Object> cir) {
        Object manager = cir.getReturnValue();
        if (manager instanceof SmallShipsLevelAccess access) access.livingwind$setServerLevel(level);
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"),
            remap = false, require = 0)
    private void livingwind$bindOnTick(ServerLevel level, CallbackInfo ci) {
        livingwind$level = level;
    }

    @Inject(method = "getWind()Lcom/talhanation/smallships/world/wind/Wind;", at = @At("HEAD"),
            cancellable = true, remap = false, require = 0)
    private void livingwind$getWind(CallbackInfoReturnable<Object> cir) {
        Object wind = SmallShipsWindBridge.serverWind(livingwind$level);
        if (wind != null) cir.setReturnValue(wind);
    }
}
