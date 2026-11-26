package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.TreasureBalloonsWindBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional target; no Treasure Balloons dependency or class loads if absent. */
@Pseudo
@Mixin(targets = "net.mcreator.realmrpgballoons.procedures.TreasureBalloonUpdateProcedure", remap = false)
public abstract class TreasureBalloonsUpdateMixin {
    // TAIL is after the procedure's final cardinal / climb / popped velocity
    // assignment. Also executes on the client for smoother prediction.
    @Inject(method = "execute", at = @At("TAIL"), remap = false, require = 0)
    private static void livingwind$afterBalloonFlight(LevelAccessor world, Entity entity, CallbackInfo ci) {
        TreasureBalloonsWindBridge.afterMovement(entity);
    }
}
