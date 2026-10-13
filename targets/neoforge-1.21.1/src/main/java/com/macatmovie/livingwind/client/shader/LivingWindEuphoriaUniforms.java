package com.macatmovie.livingwind.client.shader;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.lang.reflect.Method;
import java.util.function.DoubleSupplier;

/**
 * Client-only optional Euphoria integration. No hard Euphoria or Iris linkage.
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

    private LivingWindEuphoriaUniforms() {}

    private static boolean active() {
        return ClientWindConfig.SHADER_BRIDGE_ENABLED.get() && ClientWindState.hasServerState();
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
