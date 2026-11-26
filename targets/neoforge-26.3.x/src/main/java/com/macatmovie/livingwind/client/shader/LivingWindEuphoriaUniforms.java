package com.macatmovie.livingwind.client.shader;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.lang.reflect.Method;
import java.util.function.DoubleSupplier;

/**
 * Client-only uniform values for optional Euphoria and Iris/Oculus integration.
 * No hard shader-loader linkage.
 * Callback suppliers are sampled PER_FRAME by Euphoria 1.10.x, rather than
 * uploading data from our own GL calls or patching render state.
 *
 * The shader pack is a separate OPTIONAL client asset: uniforms alone do not
 * alter vanilla/Complementary shaders.
 */
public final class LivingWindEuphoriaUniforms {
    private static long lastNanos;
    private static double waveClock;
    private static float smoothedSpeed = 1f;
    private static boolean loggedFailure;
    private static boolean loggedIrisFailure;
    private static long lastStormNanos;
    private static double stormX, stormZ;
    private static double smoothStormX, smoothStormZ;
    private static Object stormDimension;


    private LivingWindEuphoriaUniforms() {}

    private static boolean active() {
        return ClientWindConfig.SHADER_BRIDGE_ENABLED.get() && ClientWindState.hasServerState()
            && !ClientWindState.dimensionWindDisabled();
    }

    /** The shader can fall back to Complementary's original waving without the mod. */
    public static double activeValue() { return active() ? 1.0 : 0.0; }
    public static double windStrength() {
        if (!active()) return 0.0;
        // Client visual wind includes altitude, but NOT camera shelter: leaves
        // outside a cave should still wave even if the player stands inside.
        return Math.max(0.0, Math.min(2.5, ClientWindState.altitudeStrength()));
    }
    public static double windGust() {
        return active() ? Math.max(0.0, Math.min(1.5, ClientWindState.gustStrength())) : 0.0;
    }
    public static double directionX() { return active() ? ClientWindState.globalWindX() / Math.max(0.0001f, ClientWindState.strength()) : 0.0; }
    public static double directionZ() { return active() ? ClientWindState.globalWindZ() / Math.max(0.0001f, ClientWindState.strength()) : 0.0; }
    public static double amplitude() { return active() ? ClientWindConfig.SHADER_WAVING_MULTIPLIER.get() : 1.0; }

    /**
     * Continuous integrated wind clock. Varying frequency by multiplying the
     * current frameTimeCounter directly would jump the phase during gusts.
     * This never reads world/chunks, and caps time after pauses/reloads.
     */
    public static synchronized double animationTime() {
        long now = System.nanoTime();
        if (lastNanos == 0L) {
            lastNanos = now;
            return waveClock;
        }
        double dt = Math.max(0.0, Math.min(0.06, (now - lastNanos) / 1_000_000_000.0));
        lastNanos = now;
        float wanted = active() ? (float) (0.60 + Math.min(2.5, windStrength()) * 0.65) : 1f;
        smoothedSpeed += (wanted - smoothedSpeed) * (float) Math.min(1.0, dt * 3.5);
        waveClock += dt * smoothedSpeed;
        if (waveClock > 100000.0) waveClock -= 100000.0;
        return waveClock;
    }

    /** Integrate fog translation in Java so changing wind strength never jumps phase.
     * At strength 0 it stops moving, but the shader's own turbulence remains.
     * Dimension transitions reset accumulated fog drift to prevent carried-over motion.
     */
    private static synchronized void advanceStorm() {
        long now = System.nanoTime();
        Object dimension = net.minecraft.client.Minecraft.getInstance().level == null ? null
            : net.minecraft.client.Minecraft.getInstance().level.dimension();
        if (dimension != stormDimension) {
            stormDimension = dimension;
            stormX = stormZ = smoothStormX = smoothStormZ = 0.0;
            lastStormNanos = now;
        }
        if (lastStormNanos == 0L) { lastStormNanos = now; return; }
        double dt = Math.max(0.0, Math.min(0.06, (now - lastStormNanos) / 1_000_000_000.0));
        lastStormNanos = now;
        if (!active() || dt <= 0.0) {
            smoothStormX = smoothStormZ = 0.0;
            return;
        }
        double strength = Math.min(2.5, Math.max(0.0, windStrength()));
        if (strength < 0.001) {
            // Zero wind means zero additional fog drift (native storm noise remains animated).
            smoothStormX = smoothStormZ = 0.0;
            return;
        }
        double speed = (0.75 * strength + 1.85 * strength * strength)
            * (1.0 + 0.12 * Math.min(1.5, windGust())); // approx 2.6 blocks/s at 1.0
        double x = directionX(), z = directionZ();
        double magnitude = Math.hypot(x, z);
        double vx = magnitude > 0.0001 ? x / magnitude * speed : 0.0;
        double vz = magnitude > 0.0001 ? z / magnitude * speed : 0.0;
        // Smooth gusts and direction changes; 1-2 frames cannot snap the fog field.
        double alpha = 1.0 - Math.exp(-dt * 3.0);
        smoothStormX += (vx - smoothStormX) * alpha;
        smoothStormZ += (vz - smoothStormZ) * alpha;
        stormX += smoothStormX * dt;
        stormZ += smoothStormZ * dt;
    }
    public static double stormActive() { return active() ? 1.0 : 0.0; }
    public static synchronized double stormOffsetX() { advanceStorm(); return stormX; }
    public static synchronized double stormOffsetZ() { advanceStorm(); return stormZ; }

    /**
     * Register directly with Iris/Oculus for unmodified Complementary (no EP).
     * Iris's public UniformHolder interface is accessed reflectively to avoid
     * linking optional shader-loader classes when they are not installed.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerIris(Object uniforms) {
        if (uniforms == null || euphoriaPresent()) return;
        try {
            for (String pkg : new String[]{"net.irisshaders.iris", "net.coderbot.iris"}) {
                Class<?> holder;
                Class<?> freq;
                try {
                    holder = Class.forName(pkg + ".gl.uniform.UniformHolder", false, uniforms.getClass().getClassLoader());
                    freq = Class.forName(pkg + ".gl.uniform.UniformUpdateFrequency", false, uniforms.getClass().getClassLoader());
                } catch (ClassNotFoundException missing) { continue; }
                if (!holder.isInstance(uniforms)) continue;
                Object perFrame = Enum.valueOf((Class<? extends Enum>) freq.asSubclass(Enum.class), "PER_FRAME");
                Method register = holder.getMethod("uniform1f", freq, String.class, DoubleSupplier.class);
                addIris(register, uniforms, perFrame, "livingWindActive", LivingWindEuphoriaUniforms::activeValue);
                addIris(register, uniforms, perFrame, "livingWindStrength", LivingWindEuphoriaUniforms::windStrength);
                addIris(register, uniforms, perFrame, "livingWindGust", LivingWindEuphoriaUniforms::windGust);
                addIris(register, uniforms, perFrame, "livingWindDirectionX", LivingWindEuphoriaUniforms::directionX);
                addIris(register, uniforms, perFrame, "livingWindDirectionZ", LivingWindEuphoriaUniforms::directionZ);
                addIris(register, uniforms, perFrame, "livingWindAmplitude", LivingWindEuphoriaUniforms::amplitude);
                addIris(register, uniforms, perFrame, "livingWindTime", LivingWindEuphoriaUniforms::animationTime);
                addIris(register, uniforms, perFrame, "livingWindStormActive", LivingWindEuphoriaUniforms::stormActive);
                addIris(register, uniforms, perFrame, "livingWindStormOffsetX", LivingWindEuphoriaUniforms::stormOffsetX);
                addIris(register, uniforms, perFrame, "livingWindStormOffsetZ", LivingWindEuphoriaUniforms::stormOffsetZ);
                return;
            }
        } catch (ReflectiveOperationException | LinkageError ex) {
            if (!loggedIrisFailure) {
                loggedIrisFailure = true;
                System.err.println("[Living Wind] Optional Iris/Oculus uniform bridge unavailable: " + ex);
            }
        }
    }

    private static boolean euphoriaPresent() {
        try {
            Class.forName("com.euphoriapatches.euphoria_patcher.integration.uniforms.EuphoriaUniforms", false,
                LivingWindEuphoriaUniforms.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError absent) { return false; }
    }

    private static void addIris(Method method, Object holder, Object freq, String name, DoubleSupplier value)
        throws ReflectiveOperationException {
        method.invoke(holder, freq, name, value);
    }

    /**
     * EuphoriaUniforms.declareAll's argument is its public UniformDeclarer
     * interface. Reflection keeps the mod startup-safe with no Euphoria JAR.
     * Euphoria 1.10.5 invokes these as PER_FRAME registrations.
     */
    public static void register(Object declarer) {
        if (declarer == null) return;
        try {
            Method uniform1f = declarer.getClass().getMethod("uniform1f", String.class, DoubleSupplier.class);
            add(uniform1f, declarer, "livingWindActive", LivingWindEuphoriaUniforms::activeValue);
            add(uniform1f, declarer, "livingWindStrength", LivingWindEuphoriaUniforms::windStrength);
            add(uniform1f, declarer, "livingWindGust", LivingWindEuphoriaUniforms::windGust);
            add(uniform1f, declarer, "livingWindDirectionX", LivingWindEuphoriaUniforms::directionX);
            add(uniform1f, declarer, "livingWindDirectionZ", LivingWindEuphoriaUniforms::directionZ);
            add(uniform1f, declarer, "livingWindAmplitude", LivingWindEuphoriaUniforms::amplitude);
            add(uniform1f, declarer, "livingWindTime", LivingWindEuphoriaUniforms::animationTime);
            add(uniform1f, declarer, "livingWindStormActive", LivingWindEuphoriaUniforms::stormActive);
            add(uniform1f, declarer, "livingWindStormOffsetX", LivingWindEuphoriaUniforms::stormOffsetX);
            add(uniform1f, declarer, "livingWindStormOffsetZ", LivingWindEuphoriaUniforms::stormOffsetZ);
        } catch (ReflectiveOperationException | LinkageError ex) {
            // Optional bridge must never crash the client if the EP API changes.
            if (!loggedFailure) {
                loggedFailure = true;
                System.err.println("[Living Wind] Optional Euphoria shader uniforms unavailable: " + ex);
            }
        }
    }

    private static void add(Method m, Object holder, String name, DoubleSupplier value)
        throws ReflectiveOperationException {
        m.invoke(holder, name, value);
    }
}
