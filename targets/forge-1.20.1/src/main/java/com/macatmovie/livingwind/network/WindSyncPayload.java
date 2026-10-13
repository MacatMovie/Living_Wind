package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record WindSyncPayload(
    float strength,
    float directionDegrees,
    float turbulence,
    float directionInstability,
    float gustStrength,
    int flags,
    long gameTime,
    boolean altitudeScalingEnabled,
    int altitudeMinY,
    int altitudeMaxY,
    float altitudeMinMultiplier,
    float altitudeMaxMultiplier
) {
    public static WindSyncPayload decode(FriendlyByteBuf buf) {
        return new WindSyncPayload(
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readVarInt(), buf.readVarLong(), buf.readBoolean(),
            buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(strength);
        buf.writeFloat(directionDegrees);
        buf.writeFloat(turbulence);
        buf.writeFloat(directionInstability);
        buf.writeFloat(gustStrength);
        buf.writeVarInt(flags);
        buf.writeVarLong(gameTime);
        buf.writeBoolean(altitudeScalingEnabled);
        buf.writeVarInt(altitudeMinY);
        buf.writeVarInt(altitudeMaxY);
        buf.writeFloat(altitudeMinMultiplier);
        buf.writeFloat(altitudeMaxMultiplier);
    }

    public static WindSyncPayload from(WindSnapshot s) {
        int flags = (s.windyPeriod() ? 1 : 0) | (s.stormFront() ? 2 : 0)
            | (WindConfig.CREATE_WINDMILLS_ENABLED.get() ? 4 : 0);
        return new WindSyncPayload(
            s.strength(), s.directionDegrees(), s.turbulence(), s.directionInstability(),
            s.gustStrength(), flags, s.gameTime(),
            WindConfig.ALTITUDE_SCALING_ENABLED.get(),
            WindConfig.ALTITUDE_MIN_Y.get(),
            WindConfig.ALTITUDE_MAX_Y.get(),
            WindConfig.ALTITUDE_MIN_MULTIPLIER.get().floatValue(),
            WindConfig.ALTITUDE_MAX_MULTIPLIER.get().floatValue()
        );
    }

    public boolean windyPeriod() { return (flags & 1) != 0; }
    public boolean stormFront() { return (flags & 2) != 0; }
    public boolean createWindmillsEnabled() { return (flags & 4) != 0; }
}
