package com.macatmovie.livingwind;

import com.macatmovie.livingwind.client.CosyCrittersCompat;
import com.macatmovie.livingwind.client.WindFoliageColorCache;
import com.macatmovie.livingwind.client.gui.LivingWindConfigScreen;
import com.macatmovie.livingwind.client.particle.AmbientPixelParticle;
import com.macatmovie.livingwind.client.particle.WindStreakParticle;
import com.macatmovie.livingwind.client.particle.WindLeafParticle;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.client.vane.WindVaneRenderer;
import com.macatmovie.livingwind.client.windmill.WindmillRenderer;
import net.minecraft.client.particle.SpriteSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LivingWind.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LivingWindClient {
    private LivingWindClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new LivingWindConfigScreen(parent))
            );
            CosyCrittersCompat.register();
        });
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> WindFoliageColorCache.clear());
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WIND_VANE.get(), WindVaneRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WINDMILL.get(), WindmillRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.AMBIENT_PIXEL.get(), AmbientPixelParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WIND_STREAK.get(), WindStreakParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WIND_LEAF.get(), WindLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WIND_NEEDLES.get(), WindLeafParticle.NeedlesProvider::new);
        event.registerSpriteSet(ModParticles.WIND_GRASS.get(), WindLeafParticle.GrassProvider::new);
    }
}
