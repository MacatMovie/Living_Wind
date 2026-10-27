package com.macatmovie.livingwind.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Streams Reflowing directly adds some waterfall particles to ParticleEngine,
 * bypassing the standard registered-ID capture path. Resolve ONLY the five known
 * particle classes, with no linkage to the optional mod's code or classes.
 */
public final class StreamsReflowingParticleIds {
    private static final String FORGE = "dev.streamsreflowing.forge.particle.";
    private static final String NEOFORGE = "dev.streamsreflowing.neoforge.particle.";
    private static final ClassValue<ResourceLocation> IDS = new ClassValue<>() {
        @Override protected ResourceLocation computeValue(Class<?> type) {
            String name = type.getName();
            String suffix;
            if (name.startsWith(FORGE)) suffix = name.substring(FORGE.length());
            else if (name.startsWith(NEOFORGE)) suffix = name.substring(NEOFORGE.length());
            else return ResourceLocation.tryParse("minecraft:empty");
            return switch (suffix) {
                case "StreamBubbleParticle" -> ResourceLocation.tryParse("streamsreflowing:stream_bubble");
                case "StreamFoamParticle" -> ResourceLocation.tryParse("streamsreflowing:stream_foam");
                case "StreamSplashParticle" -> ResourceLocation.tryParse("streamsreflowing:stream_splash");
                case "StreamLeafParticle" -> ResourceLocation.tryParse("streamsreflowing:stream_leaf");
                case "StreamStickParticle" -> ResourceLocation.tryParse("streamsreflowing:stream_stick");
                default -> ResourceLocation.tryParse("minecraft:empty");
            };
        }
    };

    private StreamsReflowingParticleIds() {}

    @Nullable public static ResourceLocation resolve(@Nullable ResourceLocation id, @Nullable Particle particle) {
        if (id != null || particle == null) return id;
        ResourceLocation guessed = IDS.get(particle.getClass());
        return "minecraft".equals(guessed.getNamespace()) && "empty".equals(guessed.getPath()) ? null : guessed;
    }
}
