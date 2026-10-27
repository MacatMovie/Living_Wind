package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Bridge to Windblown's per-frame GPU uniforms. No plant/block/chunk scans.
 * Changes FRAME[1], [8], [9] for atmospheric foliage and uses the unused
 * camera.w float in FRAME[7] to preserve the original water direction.
 * Windblown retains its native time, procedural gusts, sky attenuation, models,
 * plant categories and rustling sounds.
 */
public final class WindblownWindCompat {
    private static volatile boolean aquaticShaderSeparated;
    private static boolean warnedShader;
    private WindblownWindCompat() {}

    public static void adjustFrame(float[] frame, ByteBuffer buffer, Vec3 camera) {
        if (!aquaticShaderSeparated || frame == null || frame.length < 12 || buffer == null
                || camera == null || frame[2] != 4321.5f
                || !ClientWindState.hasServerState()
                || !ClientWindConfig.WINDBLOWN_WIND_COMPAT.get()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float strength = ClientWindState.strengthAtAltitude((int)Math.floor(camera.y),
                Level.OVERWORLD.equals(level.dimension()));
        strength *= frame[3] * ClientWindConfig.WINDBLOWN_WIND_MULTIPLIER.get().floatValue();
        if (!Float.isFinite(strength)) return;
        strength = Math.max(0f, Math.min(3.5f, strength));
        // Preserve Windblown's original pre-override direction for its water shader.
        // Camera.w was zero/unused in both tested Windblown versions. Encode the
        // native direction as an angle plus a sentinel offset (0 means unmodified).
        float originalWaterAngle = (float)Math.atan2(frame[9], frame[8]);
        float waterMarker = 10.0f + originalWaterAngle;
        float angle = ClientWindState.directionDegrees();
        float x = WindMath.windX(1f, angle);
        float z = WindMath.windZ(1f, angle);
        // The first 3 uniforms are part of WindblownInfo; values at 8/9 are WindblownWind.xy.
        // Keep Windblown's shadow/cave sky factor and its own wave/gust GPU calculations.
        frame[1] = strength;
        frame[7] = waterMarker;
        frame[8] = x;
        frame[9] = z;
        buffer.putFloat(4, strength);
        buffer.putFloat(28, waterMarker);
        buffer.putFloat(32, x);
        buffer.putFloat(36, z);
    }

    /**
     * Restore Windblown's original independent water current for its shader's water
     * category. This must succeed before we ever change the common GPU wind vector.
     * Both supplied Windblown JARs use exactly this shader layout.
     */
    public static String patchAquaticShader(String source) {
        if (source == null) return null;
        if (source.contains("windblownWaterDir")) {
            aquaticShaderSeparated = true;
            return source;
        }
        String begin = "if (kind > 2.5) {";
        String line = "vec2 xz = (dir * sway.x + side * sway.y)";
        if (!source.contains(begin) || !source.contains(line)
                || !source.contains("vec2 dir = WindblownWind.xy;")) {
            aquaticShaderSeparated = false;
            if (!warnedShader) {
                warnedShader = true;
                LoggerFactory.getLogger("Living Wind / Windblown").warn(
                        "Unknown Windblown water shader; disabled Living Wind foliage override to keep aquatic plants unaffected.");
            }
            return source;
        }
        // Camera.w is unused in Windblown 1.0.4/1.0.5, and is normally zero.
        // When LW integration is active we encode the original Windblown angle
        // there (+10 to distinguish it from the stock zero). This preserves the
        // aquatic movement exactly, including its original long-running wind cycle.
        String waterVector = "vec2 windblownWaterDir = dir;\n"
                + "        vec2 windblownWaterSide = side;\n"
                + "        if (WindblownCamera.w > 8.0) {\n"
                + "            float windblownWaterAngle = WindblownCamera.w - 10.0;\n"
                + "            windblownWaterDir = vec2(cos(windblownWaterAngle), sin(windblownWaterAngle));\n"
                + "            windblownWaterSide = vec2(-windblownWaterDir.y, windblownWaterDir.x);\n"
                + "        }\n";
        String patched = source.replace(begin, begin + "\n        " + waterVector);
        patched = patched.replace(line,
                "vec2 xz = (windblownWaterDir * sway.x + windblownWaterSide * sway.y)");
        aquaticShaderSeparated = true;
        return patched;
    }
}
