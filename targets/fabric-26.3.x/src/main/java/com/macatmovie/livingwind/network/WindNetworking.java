package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.wind.WindSnapshot;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WindNetworking {
    private WindNetworking() {}
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(WindSyncPayload.TYPE, WindSyncPayload.STREAM_CODEC);
    }
    public static void sendToLevel(ServerLevel level, WindSnapshot snapshot) {
        for (ServerPlayer player : level.players()) sendToPlayer(player, snapshot);
    }
    public static void sendToPlayer(ServerPlayer player, WindSnapshot snapshot) {
        boolean blocked = WindDimensionBlacklist.contains(player.level());
        WindSnapshot effective = blocked ? WindDimensionBlacklist.calm(player.level().getGameTime()) : snapshot;
        if (ServerPlayNetworking.canSend(player, WindSyncPayload.TYPE))
            ServerPlayNetworking.send(player, WindSyncPayload.from(effective, blocked, player.level().dimension().identifier()));
    }
}
