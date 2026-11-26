package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.ChimesWindMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Chimes 2.2.0: replace the glass bell's native ambient probability only. */
@Pseudo
@Mixin(targets = "com.cicada.chimes.block.entity.WindBellBE", remap = false)
public abstract class Chimes26BellAmbientMixin {
    @ModifyConstant(method = "broadcastLocalChimingSounds(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        constant = @Constant(doubleValue = 0.03), require = 0, remap = false)
    private double livingwind$ambientChance(double original, Level level, BlockPos pos, RandomSource random) {
        return ChimesWindMotion.adjustedChance((float)original, level, pos);
    }
}
