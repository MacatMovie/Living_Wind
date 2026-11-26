package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Falling Leaves Plus's CustomFallingLeavesParticle overrides Particle.move to
 * support visual-shape collisions, bypassing Living Wind's generic move hook.
 *
 * Temporarily neutralize *only* its native horizontal wind during tick, restore
 * all original values at RETURN, then add one Living Wind displacement in its
 * overridden move method. Gravity, rotation, visual collisions and decay remain
 * owned by Falling Leaves Plus. No optional classes are linked by this source.
 * Fabric also matches intermediary-named vanilla method overrides in the released jar.
 */
@Pseudo
@Mixin(targets = "fuzs.fallingleavesplus.common.client.particle.CustomFallingLeavesParticle", remap = false)
public abstract class FallingLeavesPlusParticleMixin {
    @Unique private boolean livingwind$replacedWind;
    @Unique private float livingwind$nativeWind;
    @Unique private double livingwind$nativeFlowX;
    @Unique private double livingwind$nativeFlowZ;

    @Unique
    private boolean livingwind$shouldReplaceWind() {
        Particle particle = (Particle) (Object) this;
        Identifier id = ((WindParticleAccess) particle).livingwind$getParticleType();
        return ParticleWindFilter.controlsFallingLeavesPlusWind(id, particle);
    }

    @Inject(method = {"tick()V", "method_3070()V"}, at = @At("HEAD"), require = 0, remap = false)
    private void livingwind$neutralizeNativeWind(CallbackInfo ci) {
        livingwind$replacedWind = livingwind$shouldReplaceWind();
        if (!livingwind$replacedWind) return;
        FallingLeavesPlusWindAccess access = (FallingLeavesPlusWindAccess) this;
        livingwind$nativeWind = access.livingwind$getNativeWind();
        livingwind$nativeFlowX = access.livingwind$getNativeFlowX();
        livingwind$nativeFlowZ = access.livingwind$getNativeFlowZ();
        access.livingwind$setNativeWind(0.0f);
        access.livingwind$setNativeFlowX(0.0);
        access.livingwind$setNativeFlowZ(0.0);
        // Native drift accumulates in velocity with friction=1.0. Clear any
        // pre-existing velocity so changing to Living Wind immediately replaces
        // the old direction instead of permanently carrying both trajectories.
        ((WindParticleAccess) this).livingwind$setHorizontalVelocity(0.0, 0.0);
    }

    @Inject(method = {"tick()V", "method_3070()V"}, at = @At("RETURN"), require = 0, remap = false)
    private void livingwind$restoreNativeWind(CallbackInfo ci) {
        if (!livingwind$replacedWind) return;
        FallingLeavesPlusWindAccess access = (FallingLeavesPlusWindAccess) this;
        access.livingwind$setNativeWind(livingwind$nativeWind);
        access.livingwind$setNativeFlowX(livingwind$nativeFlowX);
        access.livingwind$setNativeFlowZ(livingwind$nativeFlowZ);
        livingwind$replacedWind = false;
    }

    // Hook the actual overridden method. The Particle.move hook explicitly
    // bypasses Falling Leaves Plus so collideWithVisualShapes=false is also safe.
    @ModifyVariable(method = {"move(DDD)V", "method_3069(DDD)V"}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0, remap = false)
    private double livingwind$applyLeafWindX(double movementX) {
        if (!livingwind$shouldReplaceWind() || ((WindParticleAccess) this).livingwind$isOnGround()) return movementX;
        Particle particle = (Particle) (Object) this;
        Identifier id = ((WindParticleAccess) particle).livingwind$getParticleType();
        return movementX + ParticleWindFilter.offsetX(id, particle);
    }

    @ModifyVariable(method = {"move(DDD)V", "method_3069(DDD)V"}, at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0, remap = false)
    private double livingwind$applyLeafWindZ(double movementZ) {
        if (!livingwind$shouldReplaceWind() || ((WindParticleAccess) this).livingwind$isOnGround()) return movementZ;
        Particle particle = (Particle) (Object) this;
        Identifier id = ((WindParticleAccess) particle).livingwind$getParticleType();
        return movementZ + ParticleWindFilter.offsetZ(id, particle);
    }
}
