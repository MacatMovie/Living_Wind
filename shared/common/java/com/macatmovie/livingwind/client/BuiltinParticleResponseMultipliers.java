package com.macatmovie.livingwind.client;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Built-in movement multipliers for optional mods. Keeps new compatibility tuning active
 * in existing user configs without overwriting their explicit response entries.
 */
public final class BuiltinParticleResponseMultipliers {
    public static final Map<ResourceLocation, Float> DEFAULTS = Map.of(
        ResourceLocation.tryParse("flying_stuff:cloud"), 0.25f,
        ResourceLocation.tryParse("flying_stuff:cloud_smoke"), 0.25f,
        ResourceLocation.tryParse("flying_stuff:cloud_smoke_1"), 0.25f,
        ResourceLocation.tryParse("flying_stuff:cloud_barrier_2"), 0.25f,
        ResourceLocation.tryParse("flying_stuff:cloud_barrier_3"), 0.25f,
        // Vanilla bursts: preserve the original animation but permit subtle
        // wind movement where the effect is not a fixed player-facing signal.
        ResourceLocation.tryParse("minecraft:firework"), 0.25f,
        ResourceLocation.tryParse("minecraft:poof"), 0.35f,
        ResourceLocation.tryParse("minecraft:cloud"), 0.35f
    );

    private BuiltinParticleResponseMultipliers() {}
}
