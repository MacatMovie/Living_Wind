package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, LivingWind.MOD_ID);

    public static final RegistryObject<SimpleParticleType> AMBIENT_PIXEL =
        PARTICLES.register("ambient_pixel", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WIND_LEAF =
        PARTICLES.register("wind_leaf", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WIND_NEEDLES =
        PARTICLES.register("wind_needles", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WIND_GRASS =
        PARTICLES.register("wind_grass", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WIND_STREAK =
        PARTICLES.register("wind_streak", () -> new SimpleParticleType(false));

    private ModParticles() {}

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
