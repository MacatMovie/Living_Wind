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
import com.macatmovie.livingwind.client.windmill.WindmillItemRenderer;
import com.macatmovie.livingwind.network.ClientWindNetworking;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.macatmovie.livingwind.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;

public final class LivingWindClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        // Client-only settings must never be loaded during dedicated-server initialization.
        ClientWindConfig.SPEC.load("livingwind-client-fabric.json");
        ClientWindNetworking.register();
        // Fabric does not use Forge/NeoForge's model JSON render_type hint.
        // Explicitly render the static, alpha-masked weather instruments as cutout.
        // The animated block entity parts use their own renderers.
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(),
            ModBlocks.WIND_VANE.get(),
            ModBlocks.BIRD_WIND_VANE.get(),
            ModBlocks.REDSTONE_WIND_VANE.get(),
            ModBlocks.WINDMILL.get(),
            ModBlocks.REDSTONE_WINDMILL.get());
        var particles = ParticleFactoryRegistry.getInstance();
        particles.register(ModParticles.AMBIENT_PIXEL.get(), AmbientPixelParticle.Provider::new);
        particles.register(ModParticles.WIND_STREAK.get(), WindStreakParticle.Provider::new);
        particles.register(ModParticles.WIND_LEAF.get(), WindLeafParticle.Provider::new);
        particles.register(ModParticles.WIND_NEEDLES.get(), WindLeafParticle.NeedlesProvider::new);
        particles.register(ModParticles.WIND_GRASS.get(), WindLeafParticle.GrassProvider::new);
        particles.register(ModParticles.WIND_PETAL.get(), WindLeafParticle.PetalProvider::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.WIND_VANE.get(), WindVaneRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.WINDMILL.get(), WindmillRenderer::new);
        WindmillItemRenderer itemRenderer = new WindmillItemRenderer();
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.WINDMILL.get(), itemRenderer::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.REDSTONE_WINDMILL.get(), itemRenderer::renderByItem);
        HudRenderCallback.EVENT.register(WindDebugHud::render);
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> CreateWindmillTooltip.onTooltip(stack, lines));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return LivingWind.id("foliage_color_cache"); }
            @Override public void onResourceManagerReload(ResourceManager manager) { WindFoliageColorCache.clear(); }
        });
        KeyMapping settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.livingwind.open_settings", GLFW.GLFW_KEY_F8, "key.categories.livingwind"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // FWA installs its SwingDrivers during client setup. Register after all
            // Fabric client entrypoints have run, without a hard dependency.
            com.macatmovie.livingwind.client.compat.FwaSwingDriverCompat.register();
            ClientWindEvents.onClientTick();
            while (settingsKey.consumeClick()) client.setScreen(new FabricSettingsScreen(client.screen));
        });
    }
}
