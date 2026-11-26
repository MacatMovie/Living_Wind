package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
    public static final RegistryHandle<SimpleParticleType> AMBIENT_PIXEL = new RegistryHandle<>();
    public static final RegistryHandle<SimpleParticleType> WIND_STREAK = new RegistryHandle<>();
    public static final RegistryHandle<SimpleParticleType> WIND_LEAF = new RegistryHandle<>();
    public static final RegistryHandle<SimpleParticleType> WIND_NEEDLES = new RegistryHandle<>();
    public static final RegistryHandle<SimpleParticleType> WIND_GRASS = new RegistryHandle<>();
    public static final RegistryHandle<SimpleParticleType> WIND_PETAL = new RegistryHandle<>();
    private ModParticles() {}
    public static void register() {
        AMBIENT_PIXEL.set(Registry.register(BuiltInRegistries.PARTICLE_TYPE, LivingWind.id("ambient_pixel"), FabricParticleTypes.simple(false)));
        WIND_STREAK.set(Registry.register(BuiltInRegistries.PARTICLE_TYPE, LivingWind.id("wind_streak"), FabricParticleTypes.simple(false)));
        WIND_LEAF.set(Registry.register(BuiltInRegistries.PARTICLE_TYPE, LivingWind.id("wind_leaf"), FabricParticleTypes.simple(false)));
        WIND_NEEDLES.set(Registry.register(BuiltInRegistries.PARTICLE_TYPE, LivingWind.id("wind_needles"), FabricParticleTypes.simple(false)));
        WIND_GRASS.set(Registry.register(BuiltInRegistries.PARTICLE_TYPE, LivingWind.id("wind_grass"), FabricParticleTypes.simple(false)));
        WIND_PETAL.set(Registry.register(BuiltInRegistries.PARTICLE_TYPE, LivingWind.id("wind_petal"), FabricParticleTypes.simple(false)));
    }
}
