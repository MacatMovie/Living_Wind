package com.macatmovie.livingwind.server;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Classloader boundary for the optional Sable physics API. Keep Sable imports out
 * of this class and the ordinary Living Wind startup path: Forge 1.20.1 and
 * NeoForge installations without Sable must not resolve physics classes.
 */
public final class SableWindIntegration {
    private static final Logger LOG = LogUtils.getLogger();

    private SableWindIntegration() {}

    public static void start(IEventBus modBus) {
        if (!ModList.get().isLoaded("sable")) return;
        // Transmission & Linkage rotors use their bearing's physics tick, not
        // the generic sail/airship force path. Cache wind on the server tick
        // even with AeroWeather present; no class is loaded unless required.
        if (ModList.get().isLoaded("aeronautics_utility_objects")) {
            try {
                Class<?> drive = Class.forName("com.macatmovie.livingwind.server.AeronauticsKineticBearingWind");
                drive.getMethod("start").invoke(null);
                LOG.info("Living Wind: Transmission & Linkage rotor wind drive enabled.");
            } catch (ReflectiveOperationException | LinkageError e) {
                LOG.error("Living Wind: could not initialize optional kinetic conversion bearing wind drive", e);
            }
        }
        // AeroWeather already drives Sable bodies with its own wind forces.
        // Avoid applying two independently computed wind fields to one ship.
        if (ModList.get().isLoaded("aeroweather")) {
            LOG.info("Living Wind: AeroWeather detected; leaving Sable contraption forces to AeroWeather.");
            return;
        }
        try {
            Class<?> handler = Class.forName("com.macatmovie.livingwind.server.SableWindForceHandler");
            handler.getMethod("start", IEventBus.class).invoke(null, modBus);
            LOG.info("Living Wind: Sable wind physics integration enabled (wind-catching contraptions only).");
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.error("Living Wind: could not initialize optional Sable wind physics integration", e);
        }
    }
}
