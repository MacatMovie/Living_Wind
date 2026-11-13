package com.macatmovie.livingwind.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access the vanilla texture image for cached foliage color sampling. Client only. */
@Mixin(SpriteContents.class)
public interface SpriteContentsOriginalImageAccessor {
    @Accessor("originalImage")
    NativeImage livingwind$getOriginalImage();
}
