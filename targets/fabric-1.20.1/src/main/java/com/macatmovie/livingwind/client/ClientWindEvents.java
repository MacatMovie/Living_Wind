package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.network.ClientWindNetworking;
import com.macatmovie.livingwind.client.compat.FwaWindMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

public final class ClientWindEvents {
    private static int tickCounter;
    // Advances only while the game is running. Paused singleplayer must not age
    // shelter measurements or evict indoor smoke positions while particles freeze.
    private static int shelterTickCounter;
    private static float smoothedExposure = 1.0f;
    // A dimension transfer may never pass through a null level. Guard against
    // applying the previous world's wind to the new world until its sync arrives.
    private static ClientLevel previousLevel;

    private ClientWindEvents() {}

    /** Also called before a received wind packet is applied, because the first
     * play packet can arrive before the first tick of the replacement world. */
    public static void prepareWorld(ClientLevel level) {
        if (level == null || level == previousLevel) return;
        previousLevel = level;
        ClientWindState.reset();
        WindAudioManager.reset();
        WindLeafSpawner.reset();
        WindStreakSpawner.reset();
        ClientBiomeWindCache.reset();
        WindShelter.clear();
        ParticleWaterProtection.clear();
        // Remove per-world spring/history from the optional animation drivers.
        FwaWindMotion.reset();
        com.macatmovie.livingwind.client.compat.GliderWindCompat.reset();
        tickCounter = 0;
        shelterTickCounter = 0;
        smoothedExposure = 1.0f;
    }

    /** Clear per-world client data as soon as a play connection closes. This
     * is also idempotently called from client ticks with no active world; no
     * old wind bed or sync state can leak into the next server's menu/load. */
    public static void resetForDisconnect() {
        WindAudioManager.reset();
        WindLeafSpawner.reset();
        WindStreakSpawner.reset();
        ClientWindState.reset();
        ClientBiomeWindCache.reset();
        WindShelter.clear();
        ParticleWaterProtection.clear();
        FwaWindMotion.reset();
        com.macatmovie.livingwind.client.compat.GliderWindCompat.reset();
        previousLevel = null;
        tickCounter = 0;
        shelterTickCounter = 0;
        smoothedExposure = 1.0f;
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            resetForDisconnect();
            return;
        }

        prepareWorld(level);
        ClientWindNetworking.applyPending(level);

        if (ClientWindState.dimensionWindDisabled()) {
            WindAudioManager.reset();
            WindLeafSpawner.reset();
            WindStreakSpawner.reset();
            ClientWindState.setBiomeWeight(0.0f);
            ClientBiomeWindCache.reset();
            ClientWindState.tick();
            return;
        }

        ClientBiomeWindCache.tick(level, player, tickCounter);
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
            // Gliders use their own optional wind influence, without double-applying Entity Wind.
            if (!com.macatmovie.livingwind.client.compat.GliderWindCompat.tickClientPlayer(player)) {
                com.macatmovie.livingwind.wind.EntityWindMotion.tickClientPlayer(player);
            }
            if (player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat boat
                    && boat.getControllingPassenger() == player) {
                com.macatmovie.livingwind.wind.EntityWindMotion.tickClientBoat(boat);
            }
            AmbientWindParticles.tick(level, player);
            WindStreakSpawner.tick(level, player);
            WindLeafSpawner.tick(level, player);
            WindAudioManager.tick(level, player);
        }
    }

}
