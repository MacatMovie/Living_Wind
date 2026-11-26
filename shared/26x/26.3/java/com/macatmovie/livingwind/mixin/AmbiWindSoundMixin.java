package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.config.ClientWindConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Optional compatibility for AmbiMod 1.2.0 on Forge 1.20.1 / NeoForge 1.21.1.
 *
 * AmbiCoreIntegration.publish(path, volume) is the common output point for
 * all Ambi rules. Suppress ONLY the wind rule here, *before* AmbiCore receives
 * its volume and enabled state. This fades an already-playing wind loop out as
 * well as preventing new playback. We never modify Ambi's saved settings or
 * its independently controlled rain, desert storm, ocean, wildlife, etc.
 *
 * The class is optional; no Ambi classes are linked to Living Wind at runtime
 * when AmbiMod is absent. The mixin is inert for every rule other than wind.
 */
@Pseudo
@Mixin(targets = "com.ambimod.AmbiCoreIntegration", remap = false)
public abstract class AmbiWindSoundMixin {
    @ModifyVariable(
        method = "publish(Ljava/lang/String;F)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1,
        require = 0,
        remap = false
    )
    private static float livingwind$selectAmbiWindSource(float original, String path, float volume) {
        // Ambi calls publish() while it is constructing the mod, before NeoForge
        // has loaded Living Wind's CLIENT config. ConfigValue.get() throws at
        // that point, so keep the configured default until loading finishes.
        if (!"wind".equals(path)) return original;

        ClientWindConfig.AmbiWindMode mode;
        try {
            mode = ClientWindConfig.AMBI_WIND_MODE.get();
        } catch (IllegalStateException configNotLoadedYet) {
            mode = ClientWindConfig.AmbiWindMode.LIVING_WIND;
        }
        return mode == ClientWindConfig.AmbiWindMode.LIVING_WIND ? 0.0f : original;
    }
}
