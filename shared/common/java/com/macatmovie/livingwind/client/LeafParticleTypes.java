package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves the SOURCE leaf block (not a flying particle) to its preferred sprite family.
 * Priority: user overrides > compatible datapack tags > built-in species > broadleaf.
 * Nothing is copied from Vanilla Backport; these are just vanilla-compatible tag identifiers.
 */
public final class LeafParticleTypes {
    public enum Type { BROADLEAF, CONIFER }

    private static final TagKey<Block> NEEDLE_TAG = TagKey.create(Registries.BLOCK,
            ResourceLocation.tryParse("minecraft:spawn_falling_needles"));
    private static final TagKey<Block> BROADLEAF_TAG = TagKey.create(Registries.BLOCK,
            ResourceLocation.tryParse("minecraft:spawn_falling_leaves"));

    // Species commonly present in Biomes We've Gone, Regions Unexplored,
    // Nature's Spirit, and other modded forests. Unlike an exhaustive per-mod
    // registry, these names also pick up new tree mods without a source dependency.
    private static final Set<String> NEEDLE_SPECIES = Set.of(
            "spruce", "pine", "fir", "cypress", "cedar", "redwood", "sequoia",
            "hemlock", "juniper", "larch", "yew", "douglas", "araucaria", "conifer",
            "podocarpus", "cryptomeria", "hinoki", "balsam", "norway_spruce");

    private static List<? extends String> lastEntries = List.of();
    private static Map<ResourceLocation, Type> overrides = Map.of();

    private LeafParticleTypes() {}

    /** Accept exact registered block ids, e.g. 'minecraft:spruce_leaves=CONIFER'. */
    public static boolean validEntry(Object value) {
        if (!(value instanceof String raw)) return false;
        int eq = raw.lastIndexOf('=');
        if (eq < 1 || eq >= raw.length() - 1) return false;
        ResourceLocation id = ResourceLocation.tryParse(raw.substring(0, eq).trim());
        if (id == null) return false;
        String type = raw.substring(eq + 1).trim().toUpperCase(Locale.ROOT);
        return type.equals("CONIFER") || type.equals("BROADLEAF");
    }

    private static synchronized Map<ResourceLocation, Type> custom() {
        List<? extends String> current = ClientWindConfig.LEAF_TYPE_OVERRIDES.get();
        if (!current.equals(lastEntries)) {
            Map<ResourceLocation, Type> next = new HashMap<>();
            for (String raw : current) {
                if (!validEntry(raw)) continue;
                int eq = raw.lastIndexOf('=');
                ResourceLocation id = ResourceLocation.tryParse(raw.substring(0, eq).trim());
                next.put(id, Type.valueOf(raw.substring(eq + 1).trim().toUpperCase(Locale.ROOT)));
            }
            overrides = Map.copyOf(next);
            lastEntries = List.copyOf(current);
        }
        return overrides;
    }

    /** Explicit user entry, for leaves blocks lacking the conventional #minecraft:leaves tag. */
    public static boolean hasOverride(BlockState state) {
        return custom().containsKey(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static Type resolve(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        Type forced = custom().get(id);
        return forced != null ? forced : resolveAutomatic(state);
    }

    /** Resolve tags and species defaults without saved/user overrides.
     * Used by the editor to preview the actual Auto choice even before Save.
     */
    public static Type resolveAutomatic(BlockState state) {
        if (state.is(NEEDLE_TAG)) return Type.CONIFER;
        if (state.is(BROADLEAF_TAG)) return Type.BROADLEAF;
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        for (String species : NEEDLE_SPECIES) {
            // Token-aware matching avoids treating unrelated blocks like 'pineapple_leaves' as pine.
            if (name.equals(species) || name.startsWith(species + "_")
                    || name.endsWith("_" + species) || name.contains("_" + species + "_"))
                return Type.CONIFER;
        }
        return Type.BROADLEAF;
    }
}
