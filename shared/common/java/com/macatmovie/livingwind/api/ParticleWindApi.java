package com.macatmovie.livingwind.api;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;

/**
 * Optional client compatibility API for particles which must not be displaced by Living Wind.
 *
 * Mods may register a particle type id when their particles are created through the normal
 * ParticleEngine path, or register their Particle implementation class for custom/direct-added
 * particles which do not retain a registry id. Registration is cheap and should normally happen
 * once during client setup.
 *
 * Data-only integrations can instead add particle ids to the particle-type tag
 * {@code livingwind:wind_immune}.
 */
public final class ParticleWindApi {
    private static final Set<ResourceLocation> IMMUNE_IDS = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> IMMUNE_CLASSES = ConcurrentHashMap.newKeySet();

    private ParticleWindApi() {}

    public static void registerImmune(ResourceLocation particleId) {
        if (particleId != null) IMMUNE_IDS.add(particleId);
    }

    public static void registerImmune(String particleId) {
        ResourceLocation id = ResourceLocation.tryParse(particleId);
        if (id != null) registerImmune(id);
    }

    public static void registerImmuneParticleClass(Class<?> particleClass) {
        if (particleClass != null && Particle.class.isAssignableFrom(particleClass)) {
            IMMUNE_CLASSES.add(particleClass);
        }
    }

    public static boolean isRuntimeImmune(ResourceLocation particleId) {
        return particleId != null && IMMUNE_IDS.contains(particleId);
    }

    public static boolean isRuntimeImmune(Particle particle) {
        if (particle == null) return false;
        Class<?> actual = particle.getClass();
        for (Class<?> registered : IMMUNE_CLASSES) {
            if (registered.isAssignableFrom(actual)) return true;
        }
        return false;
    }
}
