package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class ServerWindEvents {
    private ServerWindEvents() {}

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        WindSavedData data = WindSavedData.get(level);
        data.tick(level);

        int interval = WindConfig.SYNC_INTERVAL_TICKS.get();
        if (interval > 0 && level.getGameTime() % interval == 0L && !level.players().isEmpty()) {
            WindNetworking.sendToLevel(level, data.snapshot(level.getGameTime()));
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WindSavedData data = WindSavedData.get(level);
            WindNetworking.sendToPlayer(player, data.snapshot(level.getGameTime()));
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WindSavedData data = WindSavedData.get(level);
            WindNetworking.sendToPlayer(player, data.snapshot(level.getGameTime()));
        }
    }
}
