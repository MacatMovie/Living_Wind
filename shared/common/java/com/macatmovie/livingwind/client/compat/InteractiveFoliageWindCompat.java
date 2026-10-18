package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Vector4f;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Optional Interactive Foliage 2.2.0 GPU wind bridge. This does not alter SWAY's
 * entity/plant interactions, add particles, or use an IF dependency at compile time.
 *
 * Shader-uniform / provider approach informed by AeroWeather's MIT-licensed
 * Interactive Foliage compatibility (PostMalloy). See THIRD_PARTY_NOTICES.md.
 */
public final class InteractiveFoliageWindCompat {
    private static final String MOD_ID = "mc2_interactivefoliage";
    private static volatile boolean shaderPatchFailed;
    private static boolean warnedPatchFailed;
    private static boolean initialized;
    private static boolean weatherEnabled;
    private static long lastFrameNanos;
    private static float strength;
    private static float direction;

    private InteractiveFoliageWindCompat() {}

    public static String patchSway(String original) {
        return patchOrKeep(original, InteractiveFoliageShaderPatch.SWAY);
    }

    /**
     * Only replaces IF's own legacy vertex shader. Resource reloads get a fresh
     * provider and the rest of the resource pipeline remains unchanged.
     */
    public static ResourceProvider wrapLegacyProvider(ResourceProvider original) {
        return location -> {
            Optional<Resource> found = original.getResource(location);
            if (found.isEmpty() || !isLegacyVertex(location)) return found;
            Resource resource = found.get();
            return Optional.of(new Resource(resource.source(), () -> {
                try (InputStream in = resource.open()) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    String patched = patchOrKeep(text, InteractiveFoliageShaderPatch.LEGACY);
                    return new ByteArrayInputStream(patched.getBytes(StandardCharsets.UTF_8));
                }
            }));
        };
    }

    private static boolean isLegacyVertex(ResourceLocation id) {
        if (!MOD_ID.equals(id.getNamespace())) return false;
        String path = id.getPath();
        return path.endsWith("foliage_legacy.vsh") || path.endsWith("foliage_legacy_1_20.vsh");
    }

    private static String patchOrKeep(String source, InteractiveFoliageShaderPatch.Names names) {
        String patched = InteractiveFoliageShaderPatch.patch(source, names);
        if (patched != null) return patched;
        shaderPatchFailed = true;
        // No incompatible shader source modification: retain IF's shader, switch to
        // strength-only so Weather.w keeps its normal distance-fade meaning.
        if (!warnedPatchFailed) {
            warnedPatchFailed = true;
            LoggerFactory.getLogger("Living Wind / Interactive Foliage")
                    .warn("Unknown Interactive Foliage shader revision; direction override safely disabled (strength-only fallback).");
        }
        return source;
    }

    private static boolean enabled() {
        return ClientWindConfig.INTERACTIVE_FOLIAGE_WIND_COMPAT.get()
                && ClientWindState.hasServerState()
                && Minecraft.getInstance().level != null;
    }

    public static Vector4f adjustWeather(Vector4f original) {
        if (!enabled()) {
            initialized = false;
            weatherEnabled = false;
            return original;
        }
        // Respect IF's own Weather Wind toggle. A zero reach also means there is
        // no GPU wind field to modify; don't force its animation on.
        weatherEnabled = original.z > 0.0f;
        if (!weatherEnabled) return original;
        updateSmoothedWind();
        float lean = Math.min(1.0f, Math.max(0.0f, strength));
        float storm = Math.min(1.0f, Math.max(0.0f, strength - 1.0f));
        float fourth = shaderPatchFailed ? original.w
                : InteractiveFoliageShaderPatch.encodeAngle(
                        WindMath.windX(1.0f, direction), WindMath.windZ(1.0f, direction));
        return new Vector4f(lean, storm, original.z, fourth);
    }

    public static float adjustCalmSway(float original) {
        if (!enabled() || !weatherEnabled) return original;
        // Calm means calm: no perpetual idle bending at Living Wind strength zero.
        return original * Math.min(1.0f, Math.max(0.0f, strength));
    }

    public static float shelterRainLevel(float original) {
        // IF's native weather-wind shelter assumes a fixed westward wind. With
        // our variable direction it would shelter the wrong side of a wall.
        return enabled() && !shaderPatchFailed ? 0.0f : original;
    }

    /** Render-thread time-based easing avoids 20 FPS steps on a 60+ FPS GPU shader. */
    private static void updateSmoothedWind() {
        long now = System.nanoTime();
        float targetStrength = Math.max(0.0f, ClientWindState.strength());
        float targetDirection = ClientWindState.directionDegrees();
        if (!initialized || !Float.isFinite(strength) || !Float.isFinite(direction)) {
            strength = targetStrength;
            direction = targetDirection;
            initialized = true;
            lastFrameNanos = now;
            return;
        }
        float dt = Math.min(0.1f, Math.max(0.0f, (now - lastFrameNanos) * 1.0e-9f));
        lastFrameNanos = now;
        if (Minecraft.getInstance().isPaused()) return;
        float rate = 1.0f - (float)Math.exp(-6.0f * dt);
        strength += (targetStrength - strength) * rate;
        float delta = WindMath.shortestDeltaDegrees(direction, targetDirection) * rate;
        float maxStep = 180.0f * dt; // smooth even during a forced 180-degree reversal
        direction = WindMath.normalizeDegrees(direction + Math.max(-maxStep, Math.min(maxStep, delta)));
    }
}
