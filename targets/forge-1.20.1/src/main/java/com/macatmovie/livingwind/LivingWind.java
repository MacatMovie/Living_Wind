package com.macatmovie.livingwind;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.macatmovie.livingwind.registry.ModItems;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LivingWind.MOD_ID)
public final class LivingWind {
    public static final String MOD_ID = "livingwind";

    public LivingWind() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WindConfig.SPEC, "livingwind-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientWindConfig.SPEC, "livingwind-client.toml");
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModParticles.register(modBus);
        WindNetworking.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
