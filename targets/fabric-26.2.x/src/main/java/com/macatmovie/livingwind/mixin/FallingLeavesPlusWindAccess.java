package com.macatmovie.livingwind.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Optional access to the *native* wind scalars; restored after every tick. */
@Pseudo
@Mixin(targets = "fuzs.fallingleavesplus.common.client.particle.FallingLeavesParticle", remap = false)
public interface FallingLeavesPlusWindAccess {
    @Accessor(value = "windBig", remap = false)
    float livingwind$getNativeWind();

    @Mutable
    @Accessor(value = "windBig", remap = false)
    void livingwind$setNativeWind(float value);

    @Accessor(value = "xaFlowScale", remap = false)
    double livingwind$getNativeFlowX();

    @Accessor(value = "xaFlowScale", remap = false)
    void livingwind$setNativeFlowX(double value);

    @Accessor(value = "zaFlowScale", remap = false)
    double livingwind$getNativeFlowZ();

    @Accessor(value = "zaFlowScale", remap = false)
    void livingwind$setNativeFlowZ(double value);
}
