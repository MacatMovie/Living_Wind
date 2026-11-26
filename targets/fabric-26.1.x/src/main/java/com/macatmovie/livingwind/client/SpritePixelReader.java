package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.mixin.SpriteContentsOriginalImageAccessor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Fabric Mojang mappings do not expose the former getPixelRGBA method. Read the backing sprite image. */
public final class SpritePixelReader {
    private SpritePixelReader() {}
    public static int getPixelRGBA(TextureAtlasSprite sprite, int mipLevel, int x, int y) {
        // Palettes sample mip level zero, identical to the old TextureAtlasSprite method.
        int argb = ((SpriteContentsOriginalImageAccessor) (Object) sprite.contents())
            .livingwind$getOriginalImage().getPixel(x, y);
        return (argb & 0xFF00FF00) | ((argb >>> 16) & 255) | ((argb & 255) << 16);
    }
}
