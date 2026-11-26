package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.config.ClientWindConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Optional, config-preserving AmbientSounds integration. The effective wind category
 * volume is zeroed for this tick, but the player's AmbientSounds volume setting is
 * neither rewritten nor saved. Other AmbientSounds categories continue normally.
 */
@Pseudo
@Mixin(targets = "team.creative.ambientsounds.sound.AmbientSoundCategory", remap = false)
public abstract class AmbientSoundsWindCategoryMixin {
    @Shadow(remap = false) public String name;

    @ModifyArg(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lteam/creative/ambientsounds/condition/AmbientSelection;mulSetting(D)V", remap = false),
        index = 0,
        require = 0,
        remap = false
    )
    private double livingwind$muteAmbientSoundsWind(double original) {
        return "wind".equals(name)
            && ClientWindConfig.WIND_AUDIO_MODE.get() == ClientWindConfig.WindAudioMode.LIVING_WIND
            ? 0.0d : original;
    }
}
