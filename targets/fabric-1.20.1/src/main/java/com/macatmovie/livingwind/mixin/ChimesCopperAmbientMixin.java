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
@Mixin(targets = "com.cicada.chimes.block.CopperWindChimeBlock", remap = false)
public abstract class ChimesCopperAmbientMixin {
    @ModifyConstant(method = "method_9496",
            constant = @Constant(intValue = 100), remap = false, require = 0)
    private int livingwind$ambientChance(int original, BlockState state,
                                    Level level, BlockPos pos, RandomSource random) {
        return ChimesWindMotion.adjustedDenominator(original, level, pos);
    }
}
