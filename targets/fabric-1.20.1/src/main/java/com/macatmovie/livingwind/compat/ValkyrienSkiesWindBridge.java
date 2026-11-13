package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindBiomeModifiers;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindMath;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Fabric 1.20.1 Valkyrien Skies 2 integration, shared by Eureka and Clockwork.
 * Clockwork PropellerController obtains VsiPhysShip.dragController.getWindVector()
 * on the physics thread; the named VS wind source below feeds that existing path.
 * No Clockwork mixin or duplicate force injection is required.
 *
 * VS owns all drag, lift and propulsion forces. We provide only an additional named
 * atmospheric wind source to its per-ship drag controller, just like its Weather2
 * compatibility. This never applies physics forces or scans assembled ship blocks.
 *
 * All VS classes/methods are resolved only when the mod is present. Reflection is
 * cached, so we do not add a compile-time or runtime dependency on VS/Kotlin/VSCore.
 * Invoke from the SERVER level tick; never invoke on VS's physics worker threads.
 */
public final class ValkyrienSkiesWindBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LivingWind/ValkyrienSkies");
    private static final String PROVIDER = "LIVINGWIND";
    // No references to ships or their levels are retained between world unloads.
    private static final Map<ServerLevel, Map<Long, SmoothedWind>> STATES = new WeakHashMap<>();

    private static boolean checkedPresence;
    private static boolean installed;
    private static boolean warnedFailure;
    private static boolean unavailable;
    private static boolean loggedActive;
    private static Method getShipWorld;
    private static Method getLoadedShips;
    private static Method getShipId;
    private static Method getShipDimension;
    private static Method getShipToWorld;
    private static Method getInertiaData;
    private static Method getCenterOfMass;
    private static Method getDragController;
    private static Method setWindDirection;
    private static Method setWindSpeed;

    private ValkyrienSkiesWindBridge() {}

    public static void tick(ServerLevel level) {
        if (unavailable || !isInstalled()) return;
        if (level.getGameTime() % WindConfig.VS_WIND_UPDATE_TICKS.get() != 0L) return;

        boolean enabled = WindConfig.VS_WIND_ENABLED.get()
            && !WindDimensionBlacklist.contains(level)
            && (WindConfig.VS_WIND_WITH_WEATHER2.get() || !FabricLoader.getInstance().isModLoaded("weather2"));
        // In the common disabled/Weather2 case there is nothing to scan unless we
        // have to clear wind already supplied to ships earlier in this session.
        if (!enabled && !STATES.containsKey(level)) return;

        try {
            Object world = shipWorld(level);
            if (world == null) return; // VS can initialize its ship world after the level.
            if (getLoadedShips == null) getLoadedShips = world.getClass().getMethod("getLoadedShips");
            Object loaded = getLoadedShips.invoke(world);
            if (!(loaded instanceof Iterable<?> ships)) return;

            Map<Long, SmoothedWind> levelStates = STATES.computeIfAbsent(level, ignored -> new HashMap<>());
            Set<Long> seen = new HashSet<>();
            String dimensionId = "minecraft:dimension:" + level.dimension().location();
            WindSnapshot atmosphere = enabled ? WindApi.get(level) : null;
            double conversion = WindConfig.VS_WIND_SPEED_SCALE.get();
            double maximumSpeed = WindConfig.VS_WIND_MAX_SPEED.get();
            boolean wroteWind = false;

            for (Object ship : ships) {
                if (ship == null) continue;
                resolveShipMethods(ship);
                if (!dimensionId.equals(getShipDimension.invoke(ship))) continue;

                long id = ((Number) getShipId.invoke(ship)).longValue();
                seen.add(id);
                Object drag = getDragController.invoke(ship);
                if (drag == null) continue; // Some ships/older VS builds have no drag controller.
                resolveWindMethods(drag);

                if (!enabled) {
                    // Drop our named wind, but never touch the Weather2 or other providers.
                    if (levelStates.remove(id) != null) writeWind(drag, new Vector3d(0, 0, 1), 0.0);
                    continue;
                }

                // VS ship coordinates are stored in distant shipyard chunks. Transform
                // the actual center of mass to world coordinates BEFORE sampling wind.
                Object inertia = getInertiaData.invoke(ship);
                if (inertia == null) continue;
                Vector3dc com = (Vector3dc) getCenterOfMass.invoke(inertia);
                Matrix4dc transform = (Matrix4dc) getShipToWorld.invoke(ship);
                if (com == null || transform == null) continue;
                Vector3d pos = transform.transformPosition(new Vector3d(com));
                if (!Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) continue;

                // Avoid chunk loads when ships travel fast or hover above unloaded terrain.
                // Base atmosphere plus altitude is enough if local biome data is absent.
                int x = Mth.floor(pos.x), z = Mth.floor(pos.z);
                int y = Mth.floor(pos.y);
                float strength = atmosphere.strength() * WindApi.altitudeMultiplier(level, y);
                if (level.dimension() == Level.NETHER && level.hasChunk(x >> 4, z >> 4)) {
                    BlockPos sample = new BlockPos(x, y, z);
                    if (level.getBiome(sample).is(WindBiomeModifiers.BLACK_ICE_GLACIERS)) {
                        strength = WindBiomeModifiers.apply(strength, 1.0f);
                    }
                }
                double speed = Mth.clamp(Math.max(0.0, strength) * conversion, 0.0, maximumSpeed);
                float bearing = atmosphere.directionDegrees();
                double targetX = WindMath.windX(1.0f, bearing) * speed;
                double targetZ = WindMath.windZ(1.0f, bearing) * speed;

                SmoothedWind wind = levelStates.computeIfAbsent(id, ignored -> new SmoothedWind());
                // Smooth actual velocity components, not degrees (handles wraparound and
                // gusts without suddenly flipping ship wind direction).
                double blend = 1.0 - Math.exp(-WindConfig.VS_WIND_UPDATE_TICKS.get() / 12.0);
                if (!wind.initialized) {
                    wind.x = targetX;
                    wind.z = targetZ;
                    wind.initialized = true;
                } else {
                    wind.x += (targetX - wind.x) * blend;
                    wind.z += (targetZ - wind.z) * blend;
                }
                double smoothedSpeed = Math.min(maximumSpeed, Math.hypot(wind.x, wind.z));
                Vector3d direction = smoothedSpeed > 1.0e-5
                    ? new Vector3d(wind.x / smoothedSpeed, 0.0, wind.z / smoothedSpeed)
                    : new Vector3d(WindMath.windX(1.0f, bearing), 0.0, WindMath.windZ(1.0f, bearing));
                writeWind(drag, direction, smoothedSpeed);
                wroteWind = true;
            }

            // Prevent retained state for destroyed/unloaded ships. VS owns the
            // controllers and their lifecycle, so no reference to them is held here.
            levelStates.keySet().removeIf(id -> !seen.contains(id));
            if (levelStates.isEmpty()) STATES.remove(level);
            if (wroteWind && !loggedActive) {
                loggedActive = true;
                LOG.info("Valkyrien Skies wind source initialized; Clockwork detected: {} (propellers consume the VS wind vector).",
                    FabricLoader.getInstance().isModLoaded("vs_clockwork"));
            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException error) {
            unavailable = true;
            STATES.clear();
            if (!warnedFailure) {
                warnedFailure = true;
                LOG.warn("Valkyrien Skies wind API unavailable/incompatible; disabling this optional integration for the session.", unwrap(error));
            }
        }
    }

    private static boolean isInstalled() {
        if (!checkedPresence) {
            installed = FabricLoader.getInstance().isModLoaded("valkyrienskies");
            checkedPresence = true;
        }
        return installed;
    }

    private static Object shipWorld(ServerLevel level) throws ReflectiveOperationException {
        if (getShipWorld == null) {
            Class<?> provider = Class.forName("org.valkyrienskies.mod.common.IShipObjectWorldServerProvider");
            getShipWorld = provider.getMethod("getShipObjectWorld");
        }
        return getShipWorld.invoke(level.getServer());
    }

    private static void resolveShipMethods(Object ship) throws ReflectiveOperationException {
        if (getShipId != null) return;
        Class<?> type = ship.getClass();
        getShipId = type.getMethod("getId");
        getShipDimension = type.getMethod("getChunkClaimDimension");
        getShipToWorld = type.getMethod("getShipToWorld");
        getInertiaData = type.getMethod("getInertiaData");
        getDragController = type.getMethod("getDragController");
        Class<?> inertiaType = getInertiaData.getReturnType();
        getCenterOfMass = inertiaType.getMethod("getCenterOfMassInShip");
    }

    private static void resolveWindMethods(Object drag) throws NoSuchMethodException {
        if (setWindSpeed != null) return;
        // Accept either concrete Vector3d or Vector3dc as the VS API parameter.
        for (Method method : drag.getClass().getMethods()) {
            Class<?>[] args = method.getParameterTypes();
            if (method.getName().equals("setWindDirection") && args.length == 2
                && args[0].isAssignableFrom(Vector3d.class) && args[1] == String.class) {
                setWindDirection = method;
            } else if (method.getName().equals("setWindSpeed") && args.length == 2
                && (args[0] == double.class || args[0] == Double.class) && args[1] == String.class) {
                setWindSpeed = method;
            }
        }
        if (setWindDirection == null || setWindSpeed == null) throw new NoSuchMethodException("VS drag controller named wind source");
    }

    private static void writeWind(Object drag, Vector3d direction, double speed) throws ReflectiveOperationException {
        setWindDirection.invoke(drag, direction, PROVIDER);
        setWindSpeed.invoke(drag, speed, PROVIDER);
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return error;
    }

    private static final class SmoothedWind {
        double x, z;
        boolean initialized;
    }
}
