package com.macatmovie.livingwind.client;

import java.util.HashMap;
import java.util.Map;
import com.macatmovie.livingwind.client.AmbientEmitterBlocks.Kind;

/**
 * Optional natural terrain and vegetation emitters for three biome mods.
 * Only names verified against the supplied mod jars are included. Missing blocks are safe:
 * the ambient sampler resolves them by registered block ID, not by loading optional classes.
 * Most modded tree leaves are already included via #minecraft:leaves.
 */
public final class BiomeModAmbientDefaults {
    private BiomeModAmbientDefaults() {}

    public static Map<String, Kind> entries() {
        Map<String, Kind> out = new HashMap<>();

        // Biomes O' Plenty
        add(out, "biomesoplenty", Kind.GRASS,
            "origin_grass_block", "glowing_moss_block", "glowing_moss_carpet", "bush",
            "dead_grass", "desert_grass", "dune_grass", "high_grass",
            "high_grass_plant", "sprout", "tundra_shrub", "wildflower",
            "cattail", "spanish_moss", "spanish_moss_plant");
        add(out, "biomesoplenty", Kind.SAND,
            "black_sand", "orange_sand", "white_sand", "mossy_black_sand");
        add(out, "biomesoplenty", Kind.DUST,
            "dried_salt", "algal_end_stone", "null_end_stone", "unmapped_end_stone",
            "black_sandstone", "orange_sandstone", "white_sandstone");
        add(out, "biomesoplenty", Kind.NETHER_DUST,
            "brimstone");
        add(out, "biomesoplenty", Kind.LEAF,
            "dead_leaves", "red_maple_leaf_pile", "orange_maple_leaf_pile", "yellow_maple_leaf_pile");

        // Additional BOP wild plants and natural Nether/End materials found in worldgen.
        add(out, "biomesoplenty", Kind.GRASS,
            "barley", "clover", "sea_oats", "reed", "lavender");
        add(out, "biomesoplenty", Kind.DUST,
            "thermal_calcite", "rose_quartz_block");
        add(out, "biomesoplenty", Kind.NETHER_DUST,
            "flesh", "porous_flesh");

        // Oh The Biomes We've Gone
        add(out, "biomeswevegone", Kind.GRASS,
            "lush_grass_block", "overgrown_stone", "beach_grass", "tall_beach_grass",
            "prairie_grass", "tall_prairie_grass", "shrub", "blueberry_bush",
            "cattail_sprout", "cattail", "flower_patch", "snowdrops",
            "spirit_roots", "fluorescent_cattail_sprout", "fluorescent_cattail");
        add(out, "biomeswevegone", Kind.SAND,
            "black_sand", "blue_sand", "pink_sand", "purple_sand",
            "white_sand", "windswept_sand", "cracked_sand", "cracked_red_sand",
            "quicksand", "red_quicksand");
        add(out, "biomeswevegone", Kind.DUST,
            "lush_dirt", "lush_dirt_path", "peat", "sandy_dirt",
            "sandy_dirt_path", "sandy_farmland", "dacite", "dacite_cobblestone",
            "white_dacite", "white_dacite_cobblestone", "mossy_stone", "red_rock",
            "rocky_stone", "pale_mud", "packed_pale_mud", "black_sandstone",
            "blue_sandstone", "pink_sandstone", "purple_sandstone", "white_sandstone",
            "windswept_sandstone");
        add(out, "biomeswevegone", Kind.LEAF,
            "leaf_pile");

        // Nature's Spirit
        add(out, "natures_spirit", Kind.GRASS,
            "red_moss_block", "red_moss_carpet", "beach_grass", "tall_beach_grass",
            "frigid_grass", "tall_frigid_grass", "melic_grass", "tall_melic_grass",
            "oat_grass", "tall_oat_grass", "scorched_grass", "tall_scorched_grass",
            "sedge_grass", "tall_sedge_grass", "lush_fern", "large_lush_fern",
            "flaxen_fern", "large_flaxen_fern", "green_bitter_sprouts", "red_bitter_sprouts",
            "purple_bitter_sprouts", "coconut_sprout", "silverbush", "purple_wildflower",
            "yellow_wildflower", "cattail");
        add(out, "natures_spirit", Kind.SAND,
            "pink_sand");
        add(out, "natures_spirit", Kind.DUST,
            "sandy_soil", "chert", "travertine", "cobbled_travertine",
            "mossy_cobbled_travertine", "pink_sandstone");

        // The colored kaolin materials are widely used by Nature's Spirit terrain
        // generation. They need separate IDs so each keeps its native block color.
        add(out, "natures_spirit", Kind.DUST,
            "kaolin", "white_kaolin", "orange_kaolin", "magenta_kaolin",
            "light_blue_kaolin", "yellow_kaolin", "lime_kaolin", "pink_kaolin",
            "gray_kaolin", "light_gray_kaolin", "cyan_kaolin", "purple_kaolin",
            "blue_kaolin", "brown_kaolin", "green_kaolin", "red_kaolin", "black_kaolin");
        // Chalk is an optional ambient surface when placed in exposed terrain.
        add(out, "natures_spirit", Kind.DUST,
            "white_chalk", "orange_chalk", "magenta_chalk", "light_blue_chalk",
            "yellow_chalk", "lime_chalk", "pink_chalk", "gray_chalk",
            "light_gray_chalk", "cyan_chalk", "purple_chalk", "blue_chalk",
            "brown_chalk", "green_chalk", "red_chalk", "black_chalk");

        return Map.copyOf(out);
    }

    private static void add(Map<String, Kind> out, String namespace, Kind kind, String... names) {
        for (String name : names) out.put(namespace + ":" + name, kind);
    }
}
