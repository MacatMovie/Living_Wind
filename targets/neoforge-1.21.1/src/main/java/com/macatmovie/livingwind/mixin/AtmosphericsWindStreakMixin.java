package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Atmospherics 2.6.5 compatibility. Its wind streaks normally travel
 * along a fixed +X axis and render with a fixed horizontal quaternion. Rotate
 * BOTH the drift and the quad to Living Wind, preserving Atmospherics' speed,
 * animation, spawn rate, and particle lifetime.
 */
@Pseudo
@Mixin(targets = "com.beash.atmospherics.particle.WindStreakParticle", remap = false)
public abstract class AtmosphericsWindStreakMixin {
    @Shadow(remap = false) @Final @Mutable private double driftX;
    @Shadow(remap = false) @Final @Mutable private double driftZ;

    @Inject(method = "tick", at = @At("HEAD"), require = 0, remap = false)
    private void livingwind$steerStreak(CallbackInfo ci) {
        if (!ParticleWindFilter.usesAtmosphericsStreakCompat() || !ClientWindState.hasServerState()) return;
        double speed = Math.hypot(driftX, driftZ);
        if (speed < 1.0e-8) return;
        double x = ClientWindState.altitudeWindX();
        double z = ClientWindState.altitudeWindZ();
        double magnitude = Math.hypot(x, z);
        if (magnitude < 1.0e-8) return;
        driftX = speed * x / magnitude;
        driftZ = speed * z / magnitude;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
        target = "Lcom/beash/atmospherics/particle/WindStreakParticle;renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;Lorg/joml/Quaternionf;F)V",
        remap = false), index = 2, require = 0, remap = false)
    private Quaternionf livingwind$rotateStreakQuad(Quaternionf original) {
        if (!ParticleWindFilter.usesAtmosphericsStreakCompat() || !ClientWindState.hasServerState()) return original;
        double x = driftX;
        double z = driftZ;
        if (x * x + z * z < 1.0e-12) return original;
        // A +X travelling streak is the original orientation. Pre-multiplying
        // rotates the horizontal quad in world space without changing its pitch.
        return new Quaternionf().rotationY((float) Math.atan2(-z, x)).mul(original);
    }
}
