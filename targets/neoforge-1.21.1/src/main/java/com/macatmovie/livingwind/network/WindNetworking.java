package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WindNetworking {
    private WindNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("6");
        registrar.playToClient(
            WindSyncPayload.TYPE,
            WindSyncPayload.STREAM_CODEC,
            (payload, context) -> {
                ClientWindState.accept(payload);
                ClientWindState.setHighSeasWindEnabled(payload.highSeasWindEnabled());
            }
        );
    }

    private static WindSnapshot withLocalWeather(ServerPlayer player, WindSnapshot snapshot) {
        if (net.neoforged.fml.ModList.get().isLoaded("simpleclouds")) {
            return com.macatmovie.livingwind.compat.SimpleCloudsWindCompat.atPlayer(player, snapshot);
        }
        return snapshot;
    }

    public static void sendToLevel(ServerLevel level, WindSnapshot snapshot) {
        for (ServerPlayer player : level.players()) {
            sendToPlayer(player, snapshot);
        }
    }

    public static void sendToPlayer(ServerPlayer player, WindSnapshot snapshot) {
        boolean blocked = WindDimensionBlacklist.contains(player.serverLevel());
        WindSnapshot effective = blocked ? WindDimensionBlacklist.calm(player.serverLevel().getGameTime()) : withLocalWeather(player, snapshot);
        PacketDistributor.sendToPlayer(player, WindSyncPayload.from(effective, blocked));
    }
}
