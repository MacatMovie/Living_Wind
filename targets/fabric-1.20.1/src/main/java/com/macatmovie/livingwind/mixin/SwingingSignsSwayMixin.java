package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.SwingingSignsWindCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Fabric Swinging Hanging Signs v1.0.1: replace only its ambient sway component.
 * The mod's spring angle is added afterward, so interactions retain native physics.
 *
 * The Fabric JAR keeps its own method name `angle` while Minecraft argument types
 * are mapped to intermediary at runtime; use the simple method name rather than
 * the Forge/NeoForge Mojang-mapped descriptor. Both supplied Fabric versions store
 * the ambient float in local variable slot 9.
 */
@Pseudo
@Mixin(targets = "dev.swingingsigns.client.Swing", remap = false)
public abstract class SwingingSignsSwayMixin {
    @ModifyVariable(method = "angle", at = @At(value = "STORE"),
            index = 9, remap = false, require = 0)
    private static float livingwind$ambientSway(float original, BlockPos pos,
                                               BlockState state, float partialTick) {
        return SwingingSignsWindCompat.ambientSway(original, pos, state, partialTick);
    }
}
