package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Pseudo
@Mixin(targets="fuzs.fallingleavesplus.common.client.particle.CustomFallingLeavesParticle", remap=false)
public abstract class FallingLeavesPlus26WindMixin {
    @Unique private int livingwind$windMoves;
    @ModifyVariable(method="move(DDD)V", at=@At("HEAD"), argsOnly=true, ordinal=0, require=0, remap=false)
    private double livingwind$steerX(double x) { return livingwind$steer(x, true); }
    @ModifyVariable(method="move(DDD)V", at=@At("HEAD"), argsOnly=true, ordinal=2, require=0, remap=false)
    private double livingwind$steerZ(double z) { return livingwind$steer(z, false); }
    @Unique private double livingwind$steer(double movement, boolean xAxis) {
        WindParticleAccess a = (WindParticleAccess)(Object)this;
        Particle p = (Particle)(Object)this;
        if (a.livingwind$isOnGround() || !ParticleWindFilter.controlsFallingLeavesPlusWind(a.livingwind$getParticleType(), p)) return movement;
        if (xAxis && livingwind$windMoves < 12) livingwind$windMoves++;
        double wx=ParticleWindFilter.offsetX(a.livingwind$getParticleType(), p), wz=ParticleWindFilter.offsetZ(a.livingwind$getParticleType(), p);
        double mag=Math.hypot(wx,wz);
        if (!Double.isFinite(mag) || mag < 0.00001) return movement;
        double target=(xAxis?wx:wz)*Math.min(1.0,0.10/Math.max(mag,0.00001));
        double v=movement+(target-movement)*0.24*Math.min(1.0,livingwind$windMoves/12.0)*Math.min(1.0,mag/0.025);
        a.livingwind$setHorizontalVelocity(xAxis?v:a.livingwind$getVelocityX(), xAxis?a.livingwind$getVelocityZ():v);
        return v;
    }
}
