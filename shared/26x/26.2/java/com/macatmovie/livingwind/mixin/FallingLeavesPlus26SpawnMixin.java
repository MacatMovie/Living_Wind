package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The actual canopy emitter shared by all six Falling Leaves Plus 26.x JARs. */
@Pseudo
@Mixin(targets="fuzs.fallingleavesplus.common.client.particle.settings.FallingLeavesManager", remap=false)
public abstract class FallingLeavesPlus26SpawnMixin {
    @Inject(method="spawnFallingLeavesParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at=@At("HEAD"), cancellable=true, require=0, remap=false)
    private static void livingwind$selectLeafSource(CallbackInfo ci) {
        if (!ParticleWindFilter.shouldSpawnFallingLeavesPlusLeaves()) ci.cancel();
    }
}
