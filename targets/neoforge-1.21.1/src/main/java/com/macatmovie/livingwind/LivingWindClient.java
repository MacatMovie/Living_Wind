package com.macatmovie.livingwind;

import com.macatmovie.livingwind.client.ClientWindEvents;
import com.macatmovie.livingwind.client.CreateWindmillTooltip;
import com.macatmovie.livingwind.client.CosyCrittersCompat;
import com.macatmovie.livingwind.client.WindFoliageColorCache;
import com.macatmovie.livingwind.client.WindDebugHud;
import com.macatmovie.livingwind.client.gui.LivingWindConfigurationSectionScreen;
import com.macatmovie.livingwind.client.particle.AmbientPixelParticle;
import com.macatmovie.livingwind.client.particle.WindStreakParticle;
import com.macatmovie.livingwind.client.particle.WindLeafParticle;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.client.vane.WindVaneRenderer;
import com.macatmovie.livingwind.client.windmill.WindmillRenderer;
import com.macatmovie.livingwind.client.windmill.WindmillItemRenderer;
import com.macatmovie.livingwind.registry.ModItems;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LivingWind.MOD_ID, dist = Dist.CLIENT)
public final class LivingWindClient {
    public LivingWindClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (ignoredContainer, parent) ->
            new ConfigurationScreen(container, parent, (configScreen, type, modConfig, title) ->
                new LivingWindConfigurationSectionScreen(configScreen, type, modConfig, title)
            )
        );
        NeoForge.EVENT_BUS.addListener(ClientWindEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(CreateWindmillTooltip::onTooltip);
        modBus.addListener(LivingWindClient::registerParticleProviders);
        modBus.addListener(LivingWindClient::registerBlockEntityRenderers);
        modBus.addListener(LivingWindClient::registerClientExtensions);
        modBus.addListener(LivingWindClient::registerClientReloadListeners);
        modBus.addListener(LivingWindClient::registerGuiLayers);
        CosyCrittersCompat.register();
        com.macatmovie.livingwind.compat.SableInstrumentSetup.client();
    }

    private static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> WindFoliageColorCache.clear());
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.AMBIENT_PIXEL.get(), AmbientPixelParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WIND_STREAK.get(), WindStreakParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WIND_LEAF.get(), WindLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WIND_NEEDLES.get(), WindLeafParticle.NeedlesProvider::new);
        event.registerSpriteSet(ModParticles.WIND_GRASS.get(), WindLeafParticle.GrassProvider::new);
    }

    private static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WIND_VANE.get(), WindVaneRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WINDMILL.get(), WindmillRenderer::new);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // One shared custom renderer for both block-items, installed only on physical clients.
        event.registerItem(new IClientItemExtensions() {
            private final WindmillItemRenderer renderer = new WindmillItemRenderer();
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, ModItems.WINDMILL.get(), ModItems.REDSTONE_WINDMILL.get());
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LivingWind.id("debug_hud"), WindDebugHud::render);
    }
}
