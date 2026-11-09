package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.compat.ValkyrienSkiesWindBridge;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingWind.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerWindEvents {
    private ServerWindEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        WindSavedData data = WindSavedData.get(level);
        if (!WindDimensionBlacklist.contains(level)) data.tick(level);
        ValkyrienSkiesWindBridge.tick(level);

        int interval = WindConfig.SYNC_INTERVAL_TICKS.get();
        if (interval > 0 && level.getGameTime() % interval == 0L && !level.players().isEmpty()) {
            WindNetworking.sendToLevel(level, WindApi.get(level));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WindSavedData data = WindSavedData.get(level);
            WindNetworking.sendToPlayer(player, WindApi.get(level));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WindSavedData data = WindSavedData.get(level);
            WindNetworking.sendToPlayer(player, WindApi.get(level));
        }
    }
}
