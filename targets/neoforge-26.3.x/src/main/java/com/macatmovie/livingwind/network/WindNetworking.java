package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.BeyondEndWindCompat;
import com.macatmovie.livingwind.api.WindApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WindNetworking {
    private WindNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("7");
        registrar.playToClient(
            WindSyncPayload.TYPE,
            WindSyncPayload.STREAM_CODEC,
            (payload, context) -> {
                ClientWindState.accept(payload);
                ClientWindState.setHighSeasWindEnabled(payload.highSeasWindEnabled());
                com.macatmovie.livingwind.compat.CreateAtmosphericsWindCompat.onWindSync(payload);
            }
        );
    }

    private static WindSnapshot withLocalWeather(ServerPlayer player, WindSnapshot snapshot) {
        return snapshot;
    }

    public static void sendToLevel(ServerLevel level, WindSnapshot snapshot) {
        for (ServerPlayer player : level.players()) {
            sendToPlayer(player, snapshot);
        }
    }

    public static void sendToPlayer(ServerPlayer player, WindSnapshot snapshot) {
        boolean blocked = WindDimensionBlacklist.contains(player.level());
        // The Beyond's clear-weather breeze is biome-scoped. Send each player
        // their OWN position-aware wind rather than broadcasting the same
        // dimension-wide snapshot (which is calm outside End storms).
        WindSnapshot effective = blocked ? WindDimensionBlacklist.calm(player.level().getGameTime())
            : BeyondEndWindCompat.scopedEnd(player.level())
                ? WindApi.getAt(player.level(), player.blockPosition())
                : withLocalWeather(player, snapshot);
        PacketDistributor.sendToPlayer(player, WindSyncPayload.from(effective, blocked));
    }
}
