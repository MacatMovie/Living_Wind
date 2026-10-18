package com.macatmovie.livingwind.client.compat;

/**
 * Rewrites Interactive Foliage 2.2.0 shader text so the direction grass leans comes
 * from the {@code Weather} uniform instead of a hard-coded constant (M16).
 * <p>
 * Pure string functions with no Minecraft or Interactive Foliage types, so the
 * rewrite can be checked offline against the real shader files.
 *
 * <h2>Why {@code Weather.w}</h2>
 * IF's wind direction is {@code const vec2 WIND_DIRECTION = vec2(-1.0, 0.0)}, baked
 * into two separate shader sources that reach the GPU three different ways. A new
 * uniform would have to be plumbed through all three, and IF's
 * {@code foliage_legacy.json} only exposes the uniforms it lists, so vanilla's
 * {@code ShaderInstance} couldn't even set one. {@code Weather} already reaches
 * every pipeline, and its fourth component is only ever IF's reach fade, which
 * IF's Java always sets to the constant 4.0. So {@code w} is free to carry an angle,
 * provided the fade uses the literal instead.
 *
 * <h2>The negative sentinel</h2>
 * An angle is sent as {@code w = -(angle + 10)}, which is always negative, while IF's
 * fade is always positive. Every patched expression checks the sign: negative means
 * "our direction, a fade of 4", positive means "IF's own direction and this fade",
 * and in that case each expression is IF's original text verbatim. So a patched
 * shader behaves identically to IF's unless it is handed an angle, which is what lets
 * {@link InteractiveFoliageWindCompat} fall back to strength-only at any time without the shader and
 * the uniform ever disagreeing. The offset of 10 only has to exceed pi, so that angle
 * 0 still encodes as negative.
 *
 * <h2>Why the gust wave is crossfaded, not rotated</h2>
 * IF's gust is a travelling wave, {@code sin(-dot(world.xz, direction) * k + t)},
 * where {@code world} is the camera's position wrapped modulo 4096 plus the vertex
 * offset — so typically 1,000-4,000 blocks, almost anywhere in the world. Rotating
 * {@code direction} moves each blade's phase by {@code k * |world| * dAngle}, with
 * {@code k} about 0.35 rad per block. Even ordinary drift of a degree or two a second
 * then shifts phase several radians a second, and a command reversing the wind
 * scrambles it completely every frame: grass glitching back and forth until the
 * rotation stops. IF never rotated its direction, so it never met this.
 * <p>
 * Anchoring the phase near the camera doesn't help. Snapping the anchor to whole
 * wavelengths only ever adds multiples of 2pi, which change nothing; a continuous
 * anchor makes the waves slide along as you walk. What does work is never rotating a
 * wave at all: sixteen stationary waves on fixed compass axes, and the gust is a
 * linear crossfade between the two either side of the wind. No wave ever moves, so
 * nothing scrambles at any rotation speed; and at a sector boundary the outgoing wave
 * has zero weight, so crossing one is seamless. The cost is two {@code sin}s instead
 * of one, and slightly patchier gusts mid-sector, where two band orientations mix.
 * <p>
 * One residual, rarely seen: IF's wrap is chosen so exactly 230 wave cycles fit 4096
 * blocks, which only makes axis-aligned waves seamless across it. A diagonal sector's
 * gust can therefore twitch once as the camera crosses a multiple of 4096 blocks.
 *
 * <h2>Why the storm shake fades in with the lean</h2>
 * IF's wind carries a flutter across it and a tug along it, which its own comments
 * describe as the plant shaking "as if the wind were about to tear it out". They run
 * at about 1.7 and 2.2 Hz, five to seven times faster than the calm sway. IF only
 * ever shows them in rain, which eases in over a few seconds and then holds at full,
 * so the shake always rides on a plant bent well over, where it reads as whipping.
 * Our wind holds a light breeze indefinitely, and there the same shake sat on a plant
 * barely leaning at all, where it reads as bouncing - most visibly near the floor,
 * fading from view around strength 30 as the lean took over (confirmed live).
 * <p>
 * So the shake now ramps in with the lean: none below {@link #SHAKE_START_LEAN} of
 * full lean, IF's full shake from {@link #SHAKE_FULL_LEAN} up. With the default full
 * lean at 50, that's none below strength 20 and full from 40, as tuned live. It works through the {@code shake} argument to IF's wind function, which
 * inside it scales only the flutter and the tug, never the lean.
 */
/**
 * Adapted from AeroWeather's MIT-licensed FoliageShaderPatch by PostMalloy.
 * The 2.2.0 sublevel-aware shader adaptation is Living Wind-specific.
 * See THIRD_PARTY_NOTICES.md for license attribution.
 */
public final class InteractiveFoliageShaderPatch {
    /** The names one shader source uses for everything the patch touches. */
    public record Names(String direction, String weather, String scale, String time, String gustSpeed,
            String windCall) {
    }

    /** {@code include/sway.glsl}, spliced into the vanilla terrain and Iris programs. */
    public static final Names SWAY = new Names(
            "MC2_WIND_DIRECTION", "mc2_Weather", "MC2_SWAY_SCALE", "gameTime", "MC2_GUST_SPEED",
            "mc2_wind(world, phase, gameTime, reach, shake)");

    /** {@code core/foliage_legacy.vsh}, IF's own shader for the Sodium path. */
    public static final Names LEGACY = new Names(
            "WIND_DIRECTION", "Weather", "SWAY_SCALE", "GameTime", "GUST_SPEED",
            "wind(world, phase, reach, shake)");

    /** Added to the angle before negating; anything above pi keeps an angle of 0 negative. */
    public static final float ANGLE_OFFSET = 10.0f;

    /** Fraction of full lean below which there is no storm shake at all: strength 20 by default. */
    public static final float SHAKE_START_LEAN = 0.4f;

    /** Fraction of full lean from which IF's full storm shake applies: strength 40 by default. */
    public static final float SHAKE_FULL_LEAN = 0.8f;

    /** How many fixed axes the gust crossfades between. */
    public static final int GUST_SECTORS = 16;

    /** IF's {@code WEATHER_WIND_FADE_BLOCKS}: the fade width {@code w} used to carry. */
    private static final String FADE_BLOCKS = "4.0";

    private InteractiveFoliageShaderPatch() {
    }

    /**
     * The source with its direction taken from {@code weather.w}, its gust crossfaded
     * and its storm shake ramped in with the lean, or {@code null} if any of the four
     * targets isn't there exactly once — the caller then leaves the shader as IF wrote
     * it, which is always safe.
     */
    public static String patch(String source, Names names) {
        String w = names.weather() + ".w";
        String constant = "const vec2 " + names.direction() + " = vec2(-1.0, 0.0);";
        // In IF 2.2.0 on NeoForge 1.21.1 a sublevel-aware macro replaces the old constant.
        // Its moving-contraption wind must be left intact. Only the normal-world branch is changed.
        String sublevelMacro = "#define WIND_DIRECTION (mc2_SubLevelDown == vec3(0.0) ? vec2(-1.0, 0.0) : mc2_SubLevelWind)";
        boolean sublevel = names == LEGACY && source.contains(sublevelMacro);
        String directionTarget = sublevel ? sublevelMacro : constant;
        String fade = names.weather() + ".z - " + w;
        String gustOriginal = "0.5 + 0.5 * sin(along * " + names.scale() + " + "
                + names.time() + " * " + names.gustSpeed() + ")";
        String gustLine = "float gust = " + gustOriginal + ";";
        if (count(source, directionTarget) != 1 || count(source, fade) != 1 || count(source, gustLine) != 1
                || count(source, names.windCall()) != 1) {
            return null;
        }

        String encoded = sublevel ? "(" + w + " < 0.0 && mc2_SubLevelDown == vec3(0.0))" : w + " < 0.0";
        String fadeEncoded = w + " < 0.0";
        String angle = "(-" + w + " - " + ANGLE_OFFSET + ")";

        // A #define, not a const: its value now comes from a uniform, which a const can't
        // hold. Legal because every use of the name is inside a function body, where the
        // macro expands and the uniform is declared by then - never in another global const
        // initialiser. Checked against both sources.
        String define = "#define " + names.direction() + " (" + encoded
                + " ? vec2(cos" + angle + ", sin" + angle + ")"
                + " : vec2(-1.0, 0.0))";

        if (sublevel) {
            define = "#define WIND_DIRECTION (mc2_SubLevelDown == vec3(0.0) ? " +
                    "(" + encoded + " ? vec2(cos" + angle + ", sin" + angle + ") : vec2(-1.0, 0.0))" +
                    " : mc2_SubLevelWind)";
        }

        String fadeWithSentinel = names.weather() + ".z - (" + fadeEncoded + " ? " + FADE_BLOCKS + " : " + w + ")";

        // Statements, not one expression, since the line sits inside the function body. IF's
        // own `along` line above is left untouched: with the gust no longer reading it, it's
        // dead code the compiler drops.
        String sectorWidth = Float.toString((float) (2.0 * Math.PI / GUST_SECTORS));
        String crossfade = "float livingwind_sector = " + angle + " / " + sectorWidth + ";\n"
                + "    float livingwind_base = floor(livingwind_sector);\n"
                + "    float livingwind_blend = livingwind_sector - livingwind_base;\n"
                + "    vec2 livingwind_axisA = vec2(cos(livingwind_base * " + sectorWidth + "), "
                + "sin(livingwind_base * " + sectorWidth + "));\n"
                + "    vec2 livingwind_axisB = vec2(cos((livingwind_base + 1.0) * " + sectorWidth + "), "
                + "sin((livingwind_base + 1.0) * " + sectorWidth + "));\n"
                + "    float gust = " + encoded + "\n"
                + "            ? mix(" + gustAlong("livingwind_axisA", names) + ",\n"
                + "                  " + gustAlong("livingwind_axisB", names) + ", livingwind_blend)\n"
                + "            : " + gustOriginal + ";";

        // IF's own shake is kept verbatim when it isn't handed our direction, like everything else.
        String shakeRamp = "(" + encoded + " ? smoothstep(" + SHAKE_START_LEAN + ", " + SHAKE_FULL_LEAN + ", "
                + names.weather() + ".x) : 1.0)";
        String windCallRamped = names.windCall().replace("shake)", "shake * " + shakeRamp + ")");

        return source.replace(names.windCall(), windCallRamped)
                .replace(gustLine, crossfade)
                .replace(fade, fadeWithSentinel)
                .replace(directionTarget, define);
    }

    /** IF's gust formula, travelling along a fixed {@code axis} instead of the wind direction. */
    private static String gustAlong(String axis, Names names) {
        return "0.5 + 0.5 * sin(-dot(world.xz, " + axis + ") * " + names.scale() + " + "
                + names.time() + " * " + names.gustSpeed() + ")";
    }

    /** The {@code w} to send for a direction: {@code atan2} of the travel vector, encoded negative. */
    public static float encodeAngle(double travelX, double travelZ) {
        return -((float) Math.atan2(travelZ, travelX) + ANGLE_OFFSET);
    }

    private static int count(String text, String target) {
        int count = 0;
        for (int at = text.indexOf(target); at >= 0; at = text.indexOf(target, at + target.length())) {
            count++;
        }
        return count;
    }
}
