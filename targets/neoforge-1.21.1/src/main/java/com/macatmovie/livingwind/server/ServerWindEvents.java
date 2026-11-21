package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class ServerWindEvents {
    private ServerWindEvents() {}

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Set availability before dimension mode lookup. Never changes other
        // loader targets, and never treats The Beyond as a hard dependency.
        refreshBeyondSettings();
        WindSavedData data = WindSavedData.get(level);
        if (!WindDimensionBlacklist.contains(level)) data.tick(level);
        // Update the optional external wind source after our authoritative tick.
        com.macatmovie.livingwind.compat.CreateAtmosphericsWindCompat.serverTick(level);

        int interval = WindConfig.SYNC_INTERVAL_TICKS.get();
        if (interval > 0 && level.getGameTime() % interval == 0L && !level.players().isEmpty()) {
            WindNetworking.sendToLevel(level, WindApi.get(level));
        }
    }

    private static void refreshBeyondSettings() {
        com.macatmovie.livingwind.wind.BeyondEndWindCompat.configure(
            net.neoforged.fml.ModList.get().isLoaded("the_beyond")
                && WindConfig.BEYOND_END_WIND_ENABLED.get(),
            WindConfig.BEYOND_BIOME_WIND_MULTIPLIER.get().floatValue(),
            WindConfig.BEYOND_STORM_WIND_MULTIPLIER.get().floatValue());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refreshBeyondSettings();
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WindSavedData data = WindSavedData.get(level);
            WindNetworking.sendToPlayer(player, WindApi.get(level));
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refreshBeyondSettings();
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WindSavedData data = WindSavedData.get(level);
            WindNetworking.sendToPlayer(player, WindApi.get(level));
        }
    }
}
