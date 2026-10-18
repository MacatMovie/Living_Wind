package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.server.SableWindForceHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Sable integration. Sable's observer API has assembly/removal events,
 * but no callback for blocks added to an already assembled contraption. Its
 * existing server-side block-change path has the precise event we need.
 * No Sable classes are referenced in the mixin signature.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.SableCommonEvents", remap = false)
public abstract class SableBlockChangeMixin {
    @Inject(method = "handleBlockChange", at = @At("TAIL"), remap = false, require = 0)
    private static void livingwind$changedBlock(ServerLevel level, LevelChunk chunk,
            int x, int y, int z, BlockState oldState, BlockState newState, CallbackInfo ci) {
        SableWindForceHandler.onSableBlockChanged(level, chunk, oldState, newState);
    }
}
