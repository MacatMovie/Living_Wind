package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.api.ParticleWindApi;

/**
 * Soft compatibility for Cosy Critters.
 *
 * Cosy Critters can add some critter particles directly to the particle engine,
 * especially on Forge 1.20.1, so those instances may not retain a usable
 * ParticleType registry id. Registering the common CritterParticle base class
 * makes birds, moths, spiders and Hat Man immune without taking a hard dependency.
 */
public final class CosyCrittersCompat {
    private static final String CRITTER_PARTICLE_CLASS = "pigcart.cosycritters.particle.CritterParticle";

    private CosyCrittersCompat() {}

    public static void register() {
        try {
            ClassLoader loader = CosyCrittersCompat.class.getClassLoader();
            Class<?> critterParticle = Class.forName(CRITTER_PARTICLE_CLASS, false, loader);
            ParticleWindApi.registerImmuneParticleClass(critterParticle);
        } catch (ClassNotFoundException | LinkageError ignored) {
            // Cosy Critters is optional.
        }
    }
}
