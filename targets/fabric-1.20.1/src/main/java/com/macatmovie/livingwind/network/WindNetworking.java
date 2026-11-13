package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** 1.20.1 still uses channel/buffer packets, not 1.21.1 CustomPacketPayload. */
public final class WindNetworking {
    public static final ResourceLocation CHANNEL = LivingWind.id("wind_sync_v2");
    private WindNetworking() {}
    public static void register() { /* No payload type registry on Minecraft 1.20.1. */ }
    public static void sendToLevel(ServerLevel level, WindSnapshot snapshot) {
        for (ServerPlayer player : level.players()) sendToPlayer(player, snapshot);
    }
    public static void sendToPlayer(ServerPlayer player, WindSnapshot snapshot) {
        boolean blocked = WindDimensionBlacklist.contains(player.serverLevel());
        WindSnapshot effective = blocked ? WindDimensionBlacklist.calm(player.serverLevel().getGameTime()) : snapshot;
        if (!ServerPlayNetworking.canSend(player, CHANNEL)) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        WindSyncPayload.from(effective, blocked, player.serverLevel().dimension().location()).encode(buf);
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }
}
