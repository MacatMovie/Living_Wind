package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cancel only Falling Leaves Plus's three visual particle types at creation. */
@Mixin(ParticleEngine.class)
public abstract class FallingLeavesPlusSpawnMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void livingwind$selectLeafSource(ParticleOptions options, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
        if (ParticleWindFilter.shouldSpawnFallingLeavesPlusLeaves()) return;
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (ParticleWindFilter.isFallingLeavesPlusParticle(id)) cir.setReturnValue(null);
    }
}
