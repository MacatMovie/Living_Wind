package com.macatmovie.livingwind;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.macatmovie.livingwind.registry.ModItems;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.server.ServerWindCommands;
import com.macatmovie.livingwind.server.ServerWindEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(LivingWind.MOD_ID)
public final class LivingWind {
    public static final String MOD_ID = "livingwind";

    public LivingWind(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, WindConfig.SPEC, "livingwind-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientWindConfig.SPEC, "livingwind-client.toml");
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        modBus.addListener(com.macatmovie.livingwind.registry.ModCreativeTabs::onBuildCreativeTab);
        ModParticles.register(modBus);
        modBus.addListener(WindNetworking::register);
        NeoForge.EVENT_BUS.addListener(ServerWindCommands::register);
        NeoForge.EVENT_BUS.addListener(ServerWindEvents::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ServerWindEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(ServerWindEvents::onPlayerChangedDimension);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
