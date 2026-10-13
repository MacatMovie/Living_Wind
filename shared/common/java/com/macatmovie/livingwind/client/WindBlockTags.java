package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class WindBlockTags {
    public static final TagKey<Block> LEAF_EMITTERS = tag("leaf_emitters");
    public static final TagKey<Block> GRASS_EMITTERS = tag("grass_emitters");
    public static final TagKey<Block> DUST_EMITTERS = tag("dust_emitters");
    public static final TagKey<Block> SAND_EMITTERS = tag("sand_emitters");
    public static final TagKey<Block> SNOW_EMITTERS = tag("snow_emitters");

    private WindBlockTags() {}
    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, LivingWind.id(path));
    }
}
