package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * The Beyond 1.0.x: the End normally stays calm, but Lustrous Echoes has a faint
 * breeze and the mod's End thunder (Migration Storm) can bring strong gusts.
 * This is SERVER-AUTHORITATIVE and does not alter The Beyond's weather system.
 * NeoForge registers availability; all other loader targets leave it disabled.
 */
public final class BeyondEndWindCompat {
    public static final ResourceKey<Biome> LUSTROUS_ECHOES = ResourceKey.create(
        Registries.BIOME, ResourceLocation.tryParse("the_beyond:lustrous_echoes"));

    private static volatile boolean installedAndEnabled;
    private static volatile float biomeWindMultiplier = 1.0f;
    private static volatile float stormWindMultiplier = 1.0f;

    private BeyondEndWindCompat() {}

    public static void configure(boolean available, float biomeMultiplier, float stormMultiplier) {
        biomeWindMultiplier = Mth.clamp(biomeMultiplier, 0.0f, 3.0f);
        stormWindMultiplier = Mth.clamp(stormMultiplier, 0.0f, 3.0f);
        installedAndEnabled = available;
    }

    /** The user's explicit dimension-mode entry/blacklist always wins. */
    public static boolean scopedEnd(ServerLevel level) {
        return installedAndEnabled && level.dimension() == Level.END
            && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.DISABLED
            && !WindDimensionSettings.hasExplicitEndRule();
    }

    /** Return a separate snapshot (never mutate the saved dimension atmosphere). */
    public static WindSnapshot at(ServerLevel level, BlockPos pos, WindSnapshot raw) {
        if (!scopedEnd(level)) return raw;
        float base = Math.max(0.0f, raw.strength());
        // Weak local wind even on naturally calm days. Source biome key matches
        // The Beyond's 1.0.x registry, not the colloquial 'Lustrous Expanse'.
        boolean breezyBiome = level.getBiome(pos).is(LUSTROUS_ECHOES);
        float breeze = breezyBiome ? Math.max(0.12f, Math.min(0.55f, base * 0.60f))
            * biomeWindMultiplier : 0.0f;
        // The Beyond makes End weather observable through rain/thunder fade data;
        // the Migration Storm advancement specifically checks End thunder.
        float stormProgress = Mth.clamp(level.getThunderLevel(1.0f), 0.0f, 1.0f);
        float storm = Math.max(0.85f, base) * stormWindMultiplier;
        float strength = Mth.lerp(stormProgress, breeze, Math.max(breeze, storm));
        return withStrength(raw, Mth.clamp(strength, 0.0f, 2.5f));
    }

    /** Dimension-wide consumers are calm in clear weather; in a storm they get storm wind. */
    public static WindSnapshot global(ServerLevel level, WindSnapshot raw) {
        if (!scopedEnd(level)) return raw;
        float stormProgress = Mth.clamp(level.getThunderLevel(1.0f), 0.0f, 1.0f);
        if (stormProgress <= 0.001f) return WindDimensionBlacklist.calm(level.getGameTime());
        float storm = Math.max(0.85f, Math.max(0.0f, raw.strength()))
            * stormWindMultiplier;
        return withStrength(raw, Mth.clamp(storm * stormProgress, 0.0f, 2.5f));
    }

    private static WindSnapshot withStrength(WindSnapshot wind, float strength) {
        return new WindSnapshot(strength, wind.directionDegrees(), wind.turbulence(),
            wind.directionInstability(), strength <= 0.0f ? 0.0f : wind.gustStrength(),
            wind.windyPeriod(), wind.stormFront(), wind.gameTime());
    }
}
