package com.macatmovie.livingwind.compat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/** Sable's NeoForge-only sublevel hooks are deliberately disabled in this Fabric baseline. */
public final class SableShelterCompat {
    private SableShelterCompat() {}
    public static float playerExposure(ClientLevel level, Player player, float vanilla, float effectiveness) {
        return vanilla;
    }
}
