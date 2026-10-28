package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Aleki's Nifty Ships (Forge 1.20.1) wind-model adapter.
 *
 * Nifty Ships captures a WindModel for each boat on both logical sides and
 * updates that model during ticks. Bridge BOTH sides: authoritative wind via
 * WindApi.getAt() on the server, synced ClientWindState on the client for its
 * HUD, telltales and locally simulated boat movement.
 *
 * Wrapping the model preserves its registry and sailing physics; when disabled,
 * return the original provider's result, including other mods' wind providers.
 *
 * No Aleki types are referenced directly; the optional target is only loaded
 * when Nifty Ships is present. Do not copy or bundle Nifty Ships classes.
 */
public final class AlekiShipsWindBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LivingWind/NiftyShips");
    // Boat propulsion and HUD use a wind speed clamped to 0.2.
    // Both sides convert strength with the same server-synced scale.
    private static final float BOAT_MAX_SPEED = 0.2f;
    // Nifty Ships changes exactly zero speed to a fixed -0.03 wind. An almost
    // zero positive speed preserves Living Wind's direction on calm days.
    private static final float BOAT_CALM_EPSILON = 0.0001f;

    private static volatile Class<?> modelInterface;
    private static volatile Constructor<?> windConstructor;
    private static volatile boolean adapterUnavailable;
    private static volatile boolean logged;

    private AlekiShipsWindBridge() {}

    /** Called at WindModels.get(Level)'s RETURN, on both physical sides. */
    public static Object wrap(Level level, Object original) {
        if (original == null || adapterUnavailable
                || (!(level instanceof ServerLevel) && !level.isClientSide())) {
            return original;
        }
        try {
            Class<?> windModel = getModelInterface();
            if (!windModel.isInstance(original)) return original;
            // Keep other adapters from accidentally nesting the same bridge.
            if (Proxy.isProxyClass(original.getClass())
                    && Proxy.getInvocationHandler(original) instanceof ModelHandler) return original;
            return Proxy.newProxyInstance(windModel.getClassLoader(),
                new Class<?>[]{windModel}, new ModelHandler(level, original));
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
            adapterUnavailable = true;
            LOG.warn("Could not install Nifty Ships wind adapter; using its original wind provider.", e);
            return original;
        }
    }

    private static Class<?> getModelInterface() throws ClassNotFoundException {
        Class<?> result = modelInterface;
        if (result == null) {
            synchronized (AlekiShipsWindBridge.class) {
                result = modelInterface;
                if (result == null) modelInterface = result = Class.forName("com.alekiponi.alekiships.wind.WindModel");
            }
        }
        return result;
    }

    /** On the server read authoritative wind; on the client use Living Wind's
     * synchronized atmosphere so Nifty Ships can continue its own animation
     * and HUD updates. Missing sync/config simply falls back to its provider. */
    private static Object livingWind(Level level, BlockPos pos) {
        if (adapterUnavailable) return null;
        final float strength;
        final float direction;
        final float scale;

        if (level instanceof ServerLevel server) {
            if (!WindConfig.ALEKI_SHIPS_WIND_ENABLED.get()
                    || WindDimensionBlacklist.contains(server)) return null;
            WindSnapshot wind = WindApi.getAt(server, pos);
            strength = wind.strength();
            direction = wind.directionDegrees();
            scale = WindConfig.ALEKI_SHIPS_WIND_SCALE.get().floatValue();
        } else if (level.isClientSide()) {
            if (!ClientWindState.alekiShipsWindEnabled()) return null;
            strength = ClientWindState.strengthAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            direction = ClientWindState.directionDegrees();
            scale = ClientWindState.alekiShipsWindScale();
        } else {
            return null;
        }

        if (!Float.isFinite(strength) || !Float.isFinite(direction) || !Float.isFinite(scale)) return null;
        float speed = Math.max(BOAT_CALM_EPSILON,
            Mth.clamp(strength * scale, 0.0f, BOAT_MAX_SPEED));
        try {
            Constructor<?> constructor = getWindConstructor();
            Object result = constructor.newInstance(speed, Mth.wrapDegrees(direction));
            if (!logged && level instanceof ServerLevel) {
                logged = true;
                LOG.info("Nifty Ships sails now use Living Wind's position-aware direction and strength.");
            }
            return result;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
            adapterUnavailable = true;
            LOG.warn("Could not create Nifty Ships wind; reverting to the original wind provider.", e);
            return null;
        }
    }

    private static Constructor<?> getWindConstructor() throws ReflectiveOperationException {
        Constructor<?> result = windConstructor;
        if (result == null) {
            synchronized (AlekiShipsWindBridge.class) {
                result = windConstructor;
                if (result == null) {
                    result = Class.forName("com.alekiponi.alekiships.wind.Wind")
                        .getConstructor(float.class, float.class);
                    windConstructor = result;
                }
            }
        }
        return result;
    }

    private record ModelHandler(Level level, Object delegate) implements java.lang.reflect.InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "LivingWind NiftyShips WindModel (" + delegate + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> method.invoke(delegate, args);
                };
            }
            if (method.getName().equals("getWind") && args != null && args.length == 1
                    && args[0] instanceof BlockPos pos) {
                Object wind = livingWind(level, pos);
                if (wind != null) return wind;
            }
            // Config disabled, dimension disabled or future API mismatch:
            // the previously registered wind model still works normally.
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
