package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** A relative client ambience sample with independently controllable gain. */
final class WindSoundInstance extends AbstractTickableSoundInstance {
    private final String eventName;
    private final int durationTicks;
    private final int fadeTicks;
    private final long playbackStartTick;
    private int ageTicks;
    // A reversible fade-out envelope. Do not replace this with an instantaneous
    // fadeStopAt reset: fluctuating shelter used to revive almost-silent clips
    // at full amplitude in a single tick.
    private boolean stopRequested;
    private float stopEnvelope = 1.0f;
    private float requestedGain;
    private float shelterMultiplier = 1.0f;
    private float smoothedGain;
    private float unmutedVolume = 0.001f;
    private boolean fluidMuted;
    private boolean windDirectional;
    private boolean gustSweep;
    // A short, click-free diagnostic gate. This is separate from the normal
    // 2.5-second crossfade and shelter fade so A/B testing is easy to hear.
    private boolean debugAudible = true;
    private float debugGate = 1.0f;

    WindSoundInstance(String event, float pitch, float seconds, int fadeTicks, long playbackStartTick) {
        super(SoundEvent.createVariableRangeEvent(LivingWind.id(event)),
            SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.eventName = event;
        this.playbackStartTick = playbackStartTick;
        this.pitch = pitch;
        this.durationTicks = Math.max(20, Math.round(seconds * 20.0f / pitch));
        this.fadeTicks = Math.max(1, fadeTicks);
        this.volume = 0.001f;
        this.looping = false;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    void positionAt(net.minecraft.core.BlockPos pos) {
        relative = false;
        attenuation = SoundInstance.Attenuation.LINEAR;
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
    }

    /** OpenAL spatialization requires mono OGGs; stereo beds do not call this. */
    void enableDirectionalWind(boolean sweepPastListener) {
        this.windDirectional = true;
        this.gustSweep = sweepPastListener;
        // Keep a reliable, consistent gain while still allowing binaural panning.
        // Linear distance attenuation here causes loudness fluctuations while
        // walking and can make a moving source disappear unexpectedly.
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    /** Position upwind, following a moving listener without changing ambience loudness.
     * A short gust traverses gradually from upwind to just downwind.
     * Direction convention: 0 = wind blowing south (+Z), 90 = west (-X).
     */
    void updateWindPosition(double listenerX, double listenerY, double listenerZ, float directionDegrees) {
        if (!windDirectional) return;
        double angle = Math.toRadians(directionDegrees);
        double windX = -Math.sin(angle);
        double windZ = Math.cos(angle);
        float progress = Mth.clamp(ageTicks / (float) durationTicks, 0.0f, 1.0f);
        float smoothProgress = Mth.clamp((progress - 0.08f) / 0.84f, 0.0f, 1.0f);
        smoothProgress = smoothProgress * smoothProgress * (3.0f - 2.0f * smoothProgress);
        double distance = gustSweep ? -5.5 + 9.0 * smoothProgress : -5.5;
        this.x = listenerX + windX * distance;
        this.y = listenerY;
        this.z = listenerZ + windZ * distance;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    void setGain(float gain) {
        // A small amount of extra headroom lets very strong wind sound slightly
        // louder than the old slider-3 ceiling, without unbounded sound gain.
        requestedGain = Mth.clamp(gain, 0.0f, 3.25f);
        if (ageTicks == 0 && smoothedGain == 0.0f) smoothedGain = requestedGain;
    }

    void setDebugAudible(boolean audible) {
        debugAudible = audible;
    }

    boolean isDirectionalWind() {
        return windDirectional;
    }

    /** Reversible output mute: never restart the sample or reset its fade. */
    void setFluidMuted(boolean muted) {
        if (fluidMuted == muted) return;
        fluidMuted = muted;
        // Restore the last full, normally mixed volume on resurfacing, rather
        // than waiting for a new fade-in or starting a duplicate sound.
        volume = muted ? 0.0f : unmutedVolume;
    }

    void setShelterMultiplier(float value) {
        shelterMultiplier = Mth.clamp(value, 0.0f, 1.0f);
    }

    /** Immediate stop for world unload/disconnect; fluid muting does not stop clips. */
    void stopImmediately() {
        requestedGain = 0.0f;
        smoothedGain = 0.0f;
        volume = 0.0f;
        stop();
    }

    void fadeOut() {
        stopRequested = true;
    }

    // A recovering track ramps back up instead of instantly jumping to the
    // full remaining playback envelope. The same sample is reused safely.
    void cancelFadeOut() {
        stopRequested = false;
    }

    boolean fadingOut() {
        return stopRequested;
    }

    int remainingTicks() {
        return durationTicks - ageTicks;
    }

    long playbackStartTick() {
        return playbackStartTick;
    }

    String debugEventName() {
        return eventName;
    }

    float debugPitch() {
        return pitch;
    }

    float debugRequestedGain() {
        return requestedGain;
    }

    @Override
    public void tick() {
        ageTicks++;
        if (ageTicks >= durationTicks) {
            stop();
            return;
        }
        smoothedGain = Mth.lerp(0.15f, smoothedGain, requestedGain);
        // The envelope can move by at most 1/fadeTicks each tick in either
        // direction. In particular a fade cancellation cannot spike loudly.
        float fadeStep = 1.0f / fadeTicks;
        stopEnvelope = Mth.clamp(stopEnvelope + (stopRequested ? -fadeStep : fadeStep), 0.0f, 1.0f);
        if (stopRequested && stopEnvelope <= 0.0f) {
            stop();
            return;
        }
        float fadeIn = Mth.clamp(ageTicks / (float) fadeTicks, 0.0f, 1.0f);
        float fadeOut = Mth.clamp((durationTicks - ageTicks) / (float) fadeTicks, 0.0f, 1.0f);
        debugGate = Mth.lerp(0.35f, debugGate, debugAudible ? 1.0f : 0.0f);
        unmutedVolume = Math.max(0.0001f, smoothedGain * shelterMultiplier * fadeIn * fadeOut * stopEnvelope * debugGate);
        this.volume = fluidMuted ? 0.0f : unmutedVolume;
    }
}
