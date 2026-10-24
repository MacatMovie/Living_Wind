package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small Ships 2.0 remains responsible for sail angle, acceleration, ship speeds,
 * turning and every other physics decision. Only its Wind(direction, strength)
 * input is replaced, on both client and server.
 *
 * All Small Ships types are loaded reflectively: this class and Living Wind can
 * be loaded without Small Ships installed. No foreign JAR is bundled.
 */
public final class SmallShipsWindBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LivingWind/SmallShips");
    private static volatile MethodHandle constructor;
    private static volatile boolean unavailable;
    private static volatile Field enableField;
    private static volatile Method enableGet;
    private static volatile boolean enableProbeFailed;
    private static boolean logged;

    private SmallShipsWindBridge() {}

    /** Return null to leave Small Ships' original wind result untouched. */
    public static Object serverWind(ServerLevel level) {
        if (level == null || !WindConfig.SMALL_SHIPS_WIND_ENABLED.get() || !smallShipsWindAllowed()) return null;
        WindSnapshot snapshot = WindApi.get(level);
        return newWind(snapshot.directionDegrees(), snapshot.strength());
    }

    /** The client uses the already-synchronized, interpolated Living Wind state. */
    public static Object clientWind() {
        if (!ClientWindState.smallShipsWindEnabled() || !smallShipsWindAllowed()) return null;
        return newWind(ClientWindState.directionDegrees(), ClientWindState.strength());
    }

    public static boolean clientActive() {
        return ClientWindState.smallShipsWindEnabled() && smallShipsWindAllowed();
    }

    public static float clientDirection() { return Mth.wrapDegrees(ClientWindState.directionDegrees()); }
    public static float clientStrength() { return Mth.clamp(ClientWindState.strength(), 0.0f, 1.0f); }

    private static Object newWind(float direction, float strength) {
        if (!Float.isFinite(direction) || !Float.isFinite(strength)) return null;
        MethodHandle ctor = factory();
        if (ctor == null) return null;
        try {
            // Both mods use Minecraft's yaw convention: X=-sin(yaw), Z=cos(yaw).
            // Small Ships' supported strength range is [0, 1].
            Object result = (Object) ctor.invokeExact(Mth.wrapDegrees(direction), Mth.clamp(strength, 0.0f, 1.0f));
            if (!logged) {
                logged = true;
                LOG.info("Small Ships 2.0 wind now follows Living Wind; Small Ships' sailing physics are unchanged.");
            }
            return result;
        } catch (Throwable error) {
            unavailable = true;
            LOG.warn("Small Ships wind API call failed; using its own wind instead.", error);
            return null;
        }
    }

    private static MethodHandle factory() {
        if (unavailable) return null;
        MethodHandle result = constructor;
        if (result != null) return result;
        synchronized (SmallShipsWindBridge.class) {
            if (constructor != null || unavailable) return constructor;
            try {
                Class<?> windType = Class.forName("com.talhanation.smallships.world.wind.Wind");
                constructor = MethodHandles.publicLookup()
                    .findConstructor(windType, MethodType.methodType(void.class, float.class, float.class))
                    .asType(MethodType.methodType(Object.class, float.class, float.class));
                return constructor;
            } catch (ReflectiveOperationException | LinkageError e) {
                unavailable = true;
                LOG.warn("Could not resolve Small Ships 2.0 Wind(float,float); leaving its own wind intact.", e);
                return null;
            }
        }
    }

    /** Honour Small Ships' existing windEnable setting, including live changes. */
    private static boolean smallShipsWindAllowed() {
        if (enableProbeFailed) return false; // safe fallback: preserve Small Ships' behavior
        try {
            if (enableField == null || enableGet == null) {
                synchronized (SmallShipsWindBridge.class) {
                    if (enableField == null || enableGet == null) {
                        Class<?> configClass = Class.forName("com.talhanation.smallships.config.SmallShipsConfig$Server");
                        Field field = configClass.getField("windEnable");
                        Object value = field.get(null);
                        if (value == null) return false; // Small Ships config not initialized yet
                        Method method = value.getClass().getMethod("get");
                        enableGet = method;
                        enableField = field;
                    }
                }
            }
            Object setting = enableField.get(null);
            return Boolean.TRUE.equals(enableGet.invoke(setting));
        } catch (ReflectiveOperationException | LinkageError e) {
            enableProbeFailed = true;
            LOG.warn("Unable to read Small Ships' windEnable setting; leaving its own wind active.", e);
            return false;
        }
    }
}
