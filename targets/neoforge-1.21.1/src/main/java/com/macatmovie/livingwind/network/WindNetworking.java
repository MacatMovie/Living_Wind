package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WindNetworking {
    private WindNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
            WindSyncPayload.TYPE,
            WindSyncPayload.STREAM_CODEC,
            (payload, context) -> {
                ClientWindState.accept(payload);
                ClientWindState.setHighSeasWindEnabled(payload.highSeasWindEnabled());
            }
        );
    }

    public static void sendToLevel(ServerLevel level, WindSnapshot snapshot) {
        WindSyncPayload payload = WindSyncPayload.from(snapshot);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendToPlayer(ServerPlayer player, WindSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, WindSyncPayload.from(snapshot));
    }
}
