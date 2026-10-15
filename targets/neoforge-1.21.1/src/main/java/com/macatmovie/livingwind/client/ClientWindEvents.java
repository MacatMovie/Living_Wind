package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.client.compat.FwaWindMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientWindEvents {
    private static int tickCounter;
    // Advances only while the game is running. Paused singleplayer must not age
    // shelter measurements or evict indoor smoke positions while particles freeze.
    private static int shelterTickCounter;
    private static float smoothedExposure = 1.0f;

    private ClientWindEvents() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            WindAudioManager.reset();
            WindLeafSpawner.reset();
            ClientWindState.reset();
            WindShelter.clear();
            ParticleWaterProtection.clear();
            FwaWindMotion.reset();
            tickCounter = 0;
            shelterTickCounter = 0;
            smoothedExposure = 1.0f;
            return;
        }

        ClientWindState.tick();
        ClientWindState.updateAltitude(player.getBlockY(), level.dimension() == net.minecraft.world.level.Level.OVERWORLD);
        tickCounter++;
        if (tickCounter % 20 == 1) ParticleWindFilter.refresh();
        WindShelter.configure(ClientWindConfig.REDUCE_WIND_INDOORS.get(),
            ClientWindConfig.SHELTER_EFFECTIVENESS.get().floatValue(), ClientWindConfig.SHELTER_LOW_COST.get());
        WindShelter.setObserverPosition(player.getX(), player.getY(), player.getZ());
        // Minecraft client tick events can continue during singleplayer ESC pause.
        // Freeze the shelter clock and stop cache cleanup until particles resume.
        if (!mc.isPaused()) {
            WindShelter.tick(level, ++shelterTickCounter);
        }
        ParticleWaterProtection.tick(level, tickCounter);

        if (!mc.isPaused() && ClientWindConfig.REDUCE_WIND_INDOORS.get()
                && shelterTickCounter % 5 == 0) {
            float target = WindShelter.playerExposure(level, player.blockPosition().above());
            smoothedExposure += (target - smoothedExposure) * 0.30f;
            ClientWindState.setExposure(smoothedExposure);
        } else if (!ClientWindConfig.REDUCE_WIND_INDOORS.get()) {
            smoothedExposure = 1.0f;
            ClientWindState.setExposure(1.0f);
        }

        if (!mc.isPaused()) {
            AmbientWindParticles.tick(level, player);
            WindLeafSpawner.tick(level, player);
            WindAudioManager.tick(level, player);
        }
    }

}
