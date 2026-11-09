package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional block emitters, kept separate from datapack block tags so that users can change
 * compatibility defaults without modifying the mod's jar or a world datapack.
 * Missing mods are harmless: their registered blocks never appear in a level.
 */
public final class AmbientEmitterBlocks {
    public enum Kind { GRASS, DUST, SAND, SNOW, LEAF, NETHER_DUST, NONE }

    private static final Map<String, Kind> BASE_DEFAULTS = Map.ofEntries(
        Map.entry("aether:angelic_stone", Kind.DUST),
        Map.entry("aether:carved_stone", Kind.DUST),
        Map.entry("aether:hellfire_stone", Kind.DUST),
        Map.entry("aether:light_angelic_stone", Kind.DUST),
        Map.entry("aether_ii:aether_dirt", Kind.DUST),
        Map.entry("aether_ii:aether_dirt_path", Kind.DUST),
        Map.entry("aether_ii:aether_grass_block", Kind.GRASS),
        Map.entry("aether_ii:aether_long_grass", Kind.GRASS),
        Map.entry("aether_ii:aether_medium_grass", Kind.GRASS),
        Map.entry("aether_ii:aether_short_grass", Kind.GRASS),
        Map.entry("aether_ii:amberoot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:amberoot_leaves", Kind.LEAF),
        Map.entry("aether_ii:ambrelinn_moss_block", Kind.GRASS),
        Map.entry("aether_ii:ambrelinn_moss_vines", Kind.GRASS),
        Map.entry("aether_ii:arctic_snow", Kind.SNOW),
        Map.entry("aether_ii:arctic_snow_block", Kind.SNOW),
        Map.entry("aether_ii:blue_aercloud", Kind.DUST),
        Map.entry("aether_ii:bryalinn_moss_block", Kind.GRASS),
        Map.entry("aether_ii:bryalinn_moss_flowers", Kind.GRASS),
        Map.entry("aether_ii:bryalinn_moss_vines", Kind.GRASS),
        Map.entry("aether_ii:coarse_aether_dirt", Kind.DUST),
        Map.entry("aether_ii:cold_aercloud", Kind.SNOW),
        Map.entry("aether_ii:enchanted_aether_grass_block", Kind.GRASS),
        Map.entry("aether_ii:ferrosite_mud", Kind.DUST),
        Map.entry("aether_ii:ferrosite_sand", Kind.SAND),
        Map.entry("aether_ii:golden_aercloud", Kind.DUST),
        Map.entry("aether_ii:greatboa_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:greatboa_leaves", Kind.LEAF),
        Map.entry("aether_ii:greatoak_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:greatoak_leaves", Kind.LEAF),
        Map.entry("aether_ii:greatroot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:greatroot_leaves", Kind.LEAF),
        Map.entry("aether_ii:green_aercloud", Kind.DUST),
        Map.entry("aether_ii:holystone", Kind.DUST),
        Map.entry("aether_ii:holystone_rock", Kind.DUST),
        Map.entry("aether_ii:icestone", Kind.SNOW),
        Map.entry("aether_ii:irradiated_greatboa_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_greatboa_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_greatoak_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_greatoak_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_greatroot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_greatroot_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_holystone", Kind.DUST),
        Map.entry("aether_ii:irradiated_skybirch_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skybirch_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skypine_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skypine_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skyplane_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skyplane_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skyroot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_skyroot_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_wisproot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_wisproot_leaves", Kind.LEAF),
        Map.entry("aether_ii:irradiated_wisptop_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:irradiated_wisptop_leaves", Kind.LEAF),
        Map.entry("aether_ii:mossy_holystone", Kind.DUST),
        Map.entry("aether_ii:purple_aercloud", Kind.DUST),
        Map.entry("aether_ii:quicksoil", Kind.SAND),
        Map.entry("aether_ii:shayelinn_moss_block", Kind.GRASS),
        Map.entry("aether_ii:shayelinn_moss_vines", Kind.GRASS),
        Map.entry("aether_ii:skybirch_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:skybirch_leaves", Kind.LEAF),
        Map.entry("aether_ii:skypine_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:skypine_leaves", Kind.LEAF),
        Map.entry("aether_ii:skyplane_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:skyplane_leaves", Kind.LEAF),
        Map.entry("aether_ii:skyroot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:skyroot_leaves", Kind.LEAF),
        Map.entry("aether_ii:storm_aercloud", Kind.DUST),
        Map.entry("aether_ii:unstable_holystone", Kind.DUST),
        Map.entry("aether_ii:wisproot_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:wisproot_leaves", Kind.LEAF),
        Map.entry("aether_ii:wisptop_leaf_pile", Kind.LEAF),
        Map.entry("aether_ii:wisptop_leaves", Kind.LEAF),
        Map.entry("twilightforest:beanstalk_leaves", Kind.LEAF),
        Map.entry("twilightforest:canopy_leaves", Kind.LEAF),
        Map.entry("twilightforest:dark_leaves", Kind.LEAF),
        Map.entry("twilightforest:giant_cobblestone", Kind.DUST),
        Map.entry("twilightforest:mangrove_leaves", Kind.LEAF),
        Map.entry("twilightforest:mazestone", Kind.DUST),
        Map.entry("twilightforest:mining_leaves", Kind.LEAF),
        Map.entry("twilightforest:rainbow_oak_leaves", Kind.LEAF),
        Map.entry("twilightforest:root", Kind.DUST),
        Map.entry("twilightforest:sorting_leaves", Kind.LEAF),
        Map.entry("twilightforest:thorn_leaves", Kind.LEAF),
        Map.entry("twilightforest:time_leaves", Kind.LEAF),
        Map.entry("twilightforest:transformation_leaves", Kind.LEAF),
        Map.entry("twilightforest:twilight_oak_leaves", Kind.LEAF),
        Map.entry("aether:aether_dirt", Kind.DUST),
        Map.entry("aether:aether_dirt_path", Kind.DUST),
        Map.entry("aether:aether_grass_block", Kind.GRASS),
        Map.entry("aether:blue_aercloud", Kind.DUST),
        Map.entry("aether:cold_aercloud", Kind.SNOW),
        Map.entry("aether:crystal_fruit_leaves", Kind.LEAF),
        Map.entry("aether:crystal_leaves", Kind.LEAF),
        Map.entry("aether:decorated_holiday_leaves", Kind.LEAF),
        Map.entry("aether:enchanted_aether_grass_block", Kind.GRASS),
        Map.entry("aether:golden_aercloud", Kind.DUST),
        Map.entry("aether:golden_oak_leaves", Kind.LEAF),
        Map.entry("aether:holiday_leaves", Kind.LEAF),
        Map.entry("aether:holystone", Kind.DUST),
        Map.entry("aether:icestone", Kind.SNOW),
        Map.entry("aether:mossy_holystone", Kind.DUST),
        Map.entry("aether:quicksoil", Kind.SAND),
        Map.entry("aether:skyroot_leaves", Kind.LEAF),
        Map.entry("regions_unexplored:alpha_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:argillite", Kind.DUST),
        Map.entry("regions_unexplored:argillite_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:ash", Kind.DUST),
        Map.entry("regions_unexplored:ash_vent", Kind.DUST),
        Map.entry("regions_unexplored:ashen_dirt", Kind.DUST),
        Map.entry("regions_unexplored:ashen_grass", Kind.GRASS),
        Map.entry("regions_unexplored:barley", Kind.GRASS),
        Map.entry("regions_unexplored:bladed_grass", Kind.GRASS),
        Map.entry("regions_unexplored:bladed_tall_grass", Kind.GRASS),
        Map.entry("regions_unexplored:brimsprout_nylium", Kind.NETHER_DUST),
        Map.entry("regions_unexplored:chalk", Kind.DUST),
        Map.entry("regions_unexplored:chalk_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:cobalt_nylium", Kind.NETHER_DUST),
        Map.entry("regions_unexplored:cobalt_obsidian", Kind.NETHER_DUST),
        Map.entry("regions_unexplored:deepslate_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:deepslate_prismoss", Kind.GRASS),
        Map.entry("regions_unexplored:deepslate_viridescent_nylium", Kind.GRASS),
        Map.entry("regions_unexplored:frozen_grass", Kind.GRASS),
        Map.entry("regions_unexplored:glistering_fern", Kind.GRASS),
        Map.entry("regions_unexplored:glistering_nylium", Kind.NETHER_DUST),
        Map.entry("regions_unexplored:glistering_wart", Kind.NETHER_DUST),
        Map.entry("regions_unexplored:grass_sprouts", Kind.GRASS),
        Map.entry("regions_unexplored:medium_grass", Kind.GRASS),
        Map.entry("regions_unexplored:mossy_stone", Kind.DUST),
        Map.entry("regions_unexplored:mycotoxic_grass", Kind.GRASS),
        Map.entry("regions_unexplored:mycotoxic_moss", Kind.GRASS),
        Map.entry("regions_unexplored:peat_coarse_dirt", Kind.DUST),
        Map.entry("regions_unexplored:peat_dirt", Kind.DUST),
        Map.entry("regions_unexplored:peat_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:peat_mud", Kind.DUST),
        Map.entry("regions_unexplored:peat_podzol", Kind.DUST),
        Map.entry("regions_unexplored:prismoss", Kind.GRASS),
        Map.entry("regions_unexplored:prismoss_sprout", Kind.GRASS),
        Map.entry("regions_unexplored:sandy_grass", Kind.GRASS),
        Map.entry("regions_unexplored:sandy_tall_grass", Kind.GRASS),
        Map.entry("regions_unexplored:silt_coarse_dirt", Kind.DUST),
        Map.entry("regions_unexplored:silt_dirt", Kind.DUST),
        Map.entry("regions_unexplored:silt_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:silt_mud", Kind.DUST),
        Map.entry("regions_unexplored:silt_podzol", Kind.DUST),
        Map.entry("regions_unexplored:steppe_grass", Kind.GRASS),
        Map.entry("regions_unexplored:steppe_shrub", Kind.GRASS),
        Map.entry("regions_unexplored:steppe_tall_grass", Kind.GRASS),
        Map.entry("regions_unexplored:stone_grass_block", Kind.GRASS),
        Map.entry("regions_unexplored:viridescent_nylium", Kind.GRASS),
        Map.entry("regions_unexplored:volcanic_ash", Kind.DUST),
        Map.entry("regions_unexplored:windswept_grass", Kind.GRASS),
        Map.entry("twilightforest:clover_patch", Kind.GRASS),
        Map.entry("twilightforest:cracked_deadrock", Kind.DUST),
        Map.entry("twilightforest:deadrock", Kind.DUST),
        Map.entry("twilightforest:fallen_leaves", Kind.LEAF),
        Map.entry("twilightforest:fiddlehead", Kind.GRASS),
        Map.entry("twilightforest:fluffy_cloud", Kind.DUST),
        Map.entry("twilightforest:giant_leaves", Kind.LEAF),
        Map.entry("twilightforest:hardened_dark_leaves", Kind.LEAF),
        Map.entry("twilightforest:mayapple", Kind.GRASS),
        Map.entry("twilightforest:moss_patch", Kind.GRASS),
        Map.entry("twilightforest:rainy_cloud", Kind.DUST),
        Map.entry("twilightforest:snowy_cloud", Kind.SNOW),
        Map.entry("twilightforest:trollsteinn", Kind.DUST),
        Map.entry("twilightforest:uberous_soil", Kind.GRASS),
        Map.entry("twilightforest:weathered_deadrock", Kind.DUST),
        Map.entry("twilightforest:wispy_cloud", Kind.DUST)
    );

    // Combine the existing RU/Aether/Twilight mappings with optional biome mod presets.
    // This is computed only once at class initialization, not while sampling particles.
    private static final Map<String, Kind> DEFAULTS = buildDefaults();

    private static Map<String, Kind> buildDefaults() {
        Map<String, Kind> out = new HashMap<>(BASE_DEFAULTS);
        out.putAll(BiomeModAmbientDefaults.entries());
        return Map.copyOf(out);
    }

    private static volatile List<? extends String> lastEntries;
    private static volatile Map<ResourceLocation, Kind> lastOverrides = Map.of();

    private AmbientEmitterBlocks() {}

    public static boolean validEntry(Object candidate) {
        if (!(candidate instanceof String s)) return false;
        int split = s.lastIndexOf('=');
        if (split <= 0 || split == s.length() - 1) return false;
        if (ResourceLocation.tryParse(s.substring(0, split).trim()) == null) return false;
        try {
            Kind.valueOf(s.substring(split + 1).trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static Map<ResourceLocation, Kind> overrides() {
        List<? extends String> entries = ClientWindConfig.AMBIENT_BLOCK_EMITTER_OVERRIDES.get();
        if (entries == lastEntries) return lastOverrides;
        synchronized (AmbientEmitterBlocks.class) {
            if (entries != lastEntries) {
                Map<ResourceLocation, Kind> parsed = new HashMap<>();
                for (String entry : entries) {
                    if (!validEntry(entry)) continue;
                    int separator = entry.lastIndexOf('=');
                    ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, separator).trim());
                    parsed.put(id, Kind.valueOf(entry.substring(separator + 1).trim().toUpperCase(Locale.ROOT)));
                }
                lastOverrides = Map.copyOf(parsed);
                lastEntries = entries;
            }
        }
        return lastOverrides;
    }

    /** NULL means defer to the datapack tags, NONE explicitly disables this block. */
    public static Kind resolve(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        Kind override = overrides().get(id);
        return override != null ? override : DEFAULTS.get(id.toString());
    }

    public static boolean isDefault(String id) { return DEFAULTS.containsKey(id); }
    public static Kind defaultKind(String id) { return DEFAULTS.getOrDefault(id, Kind.DUST); }

    /** All known presets, including absent compatibility mods, for UI browsing only. */
    public static List<String> allDefaults() {
        List<String> result = new ArrayList<>(DEFAULTS.keySet());
        result.sort(String::compareTo);
        return result;
    }

    /** The config editor shows compatibility presets only for mods actually installed. */
    public static List<String> installedDefaults() {
        List<String> out = new ArrayList<>();
        for (String id : DEFAULTS.keySet()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null && BuiltInRegistries.BLOCK.containsKey(key)) out.add(id);
        }
        out.sort(String::compareTo);
        return out;
    }
}
