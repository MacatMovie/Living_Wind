package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.ChimesWindMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Chimes 2.2.0: all tall chimes now share this client block-entity sound path. */
@Pseudo
@Mixin(targets = "com.cicada.chimes.block.entity.WindChimeBE", remap = false)
public abstract class Chimes26AmbientMixin {
    @ModifyVariable(method = "broadcastLocalChimingSounds(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At("STORE"), index = 4, require = 0, remap = false)
    private float livingwind$ambientChance(float original, Level level, BlockPos pos, RandomSource random) {
        return ChimesWindMotion.adjustedChance(original, level, pos);
    }
}
