package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.DelightOfFlightBalloonWindBridge;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only applied if Delight o' Flight's balloon entity is installed. */
@Pseudo
@Mixin(targets = "com.cloudmeow.delightoflight.entity.PhantomMembraneBalloonEntity", remap = false)
public abstract class DelightOfFlightBalloonMixin {
    // Forge 1.20.1 also accepts the SRG/obfuscated override name, depending on
    // how the other mod was packaged; NeoForge 1.21.1 uses Mojang mappings.
    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$driftWithWind(CallbackInfo ci) {
        DelightOfFlightBalloonWindBridge.afterBalloonTick((Entity) (Object) this,
            WindConfig.DELIGHT_OF_FLIGHT_BALLOON_WIND_ENABLED.get(),
            WindConfig.DELIGHT_OF_FLIGHT_BALLOON_WIND_MULTIPLIER.get());
    }
}
