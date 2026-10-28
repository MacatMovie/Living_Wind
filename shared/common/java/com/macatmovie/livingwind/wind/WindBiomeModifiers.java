package com.macatmovie.livingwind.wind;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Built-in positional wind effects. The dimension's atmospheric direction and events stay
 * untouched; only the effective strength changes. No optional mod classes are referenced.
 */
public final class WindBiomeModifiers {
    public static final ResourceKey<Biome> BLACK_ICE_GLACIERS = ResourceKey.create(
        Registries.BIOME, ResourceLocation.tryParse("netherexp:black_ice_glaciers"));

    private WindBiomeModifiers() {}

    /** A continuous floor in the glaciers, with extra force during existing gusts. */
    public static float apply(float atmosphericStrength, float glacierWeight) {
        float weight = Math.max(0.0f, Math.min(1.0f, glacierWeight));
        if (weight == 0.0f || !Float.isFinite(atmosphericStrength)) return atmosphericStrength;
        float glacierStrength = Math.max(1.35f, Math.min(2.65f, atmosphericStrength * 1.65f));
        return atmosphericStrength + (glacierStrength - atmosphericStrength) * weight;
    }
}
