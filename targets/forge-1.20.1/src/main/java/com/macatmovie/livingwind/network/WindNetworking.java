package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WindNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(LivingWind.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static boolean initialized;

    private WindNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        CHANNEL.registerMessage(
            0,
            WindSyncPayload.class,
            (msg, buf) -> msg.encode(buf),
            WindSyncPayload::decode,
            WindNetworking::handle
        );
    }

    private static void handle(WindSyncPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientWindState.accept(payload));
        context.setPacketHandled(true);
    }

    private static WindSnapshot withLocalWeather(ServerPlayer player, WindSnapshot snapshot) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("simpleclouds")) {
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
        sendToPlayer(player, WindSyncPayload.from(withLocalWeather(player, snapshot)));
    }

    private static void sendToPlayer(ServerPlayer player, WindSyncPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
