package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.AdvancedChimneysSmokeWind;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Only present on Advanced Chimneys' client smoke particles. It owns chimney
 * routing, collisions and vertical movement; we replace horizontal movement
 * only AFTER the particle has left a chimney. No global particle-tick hook.
 */
@Pseudo
@Mixin(targets = "com.endertech.minecraft.mods.adchimneys.particles.AbstractSmokeParticle$Instance", remap = false)
public abstract class AdvancedChimneysSmokeMixin {
    @Shadow(remap = false) protected boolean insideChimney;
    @Unique private boolean livingwind$hasSmoothedWind;
    @Unique private double livingwind$smoothedWindX;
    @Unique private double livingwind$smoothedWindZ;
    @Unique private float livingwind$lastKnownExposure = -1.0f;

    /**
     * IMPORTANT: modify ACTUAL movement arguments, not just particle.xd/zd.
     * By the time an @Inject at INVOKE runs, the original velocities are already
     * on the operand stack. Updating fields alone cannot redirect that movement.
     *
     * Both supplied Advanced Chimneys JARs invoke precisely one move(DDD) at the
     * end of their tick, with platform-specific names.
     */
    // Forge 1.20.1: @ModifyArgs generates Args$1 on the target classloader,
    // which can throw NoClassDefFoundError when Advanced Chimneys spawns smoke.
    // @ModifyArg edits each actual move(DDD) operand without synthetic Args.
    @ModifyArg(method = "m_5989_", at = @At(value = "INVOKE",
        target = "Lcom/endertech/minecraft/mods/adchimneys/particles/AbstractSmokeParticle$Instance;m_6257_(DDD)V",
        remap = false), index = 0, require = 0, remap = false)
    private double livingwind$redirectOutdoorSmokeX(double originalX) {
        // Advanced Chimneys owns all motion while smoke is inside a vent/pipe.
        if (insideChimney || !AdvancedChimneysSmokeWind.active()) {
            livingwind$hasSmoothedWind = false;
            livingwind$lastKnownExposure = -1.0f;
            return originalX;
        }

        WindParticleAccess particle = (WindParticleAccess) this;
        float exposure = AdvancedChimneysSmokeWind.exposure(particle);
        if (exposure >= 0.0f) {
            livingwind$lastKnownExposure = exposure;
        } else {
            // Worker-thread cache misses must not read world blocks. Keep the
            // last confirmed local exposure; for freshly spawned smoke, use the
            // already-published player exposure until its own cell is surveyed.
            exposure = livingwind$lastKnownExposure >= 0.0f
                ? livingwind$lastKnownExposure : AdvancedChimneysSmokeWind.initialExposure();
        }

        double desiredX = AdvancedChimneysSmokeWind.targetX(exposure);
        double desiredZ = AdvancedChimneysSmokeWind.targetZ(exposure);
        if (!livingwind$hasSmoothedWind) {
            // Begin with vertical rise and bend into the prevailing wind. Do not
            // seed from Advanced Chimneys' wind (it may point the opposite way).
            livingwind$smoothedWindX = 0.0;
            livingwind$smoothedWindZ = 0.0;
            livingwind$hasSmoothedWind = true;
        }
        livingwind$smoothedWindX += (desiredX - livingwind$smoothedWindX) * 0.35;
        livingwind$smoothedWindZ += (desiredZ - livingwind$smoothedWindZ) * 0.35;

        // Replace the actual move(DDD) inputs; writing only the fields here is
        // too late because vanilla/JVM already loaded the old args on the stack.
        // Keep the particle's velocity consistent for any subsequent physics
        // and collision logic; never touch its vertical component.
        particle.livingwind$setHorizontalVelocity(livingwind$smoothedWindX, livingwind$smoothedWindZ);
        return livingwind$smoothedWindX;
    }

    @ModifyArg(method = "m_5989_", at = @At(value = "INVOKE",
        target = "Lcom/endertech/minecraft/mods/adchimneys/particles/AbstractSmokeParticle$Instance;m_6257_(DDD)V",
        remap = false), index = 2, require = 0, remap = false)
    private double livingwind$redirectOutdoorSmokeZ(double originalZ) {
        if (insideChimney || !AdvancedChimneysSmokeWind.active()) return originalZ;
        return livingwind$hasSmoothedWind ? livingwind$smoothedWindZ : originalZ;
    }
}
