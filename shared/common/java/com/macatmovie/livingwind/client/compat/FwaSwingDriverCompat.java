package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.config.ClientWindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Optional Fancy World Animations SwingDrivers API integration (FWA 1.3.4+).
 *
 * No compile-time dependency on FWA: the proxy implements its public SwingDriver
 * interface at runtime, so Living Wind can run without FWA installed. This replaces
 * the fragile animation-field mixins. Bells are intentionally left to FWA
 * until its public API supports bell animation.
 */
public final class FwaSwingDriverCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("LivingWind/FWA");
    private static boolean attempted;

    private FwaSwingDriverCompat() {}

    /** Called once on the client thread during client setup, after mod discovery. */
    public static synchronized void register() {
        if (attempted) return;
        attempted = true;

        try {
            Class<?> driversClass = Class.forName("fr.madu59.fwa.api.animations.SwingDrivers");
            Class<?> driverClass = Class.forName("fr.madu59.fwa.api.animations.SwingDriver");
            Class<?> swingClass = Class.forName("fr.madu59.fwa.api.animations.HangingSwing");

            Method getDriver = driversClass.getMethod("getDriver");
            Method register = driversClass.getMethod("register", driverClass);
            Method getSwing = driverClass.getMethod("getSwing",
                BlockPos.class, BlockState.class, double.class, swingClass);
            Field tiltX = swingClass.getField("tiltX");
            Field tiltZ = swingClass.getField("tiltZ");
            Field spin = swingClass.getField("spin");

            Object originalDriver = getDriver.invoke(null);
            if (originalDriver == null) return;
            InvocationHandler adapter = (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "Living Wind FWA SwingDriver (wrapping " + originalDriver + ")";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                }
                if (!"getSwing".equals(method.getName())) {
                    return invokeOriginal(getSwing, originalDriver, args);
                }

                // Always delegate first. FWA's own weather and animation settings
                // continue to determine its base swing, including when LW is off.
                boolean hasSwing = (Boolean) invokeOriginal(getSwing, originalDriver, args);
                if (!hasSwing || !ClientWindConfig.FWA_WIND_COMPAT.get()) return hasSwing;

                BlockPos pos = (BlockPos) args[0];
                BlockState state = (BlockState) args[1];
                FwaWindMotion.ObjectType type;
                if (state.getBlock() instanceof LanternBlock
                        || (state.hasProperty(BlockStateProperties.HANGING)
                            && state.getValue(BlockStateProperties.HANGING))) {
                    // FWA also animates modded lanterns with the vanilla HANGING
                    // property, even if their block class is not LanternBlock.
                    type = FwaWindMotion.ObjectType.LANTERN;
                } else if (state.getBlock() instanceof ChainBlock) {
                    type = FwaWindMotion.ObjectType.CHAIN;
                } else {
                    // FWA also uses SwingDrivers for hanging signs. Do not change
                    // their behavior or interfere with other providers' animations.
                    return true;
                }

                Object swing = args[3];
                float x = tiltX.getFloat(swing);
                float z = tiltZ.getFloat(swing);
                float y = spin.getFloat(swing);
                FwaWindMotion.Angles updated = FwaWindMotion.swingingForTick(pos, x, z, y,
                    type, ((Number) args[2]).doubleValue());
                tiltX.setFloat(swing, updated.x());
                tiltZ.setFloat(swing, updated.z());
                spin.setFloat(swing, updated.spin());
                return true;
            };

            Object proxy = Proxy.newProxyInstance(driverClass.getClassLoader(),
                new Class<?>[]{driverClass}, adapter);
            register.invoke(null, proxy);
            LOGGER.info("Registered Living Wind with Fancy World Animations' SwingDrivers API");
        } catch (ClassNotFoundException ignored) {
            // FWA is an optional dependency; no special handling needed.
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Could not register Fancy World Animations SwingDrivers API; FWA will use its default swings", e);
        }
    }

    private static Object invokeOriginal(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
