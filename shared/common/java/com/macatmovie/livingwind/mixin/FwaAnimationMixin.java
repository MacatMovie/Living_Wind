package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.FwaAnimationAccess;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** Optional bridge, loaded only if Fancy World Animations is present. */
@Pseudo
@Mixin(targets = "fr.madu59.fwa.anims.Animation", remap = false)
public abstract class FwaAnimationMixin implements FwaAnimationAccess {
    @Shadow(remap = false) public abstract BlockPos getPos();

    @Override
    public BlockPos livingwind$getPosition() {
        return getPos();
    }
}
