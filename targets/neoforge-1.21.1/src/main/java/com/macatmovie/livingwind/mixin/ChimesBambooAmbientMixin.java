package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.ChimesWindMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Only changes the existing random ambient sound chance, never interaction sounds. */
@Pseudo
@Mixin(targets = "com.cicada.chimes.block.CarvedBambooWindChimeBlock", remap = false)
public abstract class ChimesBambooAmbientMixin {
    @ModifyConstant(method = "animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            constant = @Constant(floatValue = 0.015f), remap = false, require = 0)
    private float livingwind$ambientChance(float original, BlockState state,
                                    Level level, BlockPos pos, RandomSource random) {
        return ChimesWindMotion.adjustedChance(original, level, pos);
    }
}
