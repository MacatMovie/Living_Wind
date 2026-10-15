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
    // At the default slider value (1.0), moderate/strong winds approach the volume
    // that previously required slider 3.0. windVolumeCurve() quiets calm breezes
    // before this gain reaches sound instances; all beds and accents share it.
    private static final float INTERNAL_VOLUME_SCALE = 16.20f;
    private static final Random RANDOM = new Random();

    private enum Bed {
        GENTLE("gentle", 4, 10.03f),
        MODERATE("moderate", 4, 10.03f),
        STRONG("strong", 3, 10.03f),
        NETHER("nether", 2, 10.03f),
        MOUNTAIN("mountain", 4, 11.88f),
        SNOWY("snowy", 2, 10.03f),
        DESERT("desert", 2, 10.03f);

        final String sound;
        final int variants;
        final float seconds;

        Bed(String sound, int variants, float seconds) {
            this.sound = sound;
            this.variants = variants;
            this.seconds = seconds;
        }

        String event(int index) {
            // Exclude strong_03 permanently without renaming strong_04 (existing sound event IDs remain stable).
            int clipNumber = this == STRONG && index == 2 ? 4 : index + 1;
            return "wind." + sound + "_" + String.format(Locale.ROOT, "%02d", clipNumber);
        }
    }

    private enum Environment { NONE, MOUNTAIN, SNOWY, DESERT, OCEAN }

    private static final EnumMap<Bed, Track> TRACKS = new EnumMap<>(Bed.class);
    private static final int[] ACCENT_LAST = {-1, -1, -1};
    private static final List<WindSoundInstance> liveAccents = new ArrayList<>();
    private static long tickCount;
    private static long nextGustTick;
    private static long nextLeavesTick;
    private static long nextMountainGustTick;
    private static float previousGust;
    private static Environment environment = Environment.NONE;
    private static boolean nearbyLeaves;
    private static boolean stormy;
    private static BlockPos leafSoundPosition;
    private static Level previousLevel;

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
        tickCount = nextGustTick = nextLeavesTick = nextMountainGustTick = 0;
        previousGust = 0;
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
        if (tickCount % PLAYBACK_CHECK_INTERVAL_TICKS == 0) {
            reconcilePlayback(Minecraft.getInstance());
        }

        boolean playOwn = ClientWindConfig.WIND_AUDIO_ENABLED.get()
            && ClientWindConfig.WIND_AUDIO_MODE.get() != ClientWindConfig.WindAudioMode.AMBIENT_SOUNDS
            && ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get()
                != ClientWindConfig.DynamicSurroundingsWindMode.DYNAMIC_SURROUNDINGS
            && ClientWindState.hasServerState();
        float indoorVolume = Mth.lerp(1.0f - ClientWindState.exposure(),
            ClientWindConfig.INDOOR_AUDIO_ATTENUATION.get().floatValue(), 1.0f);
        float wind = Mth.clamp(ClientWindState.localStrength(), 0.0f, 3.0f);
        // Calibrated per-strength loudness, NOT a strength multiplier. This avoids
        // roaring ambience at 0.1-0.4 while preserving full loudness from ~0.75.
        float rawMaster = playOwn
            ? ClientWindConfig.WIND_AUDIO_VOLUME.get().floatValue() * INTERNAL_VOLUME_SCALE
                * windVolumeCurve(wind)
            : 0.0f;
        float master = rawMaster * indoorVolume;
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

        // Short crossfading ambience beds. Their amplitudes overlap at transition strengths.
        // Gentle winds no longer receive an additional +30% boost: the master strength
        // curve handles low-wind loudness consistently for beds and accents.
        float gentle = master * active * (1 - smooth(.30f, .90f, wind)) * 0.27f;
        float moderate = master * active * smooth(.23f, .67f, wind)
            * (1 - smooth(.85f, 1.60f, wind)) * 0.31f;
        float strong = master * active * smooth(.65f, 1.55f, wind) * 0.43f;
        float netherGain = master * active * (0.18f + .19f * smooth(.28f, 1.30f, wind));

        update(Bed.GENTLE, nether ? 0 : gentle);
        update(Bed.MODERATE, nether ? 0 : moderate);
        update(Bed.STRONG, nether ? 0 : strong);
        update(Bed.NETHER, nether ? netherGain : 0);

        boolean envAllowed = overworld && environmentalEnabled && master > 0;
        float envGain = master * active * smooth(.27f, 1.05f, wind) * 0.16f;
        update(Bed.MOUNTAIN, envAllowed && environment == Environment.MOUNTAIN ? envGain : 0);
        update(Bed.SNOWY, envAllowed && environment == Environment.SNOWY ? envGain : 0);
        update(Bed.DESERT, envAllowed && environment == Environment.DESERT ? envGain : 0);

        if (master > 0 && ClientWindConfig.WIND_AUDIO_GUSTS.get()
            && gust > 0.075f && previousGust <= 0.075f && tickCount >= nextGustTick) {
            playAccent("gust", 4, 0, 0.35f * rawMaster * active * Mth.clamp(gust * 2.1f, 0.25f, 1.0f));
            nextGustTick = tickCount + 75;
        }
        previousGust = gust;

        if (envAllowed && nearbyLeaves && wind > 0.20f && tickCount >= nextLeavesTick) {
            playAccent("leaves", 7, 1, 0.16f * rawMaster * active * smooth(.20f, 1.30f, wind));
            nextLeavesTick = tickCount + 130 + RANDOM.nextInt(150);
        }
        if (envAllowed && environment == Environment.MOUNTAIN && wind > 0.75f
            && tickCount >= nextMountainGustTick) {
            playAccent("mountain_gust", 2, 2, 0.11f * rawMaster * smooth(.75f, 1.6f, wind));
            nextMountainGustTick = tickCount + 170 + RANDOM.nextInt(150);
        }
        if (master == 0) {
            for (WindSoundInstance accent : liveAccents) accent.fadeOut();
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
     * Smooth three-stage sound gain: very quiet calm winds, an increasingly present
     * breeze, then full wind/storm loudness. No extra wind simulation or sampling.
     *
     * 0.00-0.40: 0.12 -> 0.43 (calm remains soft, even at the new 3x base gain)
     * 0.40-0.75: 0.43 -> 1.00 (quickly reaches the former slider-3 loudness)
     * 0.75+    : 1.00 -> 1.08 (subtle additional presence for very strong winds)
     */
    private static float windVolumeCurve(float strength) {
        if (strength <= 0.40f) {
            return Mth.lerp(smooth(0.07f, 0.40f, strength), 0.12f, 0.43f);
        }
        if (strength <= 0.75f) {
            return Mth.lerp(smooth(0.40f, 0.75f, strength), 0.43f, 1.00f);
        }
        return 1.00f + 0.08f * smooth(0.90f, 1.80f, strength);
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
        else if (type.equals("gust")) seconds = selection < 2 ? 4.0f : 6.04f;
        else seconds = selection == 0 ? 6.08f : 11.88f;

        WindSoundInstance sound = new WindSoundInstance(event, pitch(), seconds, 12, tickCount);
        if (type.equals("leaves") && leafSoundPosition != null) sound.positionAt(leafSoundPosition);
        sound.setGain(volume);
        float indoorVolume = Mth.lerp(1.0f - ClientWindState.exposure(),
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

        if (t.current == null || t.current.fadingOut()) {
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
        float seconds = bed.seconds;

        int next = select(bed.variants, track.lastIndex);
        track.lastIndex = next;
        event = bed.event(next);
        track.lastEvent = event;

        WindSoundInstance sound = new WindSoundInstance(event, pitch(), seconds, CROSSFADE_TICKS, tickCount);
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
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
        return debugTracks(Bed.GENTLE, Bed.MODERATE, Bed.STRONG, Bed.NETHER);
    }

    /** Current biome/environment classification and any environmental clips now audible. */
    public static String debugEnvironmentClips() {
        String tracks = debugTracks(Bed.MOUNTAIN, Bed.SNOWY, Bed.DESERT);
        String leaves = debugAccent("wind.leaves_");
        String audible = joinNonEmpty(tracks, leaves);
        return environment.name() + (audible.isEmpty() ? " / —" : " / " + audible);
    }

    /** Current general or mountain gust accent, if any. */
    public static String debugGustClip() {
        String normal = debugAccent("wind.gust_");
        String mountain = debugAccent("wind.mountain_gust_");
        String both = joinNonEmpty(normal, mountain);
        return both.isEmpty() ? "—" : both;
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
        for (Bed bed : new Bed[]{Bed.GENTLE, Bed.MODERATE, Bed.STRONG, Bed.NETHER}) {
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
