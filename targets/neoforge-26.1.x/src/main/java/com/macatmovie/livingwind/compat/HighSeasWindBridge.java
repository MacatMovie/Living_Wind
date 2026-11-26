package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small optional cross-mod wind adapter. High Seas remains the owner of its
 * sails, boats, aero response, instruments and visuals. Its (private) wind
 * generator is bypassed only for wind queries while compatibility is enabled.
 *
 * Does not link to, redistribute, or compile against any High Seas code.
 * The provided source was consulted for API shape only (ARR licensed).
 */
public final class HighSeasWindBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LivingWind/HighSeas");
    private static volatile MethodHandle constructor;
    private static volatile boolean unavailable;
    private static boolean firstSuccess;
    private static ServerLevel cachedLevel;
    private static long cachedTick = Long.MIN_VALUE;
    private static WindSnapshot cachedWind;

    private HighSeasWindBridge() {}

    /** Returns null to let High Seas use its unmodified implementation. */
    public static Object override(Level level, double x, double y, double z) {
        if (level == null || unavailable) return null;
        final float strength;
        final float direction;
        final double altitude;
        if (level instanceof ServerLevel server) {
            if (!WindConfig.HIGH_SEAS_WIND_ENABLED.get()) return null;
            long tick = server.getGameTime();
            // High Seas can query dozens of sails per tick; only fetch Living Wind's
            // saved atmospheric snapshot once on the server for that dimension.
            if (cachedLevel != server || cachedTick != tick || cachedWind == null) {
                cachedWind = WindApi.get(server);
                cachedLevel = server;
                cachedTick = tick;
            }
            strength = cachedWind.strength();
            direction = cachedWind.directionDegrees();
            altitude = WindApi.altitudeMultiplier(server, (int) Math.floor(y));
        } else if (level.isClientSide()) {
            // The toggle is synced by Living Wind's existing wind packet flags.
            // High Seas vane, goggles, sail shaders and debug renderer all use
            // the same source as server-side sails, including moving vessels.
            if (!ClientWindState.highSeasWindEnabled()) return null;
            strength = ClientWindState.strength();
            direction = ClientWindState.directionDegrees();
            altitude = strength == 0f ? 1.0 :
                    ClientWindState.strengthAtAltitude((int) Math.floor(y), level.dimension() == Level.OVERWORLD) / strength;
        } else return null;

        // Preserve calm states; never permit NaN / infinity to enter physics.
        if (!Float.isFinite(strength) || !Float.isFinite(direction) || !Double.isFinite(altitude)) return null;
        double s = Math.max(0.0, strength * altitude);
        Vec3 velocity = new Vec3(WindMath.windX((float) s, direction), 0,
                WindMath.windZ((float) s, direction));
        try {
            MethodHandle factory = getFactory();
            if (factory == null) return null;
            // High Seas WindSample record(Vec3, weatherMult, biomeMult,
            // altitudeMult, occlusion, gust). Its physics uses .vector().
            // Extra diagnostic multipliers must not multiply the vector again.
            Object result = (Object) factory.invokeExact(velocity, 1.0d, 1.0d, altitude, 1.0d, 1.0d);
            if (!firstSuccess) {
                firstSuccess = true;
                LOG.info("Create: High Seas wind queries now use Living Wind. Ships, sail physics, vane and sail visuals share a single wind source.");
            }
            return result;
        } catch (Throwable error) {
            unavailable = true;
            LOG.warn("Failed to construct the installed High Seas wind result; falling back to High Seas wind.", error);
            return null;
        }
    }

    private static MethodHandle getFactory() {
        MethodHandle result = constructor;
        if (result != null || unavailable) return result;
        synchronized (HighSeasWindBridge.class) {
            if (constructor != null || unavailable) return constructor;
            try {
                Class<?> type = Class.forName("com.maxenonyme.highseas.wind.WindSample");
                constructor = MethodHandles.publicLookup()
                        .findConstructor(type, MethodType.methodType(void.class,
                                Vec3.class, double.class, double.class, double.class,
                                double.class, double.class))
                        .asType(MethodType.methodType(Object.class, Vec3.class,
                                double.class, double.class, double.class, double.class, double.class));
                return constructor;
            } catch (ReflectiveOperationException | LinkageError e) {
                unavailable = true;
                LOG.warn("Create: High Seas changed its wind API; leaving its original wind provider active.", e);
                return null;
            }
        }
    }
}
