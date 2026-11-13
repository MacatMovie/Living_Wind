package com.macatmovie.livingwind.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Loader/version adapter: retain the existing native sprite pixel access. */
public final class SpritePixelReader {
    private SpritePixelReader() {}
    public static int getPixelRGBA(TextureAtlasSprite sprite, int mipLevel, int x, int y) {
        return sprite.getPixelRGBA(mipLevel, x, y);
    }
}
