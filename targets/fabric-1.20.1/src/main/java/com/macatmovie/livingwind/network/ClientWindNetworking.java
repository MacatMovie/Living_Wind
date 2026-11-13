package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.ClientWindEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Fabric-only dimension-aware wind sync. Play packets can be received before
 * the new ClientLevel exists during initial login or dimension travel. Keep a
 * single short-lived snapshot rather than applying another dimension's wind.
 * All mutable state here is confined to the Minecraft main client thread.
 */
public final class ClientWindNetworking {
    private static WindSyncPayload pending;
    private static int pendingTicks;
    private ClientWindNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WindNetworking.CHANNEL, (client, handler, buf, sender) -> {
            WindSyncPayload payload = WindSyncPayload.decode(buf);
            client.execute(() -> receive(payload));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            clearPending();
            ClientWindEvents.resetForDisconnect();
        }));
    }

    private static void receive(WindSyncPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && Minecraft.getInstance().player != null
                && level.dimension().location().equals(payload.dimensionId())) {
            pending = null;
            pendingTicks = 0;
            apply(level, payload);
        } else {
            // Only hold the newest snapshot. This keeps memory usage bounded and
            // also lets a later correct-dimension packet supersede a stale one.
            pending = payload;
            pendingTicks = 0;
        }
    }

    /** Called after the world-reset check in ClientWindEvents' client tick. */
    public static void applyPending(ClientLevel level) {
        if (pending == null) return;
        if (level.dimension().location().equals(pending.dimensionId())) {
            WindSyncPayload ready = pending;
            pending = null;
            pendingTicks = 0;
            apply(level, ready);
        } else if (++pendingTicks > 200) {
            // Never carry snapshots from old dimensions indefinitely.
            clearPending();
        }
    }

    public static void clearPending() {
        pending = null;
        pendingTicks = 0;
    }

    private static void apply(ClientLevel level, WindSyncPayload payload) {
        ClientWindEvents.prepareWorld(level);
        ClientWindState.accept(payload);
        ClientWindState.setAlekiShipsWindEnabled(payload.alekiShipsWindEnabled());
        ClientWindState.setAlekiShipsWindScale(payload.alekiShipsWindScale());
    }
}
