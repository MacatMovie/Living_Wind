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
        // Biome ambience: let genuinely airborne spores, steam and soft natural
        // particles drift without overpowering their original movement patterns.
        // Falling-leaf particle types are intentionally left at their normal wind
        // response; Living Wind's existing leaf compatibility handles them.
        Map.entry(ResourceLocation.tryParse("biomesoplenty:steam"), 0.65f),
        Map.entry(ResourceLocation.tryParse("biomesoplenty:end_spore"), 0.70f),
        Map.entry(ResourceLocation.tryParse("biomesoplenty:falling_blood"), 0.35f),
        Map.entry(ResourceLocation.tryParse("biomesoplenty:pus"), 0.45f),
        Map.entry(ResourceLocation.tryParse("biomesoplenty:binary"), 0.55f),
        // Iron's Spells 'n Spellbooks: affect ambient-looking elemental debris,
        // not spell trajectories. Most are reduced because they are spawned by
        // casts and should stay visually close to the intended effect.
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:blood"), 0.20f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:wisp"), 0.40f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:snowflake"), 0.70f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:dragon_fire"), 0.35f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:fire"), 0.35f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:embers"), 0.65f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:spell"), 0.35f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:acid"), 0.20f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:snow_dust"), 0.60f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:fog"), 0.25f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:spark"), 0.35f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:emberous_ash"), 0.35f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:fiery_smoke"), 0.30f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:falling_sparkle"), 0.60f),
        Map.entry(ResourceLocation.tryParse("irons_spellbooks:soul_fire"), 0.35f),
        // Cataclysm: a small amount of wind for free-floating cloud/dust/fire,
        // without altering boss telegraphs or their precise trails.
        Map.entry(ResourceLocation.tryParse("cataclysm:spark"), 0.40f),
        Map.entry(ResourceLocation.tryParse("cataclysm:soul_lava"), 0.50f),
        Map.entry(ResourceLocation.tryParse("cataclysm:rain_cloud"), 0.25f),
        Map.entry(ResourceLocation.tryParse("cataclysm:rain_fog"), 0.25f),
        Map.entry(ResourceLocation.tryParse("cataclysm:custom_poof"), 0.35f),
        Map.entry(ResourceLocation.tryParse("cataclysm:cursed_flame"), 0.35f),
        Map.entry(ResourceLocation.tryParse("cataclysm:small_cursed_flame"), 0.35f),
        Map.entry(ResourceLocation.tryParse("cataclysm:dust_blast"), 0.25f),
        Map.entry(ResourceLocation.tryParse("cataclysm:dust_pillar"), 0.30f),
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

    /** All known presets, including those for mods not installed in this instance. */
    public static List<String> allDefaults() {
        List<String> result = new ArrayList<>();
        for (ResourceLocation id : DEFAULTS.keySet()) result.add(id.toString());
        result.sort(String::compareTo);
        return result;
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
