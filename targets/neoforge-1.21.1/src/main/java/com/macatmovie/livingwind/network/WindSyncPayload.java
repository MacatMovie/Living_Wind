package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
) implements CustomPacketPayload {
    public static final Type<WindSyncPayload> TYPE = new Type<>(LivingWind.id("wind_sync"));

    public static final StreamCodec<ByteBuf, WindSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public WindSyncPayload decode(ByteBuf buf) {
            return new WindSyncPayload(
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), buf.readBoolean(),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                buf.readFloat(), buf.readFloat()
            );
        }

        @Override
        public void encode(ByteBuf buf, WindSyncPayload value) {
            buf.writeFloat(value.strength());
            buf.writeFloat(value.directionDegrees());
            buf.writeFloat(value.turbulence());
            buf.writeFloat(value.directionInstability());
            buf.writeFloat(value.gustStrength());
            ByteBufCodecs.VAR_INT.encode(buf, value.flags());
            ByteBufCodecs.VAR_LONG.encode(buf, value.gameTime());
            buf.writeBoolean(value.altitudeScalingEnabled());
            ByteBufCodecs.VAR_INT.encode(buf, value.altitudeMinY());
            ByteBufCodecs.VAR_INT.encode(buf, value.altitudeMaxY());
            buf.writeFloat(value.altitudeMinMultiplier());
            buf.writeFloat(value.altitudeMaxMultiplier());
        }
    };

    public static WindSyncPayload from(WindSnapshot s) {
        int flags = (s.windyPeriod() ? 1 : 0) | (s.stormFront() ? 2 : 0)
            | (WindConfig.CREATE_WINDMILLS_ENABLED.get() ? 4 : 0)
            | (WindConfig.HIGH_SEAS_WIND_ENABLED.get() ? 8 : 0)
            | (WindConfig.SMALL_SHIPS_WIND_ENABLED.get() ? 16 : 0);
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
    public boolean smallShipsWindEnabled() { return (flags & 16) != 0; }
    /** Server-authoritative toggle, carried in existing flags without changing the packet wire format. */
    public boolean highSeasWindEnabled() { return (flags & 8) != 0; }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
