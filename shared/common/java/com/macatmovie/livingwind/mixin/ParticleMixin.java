package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.ParticleWaterProtection;
import com.macatmovie.livingwind.client.ParticleShelterContinuity;
import com.macatmovie.livingwind.client.JadensNetherExpansionParticleCompat;
import com.macatmovie.livingwind.client.AtmosphericsDustCompat;
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
    @Shadow protected boolean onGround;

    @Unique private @Nullable ResourceLocation livingwind$particleType;
    @Unique private boolean livingwind$particularLeafSettled;
    @Unique private int livingwind$lastShelteredTick = -1;
    @Unique private int livingwind$lastShelteredX;
    @Unique private int livingwind$lastShelteredY;
    @Unique private int livingwind$lastShelteredZ;
    @Unique private int livingwind$exposureTick = -1;
    @Unique private double livingwind$exposureX, livingwind$exposureY, livingwind$exposureZ;
    @Unique private float livingwind$exposureValue;
    @Unique private float livingwind$visualExposure = Float.NaN;
    @Unique private int livingwind$visualExposureTick = -1;

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

    @Override public int livingwind$getLastShelteredTick() { return livingwind$lastShelteredTick; }
    @Override public int livingwind$getLastShelteredX() { return livingwind$lastShelteredX; }
    @Override public int livingwind$getLastShelteredY() { return livingwind$lastShelteredY; }
    @Override public int livingwind$getLastShelteredZ() { return livingwind$lastShelteredZ; }

    @Override
    public void livingwind$rememberSheltered(int tick, int x, int y, int z) {
        livingwind$lastShelteredTick = tick;
        livingwind$lastShelteredX = x;
        livingwind$lastShelteredY = y;
        livingwind$lastShelteredZ = z;
    }

    @Override
    public float livingwind$cachedExposure(int tick, double px, double py, double pz) {
        return livingwind$exposureTick == tick && livingwind$exposureX == px
            && livingwind$exposureY == py && livingwind$exposureZ == pz
            ? livingwind$exposureValue : Float.NaN;
    }

    @Override
    public void livingwind$storeExposure(int tick, double px, double py, double pz, float exposure) {
        livingwind$exposureTick = tick;
        livingwind$exposureX = px;
        livingwind$exposureY = py;
        livingwind$exposureZ = pz;
        livingwind$exposureValue = exposure;
    }

    @Override
    public float livingwind$easeExposure(float target, int tick) {
        if (livingwind$visualExposureTick == tick) return livingwind$visualExposure;
        // Pauses do not advance shelterTick. New or resumed particles start from
        // their actual state rather than reusing a stale wind envelope.
        if (livingwind$visualExposureTick < 0 || tick < livingwind$visualExposureTick
                || tick - livingwind$visualExposureTick > 20) {
            livingwind$visualExposure = target;
        } else {
            livingwind$visualExposure = ParticleShelterContinuity.easeVisualExposure(
                livingwind$visualExposure, target);
        }
        livingwind$visualExposureTick = tick;
        return livingwind$visualExposure;
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
        } else if (ParticleWindFilter.usesLegacyParticleRainSnowCompat(
                livingwind$particleType, (Particle) (Object) this)) {
            // Particle Rain 3.x snow otherwise keeps its hardcoded diagonal
            // velocity forever. Set the actual velocity once per tick so
            // strength/direction changes, including gusts, remain visible.
            this.xd = ParticleWindFilter.legacyParticleRainSnowVelocityX(this.x, this.y, this.z);
            this.zd = ParticleWindFilter.legacyParticleRainSnowVelocityZ(this.x, this.y, this.z);
        } else if (ParticleWindFilter.usesPrettyRainDirectionOnlyCompat(livingwind$particleType)) {
            // Shrub/tumbleweed speed belongs to Pretty Rain. Rotate its current speed into Living
            // Wind's direction rather than adding force or scaling the magnitude.
            ParticleWindFilter.orientPrettyRainDirectionOnly(this);
        }
    }

    /**
     * Particular Reforged does not register separate particles for landed leaves.
     * Its falling-leaf instance changes into a horizontal, fading ground decal
     * as soon as it detects a collision. Generic wind should affect it while
     * airborne, but continuing to move that same instance after landing makes
     * the near-coplanar decal flicker against the block beneath it.
     *
     * Latch the grounded state so subsequent changes in onGround due to movement
     * or collision interpolation cannot restart wind on a resting leaf. This
     * touches no other particles and requires no optional Particular classes.
     */
    @Unique
    private boolean livingwind$skipGroundedParticularLeafWind() {
        if (livingwind$particularLeafSettled) return true;
        if (this.onGround && ParticleWindFilter.isParticularLeaf(livingwind$particleType)) {
            livingwind$particularLeafSettled = true;
            return true;
        }
        return false;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double livingwind$applyWindX(double movementX) {
        if (livingwind$skipGroundedParticularLeafWind()) return movementX;
        double atmospherics = AtmosphericsDustCompat.replacementX(livingwind$particleType, (Particle) (Object) this);
        if (!Double.isNaN(atmospherics)) return atmospherics;
        double jne = JadensNetherExpansionParticleCompat.replacementX(livingwind$particleType, (Particle) (Object) this);
        if (!Double.isNaN(jne)) return jne;
        return movementX + ParticleWindFilter.offsetX(livingwind$particleType, (Particle) (Object) this);
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double livingwind$applyWindZ(double movementZ) {
        if (livingwind$skipGroundedParticularLeafWind()) return movementZ;
        double atmospherics = AtmosphericsDustCompat.replacementZ(livingwind$particleType, (Particle) (Object) this);
        if (!Double.isNaN(atmospherics)) return atmospherics;
        double jne = JadensNetherExpansionParticleCompat.replacementZ(livingwind$particleType, (Particle) (Object) this);
        if (!Double.isNaN(jne)) return jne;
        return movementZ + ParticleWindFilter.offsetZ(livingwind$particleType, (Particle) (Object) this);
    }
}
