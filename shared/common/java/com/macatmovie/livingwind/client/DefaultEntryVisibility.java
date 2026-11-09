package com.macatmovie.livingwind.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** UI-only filter for built-in presets. Never changes which defaults are active in gameplay. */
public final class DefaultEntryVisibility {
    private static boolean installedOnly = true;
    private DefaultEntryVisibility() {}

    public static boolean installedOnly() { return installedOnly; }
    public static void toggle() { installedOnly = !installedOnly; }

    public static boolean particleInstalled(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id != null && BuiltInRegistries.PARTICLE_TYPE.containsKey(id);
    }

    public static boolean blockInstalled(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id != null && BuiltInRegistries.BLOCK.containsKey(id);
    }

    public static boolean entityInstalled(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }
}
