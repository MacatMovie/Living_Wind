package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.server.AeronauticsKineticBearingWind;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional: physically drive Transmission & Linkage's kinetic conversion bearing. */
@Pseudo
@Mixin(targets = "com.enxv.aerouniversaljoint.content.DampingStressBearingBlockEntity", remap = false)
public abstract class AeronauticsKineticBearingMixin {
    @Unique private final AeronauticsKineticBearingWind.RotorCache livingwind$rotor =
            new AeronauticsKineticBearingWind.RotorCache();

    // At TAIL of this helper we know the native bearing has already verified
    // that the physics callback belongs to its own assembled rotor, rather
    // than a different body in the same Sable physics system.
    @Inject(method = "aeronautics$runResistancePhysicsTick", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$driveRotor(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep, CallbackInfo ci) {
        livingwind$rotor.physicsTick((BlockEntity) (Object) this, subLevel, handle, timeStep);
    }
}
