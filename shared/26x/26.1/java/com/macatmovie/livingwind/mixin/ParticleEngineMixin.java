package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticularLeafCompat;
import com.macatmovie.livingwind.client.AtmosphericsDustCompat;
import com.macatmovie.livingwind.client.SkyLandsParticleWindCompat;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
        // Filter Atmospherics' desert dust before it is constructed, avoiding
        // stationary clouds in calm weather and reducing the particle workload.
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (com.macatmovie.livingwind.client.ParticleWindFilter.isVanillaLeaf(id)
                && !com.macatmovie.livingwind.client.ParticleWindFilter.shouldSpawnVanillaLeaves()) {
            cir.setReturnValue(null);
            return;
        }
        if (com.macatmovie.livingwind.client.ParticleWindFilter.isFallingLeavesPlusParticle(id)
                && !com.macatmovie.livingwind.client.ParticleWindFilter.shouldSpawnFallingLeavesPlusLeaves()) {
            cir.setReturnValue(null);
            return;
        }
        // Ecliptic Seasons' leaves have their own spawn logic. Suppress them at
        // creation when native leaves are selected, avoiding unnecessary particle
        // allocations and leaving Ecliptic's other seasonal effects untouched.
        if (id != null && "eclipticseasons".equals(id.getNamespace())
                && "fallen_leaves".equals(id.getPath())
                && !com.macatmovie.livingwind.client.ParticleWindFilter.shouldSpawnEclipticLeaves()) {
            cir.setReturnValue(null);
            return;
        }
        if (!AtmosphericsDustCompat.shouldSpawn(id, x, y, z)) {
            cir.setReturnValue(null);
            return;
        }
        if (!ParticularLeafCompat.beginCreate(options, x, y, z, xSpeed, ySpeed, zSpeed)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void livingwind$particularLeafDensity(Particle particle, CallbackInfo ci) {
        // A provider may admit a custom leaf without going through our ID-based create hook.
        if (com.macatmovie.livingwind.client.ParticleWindFilter.isFallingLeavesPlusClass(particle)) {
            if (!com.macatmovie.livingwind.client.ParticleWindFilter.shouldSpawnFallingLeavesPlusLeaves()) {
                ci.cancel();
                return;
            }
            if (particle instanceof WindParticleAccess access && access.livingwind$getParticleType() == null) {
                String type = particle.getClass().getName().endsWith("TerrainFallingLeavesParticle")
                    ? "terrain_leaves" : "falling_leaves";
                access.livingwind$setParticleType(Identifier.fromNamespaceAndPath("fallingleavesplus", type));
            }
        }
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
            Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
            access.livingwind$setParticleType(id);
            // Scorchful also uses dust_grain for a scripted Heat Vision burst;
            // only the larger (scale 10) sandstorm cloud should use ambient wind.
            access.livingwind$setScorchfulStormDust(
                com.macatmovie.livingwind.client.ParticleWindFilter.isScorchfulStormParticleOptions(options, id));
            if (SkyLandsParticleWindCompat.sourceAnchoredAtSpawn(id, x, y, z, xSpeed, ySpeed, zSpeed))
                access.livingwind$setSkylandsSourceAnchored(true);
        }
        ParticularLeafCompat.finishCreate((ParticleEngine) (Object) this, particle);
    }
}
