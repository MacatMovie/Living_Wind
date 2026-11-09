package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Client-only wind sound mixer. No second wind simulation: all intensity comes from
 * the synchronized, altitude- and shelter-adjusted ClientWindState.localStrength().
 * Short non-looping recordings overlap near their ends and receive tiny pitch variation.
 */
public final class WindAudioManager {
    private static final int CROSSFADE_TICKS = 50;
    // Minecraft can discard playing sounds during resource/audio reloads without
    // ticking our sound instances to completion. Check the engine occasionally,
    // not every tick, and allow new sounds time to enter the playback queue.
    private static final int PLAYBACK_CHECK_INTERVAL_TICKS = 40;
    private static final int PLAYBACK_START_GRACE_TICKS = 20;
    // The default 1.0 wind volume now has the gain of the previous 0.7 setting.
    // Keep this multiplier in the common mixer so it applies to both loaders,
    // directional audio, gusts and environmental accents without touching assets.
    private static final float INTERNAL_VOLUME_SCALE = 11.34f;
    // Wind *sound* intensity is deliberately gentler than the physical wind.
    // Gentle/moderate beds dominate ordinary weather; strong/extreme recordings
    // are reserved for the highest physical wind strengths.
    // Do not change ClientWindState: particles, entities and API stay unmodified.
    // With five genuinely gentle recordings, restore a little moderate presence
    // compared with the extra-soft 0.45 calibration, while preserving the
    // delayed strong/extreme thresholds. At strength 1.0, gentle still dominates.
    private static final float SOUND_STRENGTH_MULTIPLIER = 0.50f;
    private static final Random RANDOM = new Random();

    private enum Bed {
        GENTLE("gentle", 5, 12.08f),
        MODERATE("moderate", 5, 12.08f),
        STRONG("strong", 5, 12.08f),
        EXTREME("extreme", 3, 12.08f),
        NETHER("nether", 7, 10.03f),
        MOUNTAIN("mountain", 9, 11.88f),
        SNOWY("snowy", 8, 12.08f),
        DESERT("desert", 8, 12.08f),
        // Quiet, single-channel components. Stereo is preserved in all main beds;
        // these samples supply subtle, real-world directionality, not the body of the wind.
        DIRECTIONAL("directional", 11, 12.08f),
        NETHER_DIRECTIONAL("nether_directional", 2, 12.08f);

        final String sound;
        final int variants;
        final float seconds;

        Bed(String sound, int variants, float seconds) {
            this.sound = sound;
            this.variants = variants;
            this.seconds = seconds;
        }

        String event(int index) {
            return "wind." + sound + "_" + String.format(Locale.ROOT, "%02d", index + 1);
        }

        float secondsFor(int index) {
            // Older Nether and mountain recordings were shorter than the new WAV set.
            if (this == NETHER && index >= 2) return 12.08f;
            if (this == MOUNTAIN && index >= 4) return 12.08f;
            return seconds;
        }
    }

    private enum Environment { NONE, MOUNTAIN, SNOWY, DESERT, OCEAN }

    private static final EnumMap<Bed, Track> TRACKS = new EnumMap<>(Bed.class);
    private static final int[] ACCENT_LAST = {-1, -1, -1, -1};
    private static final List<WindSoundInstance> liveAccents = new ArrayList<>();
    private static long tickCount;
    private static long nextGustTick;
    private static long nextLeavesTick;
    private static long nextMountainGustTick;
    private static long nextForestAccentTick;
    private static float previousGust;
    // Shelter samples can fluctuate sharply near walls/overhangs. Audio has
    // its own smoothing envelope so clips never chase those fluctuations.
    // This does not alter exposure used by particles, entities or the Wind API.
    private static float audioExposure = 1.0f;
    private static boolean audioExposureInitialized;
    private static Environment environment = Environment.NONE;
    private static boolean nearbyLeaves;
    private static boolean stormy;
    private static BlockPos leafSoundPosition;
    private static Level previousLevel;
    // Each directional track is a very quiet mono layer. The stereo ambience
    // remains listener-relative and wide, avoiding hard-panned constant wind.
    // The old 0.60 maximum was rarely reached, while the low-wind fade gate
    // almost silenced directional audio below physical wind strength 1.0.
    // This remains an accent, not a second full-strength wind bed.
    private static final float DIRECTIONAL_MAX_GAIN = 0.85f;

    static {
        for (Bed bed : Bed.values()) TRACKS.put(bed, new Track());
    }

    private WindAudioManager() {}

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        for (Track track : TRACKS.values()) {
            if (track.current != null) mc.getSoundManager().stop(track.current);
            if (track.next != null) mc.getSoundManager().stop(track.next);
            track.current = track.next = null;
            track.lastIndex = -1;
            track.lastEvent = null;
            track.targetGain = 0;
        }
        for (WindSoundInstance accent : liveAccents) mc.getSoundManager().stop(accent);
        liveAccents.clear();
        previousLevel = null;
        tickCount = nextGustTick = nextLeavesTick = nextMountainGustTick = nextForestAccentTick = 0;
        previousGust = 0;
        audioExposure = 1.0f;
        audioExposureInitialized = false;
        java.util.Arrays.fill(ACCENT_LAST, -1);
        nearbyLeaves = false;
        stormy = false;
        leafSoundPosition = null;
        environment = Environment.NONE;
    }

    public static void tick(ClientLevel level, Player player) {
        if (previousLevel != level) {
            reset();
            previousLevel = level;
        }
        tickCount++;
        liveAccents.removeIf(WindSoundInstance::isStopped);
        // Follow the player's location while staying physically upwind, so
        // turning the camera pans the quiet layer but running does not leave
        // a stationary wind emitter behind or produce distance-volume jumps.
        float direction = ClientWindState.directionDegrees();
        for (Track track : TRACKS.values()) {
            if (track.current != null) track.current.updateWindPosition(player.getX(), player.getEyeY(), player.getZ(), direction);
            if (track.next != null) track.next.updateWindPosition(player.getX(), player.getEyeY(), player.getZ(), direction);
        }
        for (WindSoundInstance sound : liveAccents) {
            sound.updateWindPosition(player.getX(), player.getEyeY(), player.getZ(), direction);
        }
        if (tickCount % PLAYBACK_CHECK_INTERVAL_TICKS == 0) {
            reconcilePlayback(Minecraft.getInstance());
        }

        boolean playOwn = ClientWindConfig.WIND_AUDIO_ENABLED.get()
            && ClientWindConfig.WIND_AUDIO_MODE.get() != ClientWindConfig.WindAudioMode.AMBIENT_SOUNDS
            && ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get()
                != ClientWindConfig.DynamicSurroundingsWindMode.DYNAMIC_SURROUNDINGS
            && ClientWindConfig.AMBI_WIND_MODE.get() != ClientWindConfig.AmbiWindMode.AMBI
            && ClientWindState.hasServerState();
        // Shelter readings often jump as a moving player crosses the edge of a
        // wall or overhang. Avoid abrupt reactivation of wind tracks by using
        // a single smoothed exposure for both the bed selection and loudness.
        float targetExposure = Mth.clamp(ClientWindState.exposure(), 0.0f, 1.0f);
        if (!audioExposureInitialized) {
            audioExposure = targetExposure;
            audioExposureInitialized = true;
        } else {
            audioExposure = Mth.lerp(0.12f, audioExposure, targetExposure);
        }
        float indoorVolume = Mth.lerp(1.0f - audioExposure,
            ClientWindConfig.INDOOR_AUDIO_ATTENUATION.get().floatValue(), 1.0f);
        float wind = Mth.clamp(ClientWindState.altitudeStrength() * audioExposure, 0.0f, 3.0f);
        // Only the selection/mixing of sound textures is shifted; the original
        // strength still governs loudness, physical gusts, and shelter exposure.
        float soundWind = soundIntensityFor(wind);
        // Keep the user's existing audio-volume slider calibration. A more
        // gradual physical-wind loudness curve handles the overly intense
        // breeze response without changing the saved slider values.
        float rawMaster = playOwn
            ? ClientWindConfig.WIND_AUDIO_VOLUME.get().floatValue() * INTERNAL_VOLUME_SCALE
                * windVolumeCurve(wind)
            : 0.0f;
        ClientWindConfig.WindAudioDebugMode audioTestMode = ClientWindConfig.WIND_AUDIO_DEBUG_MODE.get();
        // Only the isolated stereo mix changes; rawMaster still drives the
        // directional bed so DIRECTIONAL_ONLY remains audible for testing.
        float master = rawMaster * indoorVolume;
        float stereoMaster = audioTestMode == ClientWindConfig.WindAudioDebugMode.DIRECTIONAL_ONLY
            ? 0.0f : master;
        for (WindSoundInstance accent : liveAccents) accent.setShelterMultiplier(indoorVolume);
        boolean environmentalEnabled = playOwn && ClientWindConfig.WIND_AUDIO_ENVIRONMENTAL.get();

        if (!environmentalEnabled) {
            environment = Environment.NONE;
            nearbyLeaves = false;
            leafSoundPosition = null;
        } else if (tickCount % 40 == 1) {
            scanEnvironment(level, player);
        }

        boolean nether = level.dimension() == Level.NETHER;
        boolean overworld = level.dimension() == Level.OVERWORLD;
        stormy = overworld && (level.isRaining() || level.isThundering());
        float active = wind < 0.07f ? 0 : Mth.clamp((wind - 0.07f) / 0.22f, 0, 1);
        float gust = ClientWindState.gustStrength();

        // Sound-bed thresholds use acoustic rather than physical intensity.
        // Together with the progressive gain envelope, this avoids hearing
        // gale-force wind while particles only show a light breeze.
        // Gentle remains the main bed at 0.7-1.0 physical strength, giving the
        // airy recordings much more space before moderate textures enter.
        float gentle = stereoMaster * active * (1 - smooth(.38f, .95f, soundWind)) * 0.30f;
        // Bring strong recordings in a little earlier (physical wind ~1.3+),
        // while preserving gentle/moderate dominance up through 1.0.
        // Duck moderate as strong comes up, so this changes the character of
        // high wind instead of simply adding a second loud wind bed.
        float strongBlend = strongBlendFor(soundWind);
        float moderate = stereoMaster * active * smooth(.30f, .70f, soundWind)
            * (1 - smooth(1.05f, 1.44f, soundWind))
            * (1.0f - 0.62f * strongBlend) * 0.31f;
        // Strong/very-strong still share one capped volume budget. Extremely
        // turbulent recordings only take over at the highest wind strengths.
        float strongBase = stereoMaster * active * strongBlend * 0.34f;
        float extremeBlend = smooth(1.20f, 1.60f, soundWind);
        // The underlying sound instance caps its gain at 3.25. Limit the
        // shared strong/extreme budget BEFORE splitting it, otherwise both
        // beds can hit the cap independently and become unexpectedly loud.
        float strongBudget = Math.min(strongBase, 3.25f);
        float strong = strongBudget * (1.0f - 0.78f * extremeBlend);
        float extreme = strongBudget * 0.78f * extremeBlend;
        float netherGain = stereoMaster * active * (0.18f + .19f * smooth(.28f, 1.30f, soundWind));

        update(Bed.GENTLE, nether ? 0 : gentle);
        update(Bed.MODERATE, nether ? 0 : moderate);
        update(Bed.STRONG, nether ? 0 : strong);
        update(Bed.EXTREME, nether ? 0 : extreme);
        update(Bed.NETHER, nether ? netherGain : 0);

        // Two-layer spatial soundscape: high-quality stereo beds retain width;
        // the quiet mono wind direction layer gives turns some gentle panning.
        // The dynamic blend fades at low wind and respects shelter, audio modes
        // and the ordinary Minecraft Ambient/Master sliders.
        boolean directionalEnabled = isDirectionalAudioEnabled();
        if (audioTestMode == ClientWindConfig.WindAudioDebugMode.STEREO_ONLY) {
            directionalEnabled = false;
        }
        float spatialPreference = directionalEnabled
            ? ClientWindConfig.WIND_AUDIO_DIRECTIONAL_VOLUME.get().floatValue() : 0.0f;
        float spatialGain = directionalGainFor(rawMaster, active, soundWind,
            indoorVolume, spatialPreference);
        update(Bed.DIRECTIONAL, overworld ? spatialGain : 0.0f);
        update(Bed.NETHER_DIRECTIONAL, nether ? spatialGain * 0.75f : 0.0f);

        // Snow, desert and mountain beds are stereo too, so keep those in
        // STEREO_ONLY. Only the one-shot leaves/forest/mountain gust accents
        // are excluded from the controlled A/B comparison.
        boolean envAllowed = overworld && environmentalEnabled && stereoMaster > 0;
        boolean oneShotAccentsAllowed = envAllowed
            && audioTestMode == ClientWindConfig.WindAudioDebugMode.NORMAL;
        float envGain = stereoMaster * active * smooth(.20f, 1.05f, soundWind) * 0.14f;
        update(Bed.MOUNTAIN, envAllowed && environment == Environment.MOUNTAIN ? envGain : 0);
        update(Bed.SNOWY, envAllowed && environment == Environment.SNOWY ? envGain : 0);
        update(Bed.DESERT, envAllowed && environment == Environment.DESERT ? envGain : 0);

        if (master > 0 && audioTestMode != ClientWindConfig.WindAudioDebugMode.STEREO_ONLY
            && ClientWindConfig.WIND_AUDIO_GUSTS.get()
            && gust > 0.075f && previousGust <= 0.075f && tickCount >= nextGustTick) {
            float gustLevel = Mth.clamp(gust * 2.1f, 0.25f, 1.0f);
            // The upwind source is deliberately softer than the former centered
            // accent; the broad stereo beds remain the dominant wind sound.
            // A physical gust during gentle weather should not sound like a
            // storm gust, even if its normalized gust-strength value is high.
            float gustWeatherScale = 0.30f + 0.70f * smooth(.35f, 2.30f, wind);
            float gustVolume = directionalEnabled
                ? Math.min(1.15f, 0.105f * rawMaster * active * gustLevel)
                    * gustWeatherScale
                    * (0.45f + 0.55f * ClientWindConfig.WIND_AUDIO_DIRECTIONAL_VOLUME.get().floatValue())
                : 0.35f * rawMaster * active * gustLevel * gustWeatherScale;
            playAccent("gust", 11, 0, gustVolume);
            nextGustTick = tickCount + 75;
        }
        previousGust = gust;

        // A special recording with built-in leaf rustling is played only near
        // actual foliage, never in open plains or deserts. Use it sparingly so
        // the same 12-second recording doesn't create an obvious repeating loop.
        if (oneShotAccentsAllowed && nearbyLeaves && wind > 0.35f && wind < 1.90f
                && tickCount >= nextForestAccentTick) {
            playAccent("forest", 1, 3, Math.min(0.78f,
                0.050f * rawMaster * active * smooth(.35f, .95f, wind)));
            nextForestAccentTick = tickCount + 520 + RANDOM.nextInt(420);
        }
        boolean forestAccentPlaying = false;
        for (WindSoundInstance accent : liveAccents) {
            if (!accent.isStopped() && accent.debugEventName().equals("wind.forest_01")) {
                forestAccentPlaying = true;
                break;
            }
        }
        if (oneShotAccentsAllowed && nearbyLeaves && !forestAccentPlaying
                && wind > 0.20f && tickCount >= nextLeavesTick) {
            // Leaves are a close-range accent, not another wind bed. Cap them to
            // prevent unexpectedly loud localized bursts in dense forests.
            playAccent("leaves", 7, 1, Math.min(1.45f,
                0.12f * rawMaster * active * smooth(.20f, 1.30f, wind)));
            nextLeavesTick = tickCount + 130 + RANDOM.nextInt(150);
        }
        if (oneShotAccentsAllowed && environment == Environment.MOUNTAIN && soundWind > 1.05f
            && tickCount >= nextMountainGustTick) {
            playAccent("mountain_gust", 2, 2, 0.11f * rawMaster * smooth(1.05f, 1.5f, soundWind));
            nextMountainGustTick = tickCount + 170 + RANDOM.nextInt(150);
        }
        if (master == 0) {
            for (WindSoundInstance accent : liveAccents) accent.fadeOut();
        }
        applyDebugAudioIsolation(audioTestMode);
    }

    /**
     * On-the-fly A/B monitoring, without restarting the sound engine or
     * abruptly stopping a sample (which can cause clicks). The per-instance
     * gate makes the switch audible immediately even during long crossfades.
     */
    private static void applyDebugAudioIsolation(ClientWindConfig.WindAudioDebugMode mode) {
        for (var entry : TRACKS.entrySet()) {
            Bed bed = entry.getKey();
            boolean positional = bed == Bed.DIRECTIONAL || bed == Bed.NETHER_DIRECTIONAL;
            boolean audible = mode == ClientWindConfig.WindAudioDebugMode.NORMAL
                || (mode == ClientWindConfig.WindAudioDebugMode.DIRECTIONAL_ONLY && positional)
                || (mode == ClientWindConfig.WindAudioDebugMode.STEREO_ONLY && !positional);
            Track track = entry.getValue();
            if (track.current != null) track.current.setDebugAudible(audible);
            if (track.next != null) track.next.setDebugAudible(audible);
        }
        for (WindSoundInstance accent : liveAccents) {
            boolean audible = mode == ClientWindConfig.WindAudioDebugMode.NORMAL
                || (mode == ClientWindConfig.WindAudioDebugMode.DIRECTIONAL_ONLY && accent.isDirectionalWind());
            accent.setDebugAudible(audible);
        }
    }

    /**
     * Heal sound-engine reloads (shader/resource-pack changes, device resets).
     * Sound instances don't necessarily receive another tick after the engine
     * drops them, so isStopped() alone is insufficient. This is deliberately
     * limited to one check per two seconds, and never forces a replay of accents:
     * the existing gust/leaves scheduling remains in charge of those sounds.
     */
    private static void reconcilePlayback(Minecraft mc) {
        // When Minecraft's ambient or master volume is muted, missing sounds are
        // intentional; don't repeatedly attempt to restart muted ambience.
        if (mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0.0f
                || mc.options.getSoundSourceVolume(SoundSource.AMBIENT) <= 0.0f) return;

        SoundManager manager = mc.getSoundManager();
        for (Track track : TRACKS.values()) {
            if (playbackLost(track.current, manager)) track.current = null;
            if (playbackLost(track.next, manager)) track.next = null;
            // Keep the still-playing crossfade partner rather than adding
            // another sound on top of an already active replacement clip.
            if (track.current == null && track.next != null) {
                track.current = track.next;
                track.next = null;
            }
        }
        // Interrupted one-shot accents are never replayed early. Only forget
        // their abandoned handles so they cannot accumulate after sound reloads.
        liveAccents.removeIf(sound -> playbackLost(sound, manager));
    }

    private static boolean playbackLost(WindSoundInstance sound, SoundManager manager) {
        return sound != null && !sound.isStopped()
            && tickCount - sound.playbackStartTick() >= PLAYBACK_START_GRACE_TICKS
            && !manager.isActive(sound);
    }

    /**
     * Strong clips begin faintly around physical strength 1.3, grow audible
     * around 1.6-1.8, and become dominant above ~2.0. Keep this blend tied to
     * acoustic rather than physical strength, leaving particles untouched.
     */
    private static float strongBlendFor(float soundWind) {
        return smooth(.64f, 1.16f, soundWind);
    }

    /**
     * An audible but still secondary upwind mono layer. The previous low-wind
     * gate and 3%-of-master factor made it nearly impossible to hear over the
     * rich stereo beds. The broad stereo ambience and its master volume remain
     * unchanged, and this layer still uses the existing user-adjustable slider.
     */
    private static float directionalGainFor(float rawMaster, float active,
                                             float soundWind, float indoorVolume,
                                             float spatialPreference) {
        float base = Math.min(DIRECTIONAL_MAX_GAIN,
            rawMaster * active * (0.060f + 0.018f * smooth(.55f, 1.20f, soundWind)));
        return base * indoorVolume * spatialPreference * smooth(.08f, .40f, soundWind);
    }

    /** Changes only the soundscape's perceived strength, never the simulated wind. */
    private static float soundIntensityFor(float physicalStrength) {
        return Mth.clamp(physicalStrength * SOUND_STRENGTH_MULTIPLIER, 0.0f, 3.0f);
    }

    /**
     * Wind audio should have a gradual loudness progression, matching the
     * simulation's visual wind effects. Previous behavior was nearly at full
     * gain by physical strength 0.7-0.75. Keep breezes soft and let the volume
     * rise progressively through the upper half of the wind scale.
     *
     * Strength: 0.3 -> .31, 0.75 -> .57, 1.5 -> .73,
     *           2.5 -> .93, 3.0 -> 1.04.
     */
    private static float windVolumeCurve(float strength) {
        if (strength <= 0.30f) {
            return Mth.lerp(smooth(0.07f, 0.30f, strength), 0.10f, 0.31f);
        }
        if (strength <= 0.75f) {
            return Mth.lerp(smooth(0.30f, 0.75f, strength), 0.31f, 0.57f);
        }
        if (strength <= 1.50f) {
            return Mth.lerp(smooth(0.75f, 1.50f, strength), 0.57f, 0.73f);
        }
        if (strength <= 2.50f) {
            return Mth.lerp(smooth(1.50f, 2.50f, strength), 0.73f, 0.93f);
        }
        return Mth.lerp(smooth(2.50f, 3.00f, strength), 0.93f, 1.04f);
    }

    private static float smooth(float low, float high, float value) {
        float t = Mth.clamp((value - low) / (high - low), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static void scanEnvironment(ClientLevel level, Player player) {
        environment = Environment.NONE;
        nearbyLeaves = false;
        leafSoundPosition = null;
        if (level.dimension() != Level.OVERWORLD) return;

        BlockPos at = player.blockPosition();
        Biome biome = level.getBiome(at).value();
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        String path = id == null ? "" : id.getPath();

        // Climate plus broad path hints keep this useful for many modded biomes without a hard dependency.
        boolean desert = containsAny(path, "desert", "badlands", "mesa", "dunes");
        boolean ocean = containsAny(path, "ocean", "beach", "shore", "coast");
        boolean snow = biome.getBaseTemperature() < 0.15f || containsAny(path, "snow", "frozen", "ice_spikes");

        if (desert) environment = Environment.DESERT;
        else if (ocean) environment = Environment.OCEAN;
        else if (snow) environment = Environment.SNOWY;
        else if (at.getY() >= 152 && ClientWindState.exposure() > 0.40f) environment = Environment.MOUNTAIN;

        // Bounded, once-per-2s sampling; no full block-area scans on each client tick.
        for (int i = 0; i < 24; i++) {
            BlockPos sampled = at.offset(RANDOM.nextInt(15) - 7,
                RANDOM.nextInt(11) - 4, RANDOM.nextInt(15) - 7);
            if (!level.hasChunkAt(sampled)) continue;
            if (level.getBlockState(sampled).is(WindBlockTags.LEAF_EMITTERS)) {
                nearbyLeaves = true;
                leafSoundPosition = sampled.immutable();
                break;
            }
        }
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static void playAccent(String type, int count, int previousSlot, float volume) {
        if (volume < 0.012f) return;
        int selection = select(count, ACCENT_LAST[previousSlot]);
        ACCENT_LAST[previousSlot] = selection;
        String event = "wind." + type + "_" + String.format(Locale.ROOT, "%02d", selection + 1);
        float seconds;
        if (type.equals("leaves")) seconds = 6.08f;
        else if (type.equals("forest")) seconds = 12.08f;
        else if (type.equals("gust")) seconds = selection < 7 ? 4.00f : 6.08f;
        else seconds = selection == 0 ? 6.08f : 11.88f;

        WindSoundInstance sound = new WindSoundInstance(event, pitch(), seconds, 12, tickCount);
        if (type.equals("leaves") && leafSoundPosition != null) sound.positionAt(leafSoundPosition);
        else if (type.equals("gust") && isDirectionalAudioEnabled()) {
            sound.enableDirectionalWind(true);
            Player player = Minecraft.getInstance().player;
            if (player != null) sound.updateWindPosition(player.getX(), player.getEyeY(), player.getZ(), ClientWindState.directionDegrees());
        }
        sound.setGain(volume);
        // Keep one-shots in the same gently smoothed shelter mix as wind beds.
        float indoorVolume = Mth.lerp(1.0f - audioExposure,
            ClientWindConfig.INDOOR_AUDIO_ATTENUATION.get().floatValue(), 1.0f);
        sound.setShelterMultiplier(indoorVolume);
        Minecraft.getInstance().getSoundManager().play(sound);
        liveAccents.add(sound);
    }

    private static void update(Bed bed, float target) {
        Track t = TRACKS.get(bed);
        t.targetGain = target;
        if (t.current != null && t.current.isStopped()) {
            t.current = t.next;
            t.next = null;
        }
        if (t.next != null && t.next.isStopped()) t.next = null;

        if (target < 0.007f) {
            if (t.current != null) t.current.fadeOut();
            if (t.next != null) t.next.fadeOut();
            return;
        }

        // A previously muted wind bed may still be fading when exposure or wind
        // recovers. Reuse it instead of abandoning a still-playing clip and adding
        // an unintended extra copy on top of its fading audio.
        if (t.current != null) t.current.cancelFadeOut();
        if (t.next != null) t.next.cancelFadeOut();
        if (t.current == null) {
            t.current = start(bed, t);
            t.next = null;
        }
        t.current.setGain(target);
        if (t.next == null && t.current.remainingTicks() <= CROSSFADE_TICKS + 3) {
            t.next = start(bed, t);
        }
        if (t.next != null) t.next.setGain(target);
    }

    private static WindSoundInstance start(Bed bed, Track track) {
        String event;
        int next = bed == Bed.NETHER
            ? selectNether(track.lastIndex, soundIntensityFor(ClientWindState.localStrength()))
            : bed == Bed.DIRECTIONAL
                ? selectDirectional(track.lastIndex, soundIntensityFor(ClientWindState.localStrength()))
                : select(bed.variants, track.lastIndex);
        float seconds = bed.secondsFor(next);

        track.lastIndex = next;
        event = bed.event(next);
        track.lastEvent = event;

        WindSoundInstance sound = new WindSoundInstance(event, pitch(), seconds, CROSSFADE_TICKS, tickCount);
        if (bed == Bed.DIRECTIONAL || bed == Bed.NETHER_DIRECTIONAL) {
            sound.enableDirectionalWind(false);
            Player player = Minecraft.getInstance().player;
            if (player != null) sound.updateWindPosition(player.getX(), player.getEyeY(), player.getZ(), ClientWindState.directionDegrees());
        }
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    }

    private static boolean isDirectionalAudioEnabled() {
        return (ClientWindConfig.WIND_AUDIO_DIRECTIONAL_ENABLED.get()
                || ClientWindConfig.WIND_AUDIO_DEBUG_MODE.get() == ClientWindConfig.WindAudioDebugMode.DIRECTIONAL_ONLY)
            && ClientWindConfig.WIND_AUDIO_DIRECTIONAL_VOLUME.get().floatValue() > 0.001f;
    }

    private static int selectDirectional(int previous, float wind) {
        int[] variants;
        if (wind < 0.52f) variants = new int[] {0, 1, 2, 3, 4};
        else if (wind < 1.35f) variants = new int[] {5, 6, 7};
        else variants = new int[] {8, 9, 10};
        int selected = variants[RANDOM.nextInt(variants.length)];
        if (selected == previous && variants.length > 1) {
            for (int i = 0; i < variants.length; i++) {
                if (variants[i] == previous) {
                    selected = variants[(i + 1 + RANDOM.nextInt(variants.length - 1)) % variants.length];
                    break;
                }
            }
        }
        return selected;
    }

    /** Match new Nether recordings to wind strength; keep both originals in rotation. */
    private static int selectNether(int previous, float wind) {
        int[] choices;
        if (wind < 0.5f) choices = new int[] {2, 3, 0}; // light, heavy/humid air
        else if (wind < 1.25f) choices = new int[] {0, 1, 4, 5};
        else choices = new int[] {1, 5, 6}; // powerful hot wind
        int selected = choices[RANDOM.nextInt(choices.length)];
        if (choices.length > 1 && selected == previous) {
            int offset = 1 + RANDOM.nextInt(choices.length - 1);
            for (int i = 0; i < choices.length; i++) {
                if (choices[i] == previous) {
                    selected = choices[(i + offset) % choices.length];
                    break;
                }
            }
        }
        return selected;
    }

    private static int select(int count, int previous) {
        int i = RANDOM.nextInt(count);
        if (count > 1 && i == previous) i = (i + 1 + RANDOM.nextInt(count - 1)) % count;
        return i;
    }

    private static float pitch() {
        return 0.965f + RANDOM.nextFloat() * 0.070f;
    }

    /** Dominant/current base clips for the in-game debug HUD. */
    public static String debugBaseClips() {
        return debugTracks(Bed.GENTLE, Bed.MODERATE, Bed.STRONG, Bed.EXTREME, Bed.NETHER);
    }

    /** Current biome/environment classification and any environmental clips now audible. */
    public static String debugEnvironmentClips() {
        String tracks = debugTracks(Bed.MOUNTAIN, Bed.SNOWY, Bed.DESERT);
        String leaves = debugAccent("wind.leaves_");
        String forest = debugAccent("wind.forest_");
        String directional = debugTracks(Bed.DIRECTIONAL, Bed.NETHER_DIRECTIONAL);
        String audible = joinNonEmpty(joinNonEmpty(joinNonEmpty(tracks, directional), leaves), forest);
        return environment.name() + (audible.isEmpty() ? " / —" : " / " + audible);
    }

    /** Current general or mountain gust accent, if any. */
    public static String debugGustClip() {
        String normal = debugAccent("wind.gust_");
        String mountain = debugAccent("wind.mountain_gust_");
        String both = joinNonEmpty(normal, mountain);
        return both.isEmpty() ? "—" : both;
    }

    /** Directional-layer target level, for diagnosing spatial audio in the debug HUD. */
    public static float debugDirectionalGain() {
        return Math.max(TRACKS.get(Bed.DIRECTIONAL).targetGain,
                        TRACKS.get(Bed.NETHER_DIRECTIONAL).targetGain);
    }

    public static float debugBaseGain() {
        Track best = dominantBaseTrack();
        return best == null ? 0.0f : best.targetGain;
    }

    public static float debugBasePitch() {
        Track best = dominantBaseTrack();
        return best == null || best.current == null ? 1.0f : best.current.debugPitch();
    }

    private static Track dominantBaseTrack() {
        Track best = null;
        for (Bed bed : new Bed[]{Bed.GENTLE, Bed.MODERATE, Bed.STRONG, Bed.EXTREME, Bed.NETHER}) {
            Track t = TRACKS.get(bed);
            if (t.current != null && !t.current.isStopped() && (best == null || t.targetGain > best.targetGain)) best = t;
        }
        return best;
    }

    private static String debugTracks(Bed... beds) {
        List<String> active = new ArrayList<>();
        for (Bed bed : beds) {
            Track t = TRACKS.get(bed);
            if (t.targetGain < 0.007f || t.current == null || t.current.isStopped()) continue;
            String clip = shortName(t.current.debugEventName());
            if (t.next != null && !t.next.isStopped()) clip += " -> " + shortName(t.next.debugEventName());
            active.add(clip);
        }
        return active.isEmpty() ? "—" : String.join(" + ", active);
    }

    private static String debugAccent(String prefix) {
        for (int i = liveAccents.size() - 1; i >= 0; i--) {
            WindSoundInstance sound = liveAccents.get(i);
            if (!sound.isStopped() && sound.debugEventName().startsWith(prefix)) return shortName(sound.debugEventName());
        }
        return "";
    }

    private static String shortName(String event) {
        return event.startsWith("wind.") ? event.substring(5) : event;
    }

    private static String joinNonEmpty(String a, String b) {
        boolean ae = a == null || a.isEmpty() || a.equals("—");
        boolean be = b == null || b.isEmpty() || b.equals("—");
        if (ae && be) return "";
        if (ae) return b;
        if (be) return a;
        return a + " + " + b;
    }

    private static final class Track {
        WindSoundInstance current;
        WindSoundInstance next;
        int lastIndex = -1;
        String lastEvent;
        float targetGain;
    }
}
