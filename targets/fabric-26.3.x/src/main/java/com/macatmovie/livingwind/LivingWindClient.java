package com.macatmovie.livingwind;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.client.ClientWindEvents;
import com.macatmovie.livingwind.client.CreateWindmillTooltip;
import com.macatmovie.livingwind.client.WindDebugHud;
import com.macatmovie.livingwind.client.WindFoliageColorCache;
import com.macatmovie.livingwind.client.gui.FabricSettingsScreen;
import com.macatmovie.livingwind.client.particle.AmbientPixelParticle;
import com.macatmovie.livingwind.client.particle.WindLeafParticle;
import com.macatmovie.livingwind.client.particle.WindStreakParticle;
import com.macatmovie.livingwind.client.vane.WindVaneRenderer;
import com.macatmovie.livingwind.client.windmill.WindmillRenderer;
import com.macatmovie.livingwind.network.ClientWindNetworking;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.macatmovie.livingwind.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.platform.InputConstants;

public final class LivingWindClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        // Client-only settings must never be loaded during dedicated-server initialization.
        ClientWindConfig.SPEC.load("livingwind-client-fabric.json");
        ClientWindNetworking.register();
        // Modern block models infer alpha masking from the textures.
        var particles = ParticleProviderRegistry.getInstance();
        particles.register(ModParticles.AMBIENT_PIXEL.get(), AmbientPixelParticle.Provider::new);
        particles.register(ModParticles.WIND_STREAK.get(), WindStreakParticle.Provider::new);
        particles.register(ModParticles.WIND_LEAF.get(), WindLeafParticle.Provider::new);
        particles.register(ModParticles.WIND_NEEDLES.get(), WindLeafParticle.NeedlesProvider::new);
        particles.register(ModParticles.WIND_GRASS.get(), WindLeafParticle.GrassProvider::new);
        particles.register(ModParticles.WIND_PETAL.get(), WindLeafParticle.PetalProvider::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.WIND_VANE.get(), WindVaneRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.WINDMILL.get(), WindmillRenderer::new);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LivingWind.id("wind_debug"), WindDebugHud::render);
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> CreateWindmillTooltip.onTooltip(stack, lines));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return LivingWind.id("foliage_color_cache"); }
            @Override public void onResourceManagerReload(ResourceManager manager) { WindFoliageColorCache.clear(); }
        });
        KeyMapping settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.livingwind.open_settings", InputConstants.KEY_F8, KeyMapping.Category.register(LivingWind.id("settings"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // FWA installs its SwingDrivers during client setup. Register after all
            // Fabric client entrypoints have run, without a hard dependency.
            com.macatmovie.livingwind.client.compat.FwaSwingDriverCompat.register();
            ClientWindEvents.onClientTick();
            while (settingsKey.consumeClick()) client.gui.setScreen(new FabricSettingsScreen(client.gui.screen()));
        });
    }
}
