package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.AmendmentsWindCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Amendments bridge: small wind force into native client-side pendulum physics. */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.amendments.common.PendulumAnimation", remap = false)
public abstract class AmendmentsPendulumWindMixin {
    @Shadow(remap = false) public abstract void addImpulse(double velocity);

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void livingwind$applyAtmosphericWind(Level level, BlockPos pos,
                                                  BlockState state, CallbackInfo ci) {
        double delta = AmendmentsWindCompat.windImpulse(level, pos, state);
        if (delta != 0.0) addImpulse(delta);
    }
}
