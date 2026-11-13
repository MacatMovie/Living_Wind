package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.compat.ValkyrienSkiesWindBridge;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerWindEvents {
    private ServerWindEvents() {}
    public static void onLevelTick(ServerLevel level) {
        WindSavedData data = WindSavedData.get(level);
        if (!WindDimensionBlacklist.contains(level)) data.tick(level);
        // Server-thread update only: VS owns the actual physics simulation.
        ValkyrienSkiesWindBridge.tick(level);
        int interval = WindConfig.SYNC_INTERVAL_TICKS.get();
        if (interval > 0 && level.getGameTime() % interval == 0L && !level.players().isEmpty())
            WindNetworking.sendToLevel(level, WindApi.get(level));
    }
    public static void onPlayerLogin(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WindSavedData.get(level);
        WindNetworking.sendToPlayer(player, WindApi.get(level));
    }
}
