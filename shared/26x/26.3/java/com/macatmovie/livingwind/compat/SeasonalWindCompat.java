package com.macatmovie.livingwind.compat;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Optional, version-shared seasonal state reader for Serene Seasons and Ecliptic Seasons.
 * Both published APIs are stable on Forge 1.20.1 and NeoForge 1.21.1. No hard
 * linkage to either mod is required (important on installs without a season mod).
 * All reflective discovery is one-time; reads are cached for 40 game ticks.
 */
public final class SeasonalWindCompat {
    public enum Season { SPRING, SUMMER, AUTUMN, WINTER, NONE }
    public record Snapshot(Season season, float progress, boolean ecliptic) {
        public boolean active() { return season != Season.NONE; }
    }
    private record Cached(long tick, Snapshot value) {}
    private static final Snapshot NONE = new Snapshot(Season.NONE, 0.0f, false);
    private static final Map<Level, Cached> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final int REFRESH_TICKS = 40;
    private static boolean discovered;
    private static Method esInstance, esEnabled, esSolarTerm, esTermDays, esDayInTerm, esSnowy;
    private static Method ssGetState, ssGetCycleTicks, ssGetCycleDuration;

    private SeasonalWindCompat() {}

    private static synchronized void discover() {
        if (discovered) return;
        discovered = true;
        try {
            Class<?> es = Class.forName("com.teamtea.eclipticseasons.api.EclipticSeasonsApi");
            esInstance = es.getMethod("getInstance");
            esEnabled = es.getMethod("isSeasonEnabled", Level.class);
            esSolarTerm = es.getMethod("getSolarTerm", Level.class);
            esTermDays = es.getMethod("getLastingDaysOfEachTerm", Level.class);
            esDayInTerm = es.getMethod("getDayInTerm", Level.class);
            esSnowy = es.getMethod("isSnowyBlock", Level.class, BlockState.class, BlockPos.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            esInstance = null;
        }
        try {
            Class<?> ss = Class.forName("sereneseasons.api.season.SeasonHelper");
            Class<?> state = Class.forName("sereneseasons.api.season.ISeasonState");
            ssGetState = ss.getMethod("getSeasonState", Level.class);
            ssGetCycleTicks = state.getMethod("getSeasonCycleTicks");
            ssGetCycleDuration = state.getMethod("getCycleDuration");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            ssGetState = null;
        }
    }

    public static Snapshot snapshot(Level level) {
        if (level == null) return NONE;
        long time = level.getGameTime();
        Cached cached = CACHE.get(level);
        if (cached != null && time >= cached.tick() && time - cached.tick() < REFRESH_TICKS)
            return cached.value();
        if (!discovered) discover();
        Snapshot read = read(level);
        CACHE.put(level, new Cached(time, read));
        return read;
    }

    private static Snapshot read(Level level) {
        // Prefer Ecliptic when both mods are present. An installed but disabled Ecliptic
        // leaves the way clear for Serene if that provider actually has season data.
        if (esInstance != null) try {
            Object api = esInstance.invoke(null);
            if (api != null && Boolean.TRUE.equals(esEnabled.invoke(api, level))) {
                Object solar = esSolarTerm.invoke(api, level);
                if (solar instanceof Enum<?> term && term.ordinal() < 24) {
                    int days = Math.max(1, ((Number) esTermDays.invoke(api, level)).intValue());
                    int day = Mth.clamp(((Number) esDayInTerm.invoke(api, level)).intValue(), 0, days - 1);
                    float year = (term.ordinal() + (day + 0.5f) / days) / 24.0f;
                    return new Snapshot(Season.values()[term.ordinal() / 6], year, true);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
        // Serene Seasons only runs its normal seasonal cycle in the Overworld.
        if (ssGetState != null && level.dimension() == Level.OVERWORLD) try {
            Object state = ssGetState.invoke(null, level);
            if (state != null) {
                int duration = ((Number) ssGetCycleDuration.invoke(state)).intValue();
                int ticks = ((Number) ssGetCycleTicks.invoke(state)).intValue();
                if (duration > 0) {
                    float year = Math.floorMod(ticks, duration) / (float) duration;
                    return new Snapshot(Season.values()[Math.min(3, (int)(year * 4.0f))], year, false);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
        return NONE;
    }

    /** Ecliptic's seasonal snow layer may be visual-only rather than a snow block. */
    public static boolean snowCoveredAt(Level level, BlockState state, BlockPos pos) {
        if (esSnowy == null) return false;
        try {
            Object api = esInstance.invoke(null);
            return api != null && Boolean.TRUE.equals(esSnowy.invoke(api, level, state, pos));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    // Smoothly interpolate four seasonal profiles and across year boundaries.
    // Season positions refer to their centers (spring, summer, autumn, winter).
    private static float seasonalValue(Snapshot state, float spring, float summer,
                                       float autumn, float winter) {
        if (!state.active()) return 1.0f;
        float phase = state.progress() * 4.0f - 0.5f;
        int i = Mth.floor(phase);
        float t = phase - i;
        t = t * t * (3.0f - 2.0f * t);
        float before = switch (Math.floorMod(i, 4)) {
            case 0 -> spring;
            case 1 -> summer;
            case 2 -> autumn;
            default -> winter;
        };
        float after = switch (Math.floorMod(i + 1, 4)) {
            case 0 -> spring;
            case 1 -> summer;
            case 2 -> autumn;
            default -> winter;
        };
        return Mth.lerp(t, before, after);
    }

    private static float intensity(float natural, double userIntensity) {
        return Mth.clamp((float) (1.0 + (natural - 1.0f) * userIntensity), 0.2f, 3.0f);
    }

    public static float backgroundMultiplier(Snapshot s, double intensity) {
        return intensity(seasonalValue(s, 1.12f, 0.80f, 1.38f, 1.10f), intensity);
    }
    public static float gustFrequencyMultiplier(Snapshot s, double intensity) {
        return intensity(seasonalValue(s, 1.27f, 0.78f, 1.60f, 1.27f), intensity);
    }
    public static float gustIntensityMultiplier(Snapshot s, double intensity) {
        return intensity(seasonalValue(s, 1.12f, 0.88f, 1.25f, 1.23f), intensity);
    }

    /** Emission profiles use existing fixed budgets, never extra source searches. */
    public static float particleMultiplier(Snapshot s, String kind, double userIntensity) {
        if (!s.active()) return 1.0f;
        float natural = switch (kind) {
            case "canopy" -> seasonalValue(s, 0.85f, 0.60f, 2.00f, 0.27f);
            case "petal" -> seasonalValue(s, 1.85f, 1.10f, 0.54f, 0.18f);
            case "grass" -> seasonalValue(s, 1.35f, 1.45f, 0.90f, 0.26f);
            case "snow" -> seasonalValue(s, 0.55f, 0.20f, 0.75f, 2.65f);
            default -> 1.0f;
        };
        // Ecliptic already has seasonal falling leaves. Keep autumn clearly windier
        // without doubling a second full foliage shower over the one it adds.
        if (s.ecliptic() && "canopy".equals(kind) && natural > 1.0f)
            natural = 1.0f + (natural - 1.0f) * 0.68f;
        return intensity(natural, userIntensity);
    }
}
