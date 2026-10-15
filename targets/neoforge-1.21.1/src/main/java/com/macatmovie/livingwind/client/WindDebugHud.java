package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class WindDebugHud {
    private static final int X = 6;
    private static final int Y = 6;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;

    private WindDebugHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientWindConfig.DebugHudMode mode = ClientWindConfig.debugHudMode();
        if (mode == ClientWindConfig.DebugHudMode.OFF) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        List<String> lines = new ArrayList<>();
        if (mode == ClientWindConfig.DebugHudMode.SIMPLE) {
            lines.add("Living Wind");
            lines.add(String.format(Locale.ROOT, "Wind: %.2f  [%s]",
                ClientWindState.localStrength(), strengthName(ClientWindState.localStrength())));
            lines.add(String.format(Locale.ROOT, "Direction: %.0f° %s",
                ClientWindState.directionDegrees(), compass(ClientWindState.directionDegrees())));
            float gust = ClientWindState.gustStrength();
            lines.add(gust > 0.005f
                ? String.format(Locale.ROOT, "Gust: ACTIVE (+%.2f)", gust)
                : "Gust: inactive");
            lines.add("Windy Period: " + (ClientWindState.windyPeriod() ? "ON" : "OFF"));
        } else {
            lines.add("Living Wind Debug");
            lines.add("Synced: " + yesNo(ClientWindState.hasServerState()));
            lines.add(String.format(Locale.ROOT, "Global: %.3f  [%s]", ClientWindState.strength(), strengthName(ClientWindState.strength())));
            lines.add(String.format(Locale.ROOT, "Synced target: %.3f", ClientWindState.targetStrength()));
            lines.add(String.format(Locale.ROOT, "Altitude: %.3f (x%.2f)", ClientWindState.altitudeStrength(), ClientWindState.altitudeMultiplier()));
            lines.add(String.format(Locale.ROOT, "Local: %.3f   Exposure: %.2f", ClientWindState.localStrength(), ClientWindState.exposure()));
            lines.add(String.format(Locale.ROOT, "Direction: %.1f° %s", ClientWindState.directionDegrees(), compass(ClientWindState.directionDegrees())));
            lines.add(String.format(Locale.ROOT, "Target dir: %.1f°", ClientWindState.targetDirectionDegrees()));
            lines.add(String.format(Locale.ROOT, "Vector X/Z: %+.3f / %+.3f", ClientWindState.localWindX(), ClientWindState.localWindZ()));
            lines.add(String.format(Locale.ROOT, "Turbulence: %.3f", ClientWindState.turbulence()));
            lines.add(String.format(Locale.ROOT, "Dir instability: %.3f", ClientWindState.directionInstability()));
            lines.add(String.format(Locale.ROOT, "Gust contribution: %+.3f", ClientWindState.gustStrength()));
            lines.add("Weather: " + weatherName(mc));
            lines.add("Windy period: " + yesNo(ClientWindState.windyPeriod()));
            lines.add("Pre-storm front: " + yesNo(ClientWindState.stormFront()));
            lines.add(String.format(Locale.ROOT, "Particle force: %.3f", ClientWindConfig.PARTICLE_WIND_FORCE.get()));
            float threshold = ClientWindConfig.AMBIENT_MIN_WIND.get().floatValue();
            lines.add(String.format(Locale.ROOT, "Ambient min: %.3f  [%s]", threshold,
                ClientWindConfig.AMBIENT_PARTICLES.get() && ClientWindState.localStrength() >= threshold ? "ACTIVE" : "inactive"));
            lines.add(String.format(Locale.ROOT, "Ambient density: x%.2f", ClientWindConfig.AMBIENT_DENSITY_MULTIPLIER.get()));
            lines.add(String.format(Locale.ROOT, "Ambient wind response: x%.2f", ClientWindConfig.AMBIENT_WIND_RESPONSE_SCALE.get()));
            lines.add("Falling leaves source: " + ClientWindConfig.FALLING_LEAVES_SOURCE.get().name());
            lines.add(String.format(Locale.ROOT, "Particular leaves density: x%.2f", ParticleWindFilter.particularLeafSpawnMultiplier()));
            lines.add(String.format(Locale.ROOT, "Ambient lift / sway: x%.2f / x%.2f", ClientWindConfig.AMBIENT_LIFT_MULTIPLIER.get(), ClientWindConfig.AMBIENT_SWAY_MULTIPLIER.get()));
            lines.add(String.format(Locale.ROOT, "Pretty Rain wind: %s  x%.2f", yesNo(ClientWindConfig.PRETTY_RAIN_COMPAT.get()), ClientWindConfig.PRETTY_RAIN_WIND_MULTIPLIER.get()));
            lines.add(String.format(Locale.ROOT, "Particle Rain wind: %s  x%.2f", yesNo(ClientWindConfig.PARTICLE_RAIN_COMPAT.get()), ClientWindConfig.PARTICLE_RAIN_WIND_MULTIPLIER.get()));
            lines.add("Ambient samples/tick: " + ClientWindConfig.AMBIENT_SAMPLES_PER_TICK.get());
            lines.add("Wind foliage: " + (ClientWindConfig.LEAF_PARTICLES_ENABLED.get() ? "ON" : "OFF"));
            lines.add(String.format(Locale.ROOT, "  Sources: canopy %d / plants %d / grass %d",
                WindLeafSpawner.debugSourceCount(), WindLeafSpawner.debugPlantSourceCount(), WindLeafSpawner.debugGrassSourceCount()));
            lines.add(String.format(Locale.ROOT, "  Ready (cooldown): C %d / P %d / G %d",
                WindLeafSpawner.debugReadyCanopyCount(), WindLeafSpawner.debugReadyPlantCount(), WindLeafSpawner.debugReadyGrassCount()));
            lines.add(String.format(Locale.ROOT, "  Emission: %.2f/tick", WindLeafSpawner.debugEmissionRate()));
            lines.add("Audio mode: " + ClientWindConfig.WIND_AUDIO_MODE.get());
            addSoundLines(lines, "Audio base", WindAudioManager.debugBaseClips());
            addSoundLines(lines, "Audio env", WindAudioManager.debugEnvironmentClips());
            addSoundLines(lines, "Audio gust", WindAudioManager.debugGustClip());
            lines.add(String.format(Locale.ROOT, "Audio gain / pitch: %.3f / %.3fx", WindAudioManager.debugBaseGain(), WindAudioManager.debugBasePitch()));
        }

        Font font = mc.font;
        int width = 0;
        for (String line : lines) width = Math.max(width, font.width(line));
        int height = lines.size() * LINE_HEIGHT + PADDING * 2 - 1;
        graphics.fill(X - PADDING, Y - PADDING, X + width + PADDING, Y - PADDING + height, 0xA0000000);

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFF78D7FF : 0xFFE8E8E8;
            graphics.drawString(font, lines.get(i), X, Y + i * LINE_HEIGHT, color, true);
        }
    }

    /** Break each active track and crossfade onto a separate line for a narrow HUD. */
    private static void addSoundLines(List<String> lines, String label, String tracks) {
        lines.add(label + ":");
        if (tracks == null || tracks.isBlank() || tracks.equals("—")) {
            lines.add("  —");
            return;
        }
        // Track debug strings are joined with " + "; splitting also preserves the environment
        // name and the per-track transition arrows (->) rather than truncating them.
        for (String track : tracks.split(" \\+ ")) {
            String[] clips = track.split(" -> ");
            for (int i = 0; i < clips.length; i++) {
                String prefix = i == 0 ? "  " : "    -> ";
                String item = clips[i];
                if (item.length() <= 43) {
                    lines.add(prefix + item);
                } else {
                    for (int at = 0; at < item.length(); at += 43)
                        lines.add(prefix + item.substring(at, Math.min(item.length(), at + 43)));
                }
            }
        }
    }

    private static String weatherName(Minecraft mc) {
        if (mc.level.isThundering()) return "THUNDER";
        if (mc.level.isRaining()) return "RAIN";
        return "CLEAR";
    }

    private static String yesNo(boolean value) {
        return value ? "YES" : "no";
    }

    private static String strengthName(float s) {
        if (s < 0.08f) return "CALM";
        if (s < 0.25f) return "LIGHT";
        if (s < 0.50f) return "BREEZY";
        if (s < 0.80f) return "STRONG";
        if (s < 1.15f) return "VERY STRONG";
        return "EXTREME";
    }

    private static String compass(float degrees) {
        String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = Math.floorMod(Math.round(degrees / 45.0f), 8);
        return names[index];
    }
}
