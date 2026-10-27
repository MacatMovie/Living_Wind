package com.macatmovie.livingwind.client;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Built-in movement multipliers for optional mods. Keeps new compatibility tuning active
 * in existing user configs without overwriting their explicit response entries.
 */
public final class BuiltinParticleResponseMultipliers {
    public static final Map<ResourceLocation, Float> DEFAULTS = Map.ofEntries(
        Map.entry(ResourceLocation.tryParse("flying_stuff:cloud"), 0.25f),
        Map.entry(ResourceLocation.tryParse("flying_stuff:cloud_smoke"), 0.25f),
        Map.entry(ResourceLocation.tryParse("flying_stuff:cloud_smoke_1"), 0.25f),
        Map.entry(ResourceLocation.tryParse("flying_stuff:cloud_barrier_2"), 0.25f),
        Map.entry(ResourceLocation.tryParse("flying_stuff:cloud_barrier_3"), 0.25f),
        Map.entry(ResourceLocation.tryParse("minecraft:firework"), 0.25f),
        Map.entry(ResourceLocation.tryParse("minecraft:poof"), 0.35f),
        Map.entry(ResourceLocation.tryParse("minecraft:cloud"), 0.35f),
        Map.entry(ResourceLocation.tryParse("streamsreflowing:stream_splash"), 0.20f),
        Map.entry(ResourceLocation.tryParse("streamsreflowing:stream_foam"), 0.20f),
        Map.entry(ResourceLocation.tryParse("particular:oak_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:birch_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:spruce_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:jungle_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:acacia_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:dark_oak_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:azalea_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:mangrove_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:white_oak_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:white_spruce_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:maple_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:brimwood_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:ru_baobab_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:kapok_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:eucalyptus_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:redwood_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:magnolia_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:ru_palm_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:larch_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:golden_larch_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:socotra_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:bamboo_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:willow_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:ru_cypress_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:ww_baobab_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:ww_cypress_leaf"), 2.5f),
        Map.entry(ResourceLocation.tryParse("particular:ww_palm_leaf"), 2.5f)
    );

    /**
     * The previous test build inadvertently saved unedited 0.3x Streams Reflowing
     * built-ins when the multiplier editor was opened. Ignore only those exact
     * obsolete values so existing test configs pick up the new defaults. Other
     * explicit user multipliers stay untouched.
     */
    public static boolean isLegacyStreamsDefault(String id, String rawValue) {
        boolean oldStreamId = switch (id) {
            case "streamsreflowing:stream_bubble", "streamsreflowing:stream_splash",
                 "streamsreflowing:stream_foam", "streamsreflowing:stream_leaf",
                 "streamsreflowing:stream_stick" -> true;
            default -> false;
        };
        if (!oldStreamId) return false;
        try {
            return Float.parseFloat(rawValue) == 0.30f;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Built-in compatibility defaults and the stock Particular leaf presets. */
    public static boolean isDefault(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id != null && (DEFAULTS.containsKey(id)
            || ("particular".equals(id.getNamespace()) && id.getPath().endsWith("_leaf")));
    }

    /** Only show optional built-ins when their particle type is actually installed. */
    public static List<String> installedDefaults() {
        List<String> result = new ArrayList<>();
        for (ResourceLocation id : DEFAULTS.keySet()) {
            if (BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) result.add(id.toString());
        }
        result.sort(String::compareTo);
        return result;
    }

    public static float defaultValue(String id) {
        Float value = DEFAULTS.get(ResourceLocation.tryParse(id));
        return value == null ? 1.0f : value;
    }

    private BuiltinParticleResponseMultipliers() {}
}
