package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.SuperbWarfareAircraftWindBridge;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No hard dependency on Superb Warfare; only targeted if it is installed. */
@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity", remap = false)
public abstract class SuperbWarfareAircraftWindMixin {
    @Unique private boolean livingwind$checkedAirplane;
    @Unique private boolean livingwind$isAirplane;

    // SBW's Kotlin travel() method name is stable on both supported loaders.
    // Called after engine calculations and before vehicle's movement/collisions.
    @Inject(method = "travel", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$applyAircraftWind(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (!livingwind$checkedAirplane) {
            livingwind$checkedAirplane = true;
            livingwind$isAirplane = SuperbWarfareAircraftWindBridge.isAirplane(self);
        }
        if (!livingwind$isAirplane) return;
        SuperbWarfareAircraftWindBridge.afterTravel(self,
            WindConfig.SUPERB_WARFARE_AIRCRAFT_WIND_ENABLED.get(),
            WindConfig.SUPERB_WARFARE_AIRCRAFT_WIND_MULTIPLIER.get());
    }
}
