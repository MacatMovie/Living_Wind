package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Particle Rain 4.x defaults snow to 0.01 wind response (rain is 0.4).
 * That almost entirely hides Living Wind even though its getWind hook works.
 * Raise only the snow multiplier in its existing wind manager: other effects,
 * collision physics and registered wind particles stay in Particle Rain's hands.
 */
@Pseudo
@Mixin(targets = "pigcart.particlerain.WindManager", remap = false)
public abstract class OfficialParticleRainSnowWindMixin {
    @Inject(method = "windMultiplier", at = @At("RETURN"), cancellable = true,
            require = 0, remap = false)
    private static void livingwind$boostSnowWind(ClientLevel level, @Coerce Object settings,
                                                   CallbackInfoReturnable<Float> cir) {
        if (!ParticleWindFilter.drivesOfficialParticleRain()
                || !(settings instanceof OfficialParticleRainDataAccess data)
                || !"snow".equals(data.livingwind$getWeatherParticleId())) return;
        float original = cir.getReturnValue();
        // Keep zero as an intentional Particle Rain opt-out. The user's global
        // Living Wind Particle Rain wind multiplier still scales the final wind.
        if (original <= 0.0f) return;
        // 0.01 -> 0.25 in normal snow, 0.1 -> 0.45 in thunderstorms.
        // Preserve settings already at least this strong; never add extra wind.
        float minimum = level != null && level.isThundering() ? 0.45f : 0.25f;
        cir.setReturnValue(Math.max(original, minimum));
    }
}
