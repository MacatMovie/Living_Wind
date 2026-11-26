package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.InteractiveFoliageWindCompat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Interactive Foliage bakes weather-wind exposure per vertex when it builds its
 * GPU foliage meshes. Water plants still need its ordinary calm sway and entity
 * pushes, but should not receive Living Wind's atmospheric wind.
 *
 * The existing exposure channel is enough: zero means no weather wind, without
 * changing the calm sway or the packed movement/push weights. This runs during
 * mesh generation, not on the render thread every frame.
 *
 * Optional and version-checked against IF 2.2.0 (Forge/NeoForge) and 2.2.1
 * (Fabric). No Interactive Foliage class is needed at compile time.
 */
@Pseudo
@Mixin(targets = "net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageRenderer$SwayAnchor", remap = false)
public abstract class InteractiveFoliageUnderwaterMixin {
    @Shadow(remap = false) private BlockState state;

    @Inject(method = "exposureAt(FFF)F", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void livingwind$ignoreWindForSubmergedFoliage(float x, float y, float z,
            CallbackInfoReturnable<Float> cir) {
        if (InteractiveFoliageWindCompat.excludeSubmergedFoliage(state)) {
            cir.setReturnValue(0.0f);
        }
    }
}
