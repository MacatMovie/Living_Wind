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
    private int fadeStopAt = Integer.MAX_VALUE;
    private float requestedGain;
    private float shelterMultiplier = 1.0f;
    private float smoothedGain;

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

    void setShelterMultiplier(float value) {
        shelterMultiplier = Mth.clamp(value, 0.0f, 1.0f);
    }

    void fadeOut() {
        fadeStopAt = Math.min(fadeStopAt, ageTicks + fadeTicks);
    }

    boolean fadingOut() {
        return fadeStopAt != Integer.MAX_VALUE;
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
        if (ageTicks >= durationTicks || ageTicks >= fadeStopAt) {
            stop();
            return;
        }
        smoothedGain = Mth.lerp(0.15f, smoothedGain, requestedGain);
        float fadeIn = Mth.clamp(ageTicks / (float) fadeTicks, 0.0f, 1.0f);
        float fadeOut = Mth.clamp((durationTicks - ageTicks) / (float) fadeTicks, 0.0f, 1.0f);
        if (fadeStopAt != Integer.MAX_VALUE) {
            fadeOut = Math.min(fadeOut, Mth.clamp((fadeStopAt - ageTicks) / (float) fadeTicks, 0.0f, 1.0f));
        }
        this.volume = Math.max(0.0001f, smoothedGain * shelterMultiplier * fadeIn * fadeOut);
    }
}
