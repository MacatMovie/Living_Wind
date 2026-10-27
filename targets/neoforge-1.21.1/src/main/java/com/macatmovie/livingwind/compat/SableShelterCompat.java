package com.macatmovie.livingwind.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Optional Sable-only adapter. Does not load any Sable classes unless Sable is installed.
 * The player shelter survey runs every five active client ticks, never per particle. */
public final class SableShelterCompat {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean SABLE_PRESENT = ModList.get().isLoaded("sable");
    private static boolean failed;

    private SableShelterCompat() {}

    public static float playerExposure(ClientLevel level, Player player, float vanillaExposure, float effectiveness) {
        if (!SABLE_PRESENT || failed || effectiveness <= 0.0f) return vanillaExposure;
        try {
            return SableShelterDetector.playerExposure(level, player.getEyePosition(), vanillaExposure, effectiveness);
        } catch (LinkageError | RuntimeException problem) {
            failed = true; // Avoid log spam / repeated expensive failures on changed Sable builds.
            LOG.warn("Living Wind: Sable sub-level shelter support unavailable; using normal shelter checks", problem);
            return vanillaExposure;
        }
    }
}
