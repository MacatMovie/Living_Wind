package com.macatmovie.livingwind.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** Streams Reflowing directly adds some waterfall particles to ParticleEngine,
 * bypassing the standard registered-ID capture path. Resolve ONLY the five known
 * particle classes, with no linkage to the optional mod's code or classes.
 */
public final class StreamsReflowingParticleIds {
    private static final String FORGE = "dev.streamsreflowing.forge.particle.";
    private static final String NEOFORGE = "dev.streamsreflowing.neoforge.particle.";
    private static final ClassValue<Identifier> IDS = new ClassValue<>() {
        @Override protected Identifier computeValue(Class<?> type) {
            String name = type.getName();
            String suffix;
            if (name.startsWith(FORGE)) suffix = name.substring(FORGE.length());
            else if (name.startsWith(NEOFORGE)) suffix = name.substring(NEOFORGE.length());
            else return Identifier.tryParse("minecraft:empty");
            return switch (suffix) {
                case "StreamBubbleParticle" -> Identifier.tryParse("streamsreflowing:stream_bubble");
                case "StreamFoamParticle" -> Identifier.tryParse("streamsreflowing:stream_foam");
                case "StreamSplashParticle" -> Identifier.tryParse("streamsreflowing:stream_splash");
                case "StreamLeafParticle" -> Identifier.tryParse("streamsreflowing:stream_leaf");
                case "StreamStickParticle" -> Identifier.tryParse("streamsreflowing:stream_stick");
                default -> Identifier.tryParse("minecraft:empty");
            };
        }
    };

    private StreamsReflowingParticleIds() {}

    @Nullable public static Identifier resolve(@Nullable Identifier id, @Nullable Particle particle) {
        if (id != null || particle == null) return id;
        Identifier guessed = IDS.get(particle.getClass());
        return "minecraft".equals(guessed.getNamespace()) && "empty".equals(guessed.getPath()) ? null : guessed;
    }
}
