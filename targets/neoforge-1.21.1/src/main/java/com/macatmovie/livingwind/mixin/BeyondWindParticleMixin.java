package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.ParticleWaterProtection;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Beyond WindParticle::move() is a custom, no-argument velocity jitter
 * routine (not Particle::move(x,y,z)). Replace its horizontal result AFTER
 * its vertical jitter so it cannot stack its wind direction with Living Wind.
 */
@Pseudo
@Mixin(targets = "com.thebeyond.client.particle.WindParticle", remap = false)
public abstract class BeyondWindParticleMixin {
    private static final ResourceLocation WIND_ID =
        ResourceLocation.fromNamespaceAndPath("the_beyond", "wind");

    @Inject(method = "move()V", at = @At("TAIL"), require = 0, remap = false)
    private void livingwind$steerBeyondWind(CallbackInfo ci) {
        if (!ClientWindConfig.BEYOND_WIND_PARTICLE_COMPAT.get()
            || !ClientWindState.hasServerState() || ClientWindState.dimensionWindDisabled()) return;
        var level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.END) return;
        Particle self = (Particle) (Object) this;
        if (!(self instanceof WindParticleAccess a) || !WIND_ID.equals(a.livingwind$getParticleType())
            || !ParticleWindFilter.allowsBeyondWindSteering(self)) return;
        double x = a.livingwind$getPositionX();
        double y = a.livingwind$getPositionY();
        double z = a.livingwind$getPositionZ();
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return;
        float strength = ClientWindState.strengthAt(x, y, z);
        // No local wind: preserve the block's original small directional puff.
        if (strength < 0.02f) return;
        float speed = Math.min(0.22f, Math.max(0.0f, strength) * 0.09f);
        a.livingwind$setHorizontalVelocity(
            WindMath.windX(speed, ClientWindState.directionDegrees()),
            WindMath.windZ(speed, ClientWindState.directionDegrees()));
    }
}
