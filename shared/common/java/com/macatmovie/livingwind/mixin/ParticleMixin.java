package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.ParticleWaterProtection;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class ParticleMixin implements WindParticleAccess {
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow protected double xd;
    @Shadow protected double zd;

    @Unique private @Nullable ResourceLocation livingwind$particleType;

    @Override
    public void livingwind$setParticleType(@Nullable ResourceLocation id) { livingwind$particleType = id; }

    @Override
    public @Nullable ResourceLocation livingwind$getParticleType() { return livingwind$particleType; }

    @Override
    public double livingwind$getPositionX() { return this.x; }

    @Override
    public double livingwind$getPositionY() { return this.y; }

    @Override
    public double livingwind$getPositionZ() { return this.z; }

    @Override
    public double livingwind$getVelocityX() { return this.xd; }

    @Override
    public double livingwind$getVelocityZ() { return this.zd; }

    @Override
    public void livingwind$setHorizontalVelocity(double x, double z) {
        this.xd = x;
        this.zd = z;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void livingwind$applyPrettyRainVelocity(CallbackInfo ci) {
        // Generic wind is guarded in ParticleWindFilter; special Pretty Rain movement
        // needs the same water rule because it bypasses the generic offset hook.
        if ((ParticleWindFilter.usesPrettyRainVelocityCompat(livingwind$particleType)
                || ParticleWindFilter.usesPrettyRainDirectionOnlyCompat(livingwind$particleType))
                && ParticleWaterProtection.isSubmerged(this.x, this.y, this.z)) return;
        if (ParticleWindFilter.usesPrettyRainVelocityCompat(livingwind$particleType)) {
            // Pretty Rain rain uses velocity for streak orientation, while its desert dust resets
            // itself toward a fixed +X/+Z drift. Replacing horizontal velocity at Particle.tick
            // keeps both effects aligned with Living Wind without touching Pretty Rain's renderer.
            this.xd = ParticleWindFilter.prettyRainVelocityX(this.x, this.y, this.z);
            this.zd = ParticleWindFilter.prettyRainVelocityZ(this.x, this.y, this.z);
        } else if (ParticleWindFilter.usesPrettyRainDirectionOnlyCompat(livingwind$particleType)) {
            // Shrub/tumbleweed speed belongs to Pretty Rain. Rotate its current speed into Living
            // Wind's direction rather than adding force or scaling the magnitude.
            ParticleWindFilter.orientPrettyRainDirectionOnly(this);
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double livingwind$applyWindX(double movementX) {
        return movementX + ParticleWindFilter.offsetX(livingwind$particleType, (Particle) (Object) this);
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double livingwind$applyWindZ(double movementZ) {
        return movementZ + ParticleWindFilter.offsetZ(livingwind$particleType, (Particle) (Object) this);
    }
}
