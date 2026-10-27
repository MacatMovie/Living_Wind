package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.SwingingSignsWindCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replaces only SwayMath's ambient result; the original spring angle is added afterward. */
@Pseudo
@Mixin(targets = "dev.swingingsigns.client.Swing", remap = false)
public abstract class SwingingSignsSwayMixin {
    // Both published v1.0.0 JARs store ambient sway in local float slot 9.
    @ModifyVariable(method = "angle(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)F",
            at = @At(value = "STORE"), index = 9, remap = false, require = 0)
    private static float livingwind$ambientSway(float original, BlockPos pos,
                                                BlockState state, float partialTick) {
        return SwingingSignsWindCompat.ambientSway(original, pos, state, partialTick);
    }
}
