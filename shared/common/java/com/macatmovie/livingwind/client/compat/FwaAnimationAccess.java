package com.macatmovie.livingwind.client.compat;

import net.minecraft.core.BlockPos;

/** Implemented by an optional Mixin on Fancy World Animations' base Animation class. */
public interface FwaAnimationAccess {
    BlockPos livingwind$getPosition();
}
