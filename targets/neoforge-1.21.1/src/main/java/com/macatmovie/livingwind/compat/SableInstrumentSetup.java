package com.macatmovie.livingwind.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Sable is optional. Resolve Sable API classes only after checking its mod id,
 * and never let a changed Sable version prevent Living Wind from starting. */
public final class SableInstrumentSetup {
    private static final Logger LOG = LogUtils.getLogger();
    private SableInstrumentSetup() {}

    public static void server() { install("com.macatmovie.livingwind.compat.SableServerInstrumentResolver"); }
    public static void client() { install("com.macatmovie.livingwind.compat.SableClientInstrumentResolver"); }

    private static void install(String className) {
        if (!ModList.get().isLoaded("sable")) return;
        try {
            Class.forName(className).getMethod("install").invoke(null);
            LOG.info("Living Wind instruments: Sable moving-contraption coordinates enabled ({})", className);
        } catch (ReflectiveOperationException | LinkageError problem) {
            LOG.warn("Living Wind instruments: Sable spatial adapter unavailable; ordinary instrument behavior retained", problem);
        }
    }
}
