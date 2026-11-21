package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindSyncPayload;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindMath;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Create: Atmospherics (mod ID aeronauticswinds, build 2.0.0).
 * Uses its PUBLIC WindsSim.Simulation#setWindOverrideVec method, not a mixin.
 * This wind vector is in m/s. The mod retains its weather, wave, sail and
 * physics calculations, and samples our wind through its normal WindField.
 *
 * The override is a volatile value in Atmospherics. We update it exclusively
 * on the main game/client tick, never on the Sable physics worker. If another
 * mod or an Atmospherics command owns the override, we leave it untouched.
 */
public final class CreateAtmosphericsWindCompat {
    private static final Logger LOG = LoggerFactory.getLogger("LivingWind/CreateAtmospherics");
    // LW strength 0.5 ~= the native Atmospherics baseline of 6 m/s.
    private static final double METRES_PER_SECOND = 12.0;
    private static final double SPEED_LIMIT = 55.0;
    private static final Map<Level, Owned> OWNED = new WeakHashMap<>();
    private static volatile boolean clientEnabled;
    private static volatile double clientMultiplier = 1.0;
    private static volatile Access access;
    private static boolean failed;

    private record Access(Method getOrNull, Field windField, Method getOverride,
                          Method setOverrideVec, Method clearOverride) {}
    private record Owned(Object field, Object override) {}

    private CreateAtmosphericsWindCompat() {}

    private static boolean installed() {
        return ModList.get().isLoaded("aeronauticswinds");
    }

    /** Server-authoritative config; the client doesn't read its local common TOML. */
    public static void onWindSync(WindSyncPayload payload) {
        clientEnabled = payload.createAtmosphericsWindEnabled() && !payload.windDisabled();
        clientMultiplier = payload.createAtmosphericsWindMultiplier();
    }

    public static void serverTick(ServerLevel level) {
        if (!installed()) return;
        boolean enabled = WindConfig.CREATE_ATMOSPHERICS_WIND_ENABLED.get()
            && !WindDimensionBlacklist.contains(level);
        if (!enabled) {
            update(level, false, 0.0, 0.0);
            return;
        }
        WindSnapshot wind = WindApi.get(level);
        double scale = WindConfig.CREATE_ATMOSPHERICS_WIND_MULTIPLIER.get() * METRES_PER_SECOND;
        update(level, true,
            WindMath.windX(wind.strength(), wind.directionDegrees()) * scale,
            WindMath.windZ(wind.strength(), wind.directionDegrees()) * scale);
    }

    public static void clientTick(Level level) {
        if (!installed()) return;
        if (!clientEnabled || !ClientWindState.hasServerState() || ClientWindState.dimensionWindDisabled()) {
            update(level, false, 0.0, 0.0);
            return;
        }
        double scale = clientMultiplier * METRES_PER_SECOND;
        update(level, true, ClientWindState.globalWindX() * scale,
            ClientWindState.globalWindZ() * scale);
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && installed()) {
            update(level, false, 0.0, 0.0);
            OWNED.remove(level);
        }
    }

    /**
     * Only clear an override we actually installed. A user /wind override or
     * another mod's override takes precedence until they remove it.
     */
    private static void update(Level level, boolean enabled, double x, double z) {
        Access h = access();
        if (h == null) return;
        try {
            Object simulation = h.getOrNull.invoke(null, level);
            if (simulation == null) {
                OWNED.remove(level);
                return;
            }
            Object field = h.windField.get(simulation);
            Object current = h.getOverride.invoke(field);
            Owned previous = OWNED.get(level);
            boolean ours = previous != null && previous.field == field && previous.override == current;
            if (!ours && previous != null) OWNED.remove(level);
            if (!enabled) {
                if (ours) h.clearOverride.invoke(simulation);
                OWNED.remove(level);
                return;
            }
            if (current != null && !ours) return; // Another controller has priority.
            if (!Double.isFinite(x) || !Double.isFinite(z)) return;
            double speed = Math.hypot(x, z);
            if (speed > SPEED_LIMIT) {
                double factor = SPEED_LIMIT / speed;
                x *= factor;
                z *= factor;
            }
            // Keep the native wind override strictly horizontal. Atmospherics
            // can still simulate its independent temperature/weather mechanics.
            h.setOverrideVec.invoke(simulation, x, 0.0, z);
            OWNED.put(level, new Owned(field, h.getOverride.invoke(field)));
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
            fail("Failed to feed Living Wind into Create: Atmospherics; reverting to native wind.", e);
        }
    }

    private static Access access() {
        if (failed || !installed()) return null;
        if (access != null) return access;
        try {
            Class<?> sim = Class.forName("com.enzonic.aeronauticswinds.WindsSim");
            Class<?> simulation = Class.forName("com.enzonic.aeronauticswinds.WindsSim$Simulation");
            Class<?> field = Class.forName("com.enzonic.aeronauticswinds.wind.WindField");
            access = new Access(sim.getMethod("getOrNull", Level.class), simulation.getField("wind"),
                field.getMethod("getOverride"),
                simulation.getMethod("setWindOverrideVec", double.class, double.class, double.class),
                simulation.getMethod("clearWindOverride"));
            return access;
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            fail("Create: Atmospherics wind API changed; optional integration disabled.", e);
            return null;
        }
    }

    private static void fail(String message, Throwable error) {
        if (!failed) LOG.warn(message, error);
        failed = true;
    }
}
