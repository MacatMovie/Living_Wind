package com.macatmovie.livingwind.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Optional 2.0.4 Swinging Lanterns render-state bridge.
 *
 * This is its own wind state, NOT the physics solver's velocity or hit impulses.
 * Swinging Lanterns retains full control of its pendulum constraints, collisions,
 * projectile impacts and Create contraption movement.
 */
@Pseudo
@Mixin(targets = "dev.rilith.swinginglanterns.client.render.SwingingLanternRenderer$ExposureState", remap = false)
public interface SwingingLanternsExposureAccess {
    @Accessor(value = "windExposure", remap = false)
    float livingwind$getWindExposure();

    @Accessor(value = "windExposure", remap = false)
    void livingwind$setWindExposure(float value);

    @Accessor(value = "leanFactor", remap = false)
    float livingwind$getLeanFactor();

    @Accessor(value = "leanFactor", remap = false)
    void livingwind$setLeanFactor(float value);

    @Accessor(value = "weatherBoost", remap = false)
    void livingwind$setWeatherBoost(float value);

    @Accessor(value = "windDirX", remap = false)
    void livingwind$setWindDirX(float value);

    @Accessor(value = "windDirZ", remap = false)
    void livingwind$setWindDirZ(float value);

}
