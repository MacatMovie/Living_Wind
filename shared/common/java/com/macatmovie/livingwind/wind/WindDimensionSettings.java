package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.config.WindConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Per-dimension atmosphere. Lookup is cached whenever the config list changes. */
public final class WindDimensionSettings {
    public enum Mode { NORMAL, REDUCED_GUSTY, VERY_WEAK, DISABLED }

    private static volatile List<? extends String> lastEntries;
    private static volatile Map<String, Mode> lastModes = Map.of();

    private WindDimensionSettings() {}

    /** Each entry is a fully qualified dimension ID followed by '=' and a mode name. */
    public static boolean validEntry(Object candidate) {
        if (!(candidate instanceof String s)) return false;
        int separator = s.lastIndexOf('=');
        if (separator <= 0 || separator == s.length() - 1) return false;
        if (!WindDimensionBlacklist.validId(s.substring(0, separator))) return false;
        try {
            Mode.valueOf(s.substring(separator + 1).trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static Map<String, Mode> overrides() {
        List<? extends String> entries = WindConfig.DIMENSION_MODES.get();
        if (entries == lastEntries) return lastModes;
        synchronized (WindDimensionSettings.class) {
            if (entries != lastEntries) {
                Map<String, Mode> parsed = new HashMap<>();
                for (String entry : entries) {
                    if (!validEntry(entry)) continue;
                    int split = entry.lastIndexOf('=');
                    parsed.put(entry.substring(0, split).trim().toLowerCase(Locale.ROOT),
                        Mode.valueOf(entry.substring(split + 1).trim().toUpperCase(Locale.ROOT)));
                }
                lastModes = Map.copyOf(parsed);
                lastEntries = entries;
            }
            return lastModes;
        }
    }

    /** Old Nether/End modes and blacklist remain valid for existing configs without migration. */
    public static Mode mode(ServerLevel level) {
        String id = level.dimension().location().toString();
        Mode defined = overrides().get(id);
        if (defined != null) return defined;
        for (String ignored : WindConfig.DIMENSION_BLACKLIST.get()) {
            if (id.equals(ignored.trim().toLowerCase(Locale.ROOT))) return Mode.DISABLED;
        }
        if (level.dimension() == Level.NETHER) {
            return switch (WindConfig.NETHER_WIND_MODE.get()) {
                case DISABLED -> Mode.DISABLED;
                case REDUCED_GUSTY -> Mode.REDUCED_GUSTY;
                case NORMAL -> Mode.NORMAL;
            };
        }
        if (level.dimension() == Level.END) {
            return switch (WindConfig.END_WIND_MODE.get()) {
                case DISABLED -> Mode.DISABLED;
                case VERY_WEAK -> Mode.VERY_WEAK;
                case NORMAL -> Mode.NORMAL;
            };
        }
        return WindConfig.DEFAULT_DIMENSION_MODE.get();
    }

    public static Mode legacyNetherMode() {
        return switch (WindConfig.NETHER_WIND_MODE.get()) {
            case DISABLED -> Mode.DISABLED;
            case REDUCED_GUSTY -> Mode.REDUCED_GUSTY;
            case NORMAL -> Mode.NORMAL;
        };
    }
    public static Mode legacyEndMode() {
        return switch (WindConfig.END_WIND_MODE.get()) {
            case DISABLED -> Mode.DISABLED;
            case VERY_WEAK -> Mode.VERY_WEAK;
            case NORMAL -> Mode.NORMAL;
        };
    }
}
