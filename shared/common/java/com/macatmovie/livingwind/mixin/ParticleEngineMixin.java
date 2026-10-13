package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticularLeafCompat;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void livingwind$beginParticleCreate(
        ParticleOptions options,
        double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed,
        CallbackInfoReturnable<Particle> cir
    ) {
        if (!ParticularLeafCompat.beginCreate(options, x, y, z, xSpeed, ySpeed, zSpeed)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void livingwind$particularLeafDensity(Particle particle, CallbackInfo ci) {
        if (!ParticularLeafCompat.allowCurrentAdd()) ci.cancel();
    }

    @Inject(method = "createParticle", at = @At("RETURN"))
    private void livingwind$captureParticleTypeAndFinishDensity(
        ParticleOptions options,
        double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed,
        CallbackInfoReturnable<Particle> cir
    ) {
        Particle particle = cir.getReturnValue();
        if (particle instanceof WindParticleAccess access) {
            ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
            access.livingwind$setParticleType(id);
        }
        ParticularLeafCompat.finishCreate((ParticleEngine) (Object) this, particle);
    }
}
