package com.macatmovie.livingwind.network;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.resources.ResourceLocation;
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
    float altitudeMaxMultiplier,
    float treasureBalloonsWindMultiplier,
    float alekiShipsWindScale,
    boolean entityWindPlayers,
    float entityWindClearWeatherMultiplier,
    float entityWindRainMultiplier,
    float entityWindThunderMultiplier,
    float entityWindPlayerMultiplier,
    float entityWindPlayerGroundedMultiplier,
    boolean entityWindBoats,
    float entityWindBoatMultiplier,
    boolean gliderWindEnabled,
    float gliderWindInfluence,
    ResourceLocation dimensionId
) {
    public static WindSyncPayload decode(FriendlyByteBuf buf) {
        return new WindSyncPayload(
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readVarInt(), buf.readVarLong(), buf.readBoolean(),
            buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readFloat(), buf.readBoolean(), buf.readFloat(), buf.readResourceLocation()
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
        buf.writeFloat(treasureBalloonsWindMultiplier);
        buf.writeFloat(alekiShipsWindScale);
        buf.writeBoolean(entityWindPlayers);
        buf.writeFloat(entityWindClearWeatherMultiplier);
        buf.writeFloat(entityWindRainMultiplier);
        buf.writeFloat(entityWindThunderMultiplier);
        buf.writeFloat(entityWindPlayerMultiplier);
        buf.writeFloat(entityWindPlayerGroundedMultiplier);
        buf.writeBoolean(entityWindBoats);
        buf.writeFloat(entityWindBoatMultiplier);
        buf.writeBoolean(gliderWindEnabled);
        buf.writeFloat(gliderWindInfluence);
        buf.writeResourceLocation(dimensionId);
    }

    public static WindSyncPayload from(WindSnapshot s, boolean windDisabled, ResourceLocation dimensionId) {
        int flags = (s.windyPeriod() ? 1 : 0) | (s.stormFront() ? 2 : 0)
            | (WindConfig.CREATE_WINDMILLS_ENABLED.get() ? 4 : 0)
            | (WindConfig.SMALL_SHIPS_WIND_ENABLED.get() ? 16 : 0)
            | (WindConfig.TREASURE_BALLOONS_WIND_ENABLED.get() ? 32 : 0)
            | (WindConfig.ALEKI_SHIPS_WIND_ENABLED.get() ? 128 : 0)
            | (windDisabled ? 64 : 0)
            | (WindConfig.ENTITY_WIND_CHECK_SHELTER.get() ? 256 : 0);
        return new WindSyncPayload(
            s.strength(), s.directionDegrees(), s.turbulence(), s.directionInstability(),
            s.gustStrength(), flags, s.gameTime(),
            WindConfig.ALTITUDE_SCALING_ENABLED.get(),
            WindConfig.ALTITUDE_MIN_Y.get(),
            WindConfig.ALTITUDE_MAX_Y.get(),
            WindConfig.ALTITUDE_MIN_MULTIPLIER.get().floatValue(),
            WindConfig.ALTITUDE_MAX_MULTIPLIER.get().floatValue(),
            WindConfig.TREASURE_BALLOONS_WIND_MULTIPLIER.get().floatValue(),
            WindConfig.ALEKI_SHIPS_WIND_SCALE.get().floatValue(),
            WindConfig.ENTITY_WIND_ENABLED.get() && WindConfig.ENTITY_WIND_PLAYERS.get(),
            WindConfig.ENTITY_WIND_CLEAR_WEATHER_MULTIPLIER.get().floatValue(),
            WindConfig.ENTITY_WIND_RAIN_MULTIPLIER.get().floatValue(),
            WindConfig.ENTITY_WIND_THUNDER_MULTIPLIER.get().floatValue(),
            WindConfig.ENTITY_WIND_PLAYER_MULTIPLIER.get().floatValue(),
            WindConfig.ENTITY_WIND_PLAYER_GROUNDED_MULTIPLIER.get().floatValue(),
            WindConfig.ENTITY_WIND_ENABLED.get() && WindConfig.ENTITY_WIND_BOATS.get(),
            WindConfig.ENTITY_WIND_BOAT_MULTIPLIER.get().floatValue(),
            WindConfig.GLIDER_WIND_ENABLED.get(),
            WindConfig.GLIDER_WIND_INFLUENCE.get().floatValue(),
            dimensionId
        );
    }

    public boolean windDisabled() { return (flags & 64) != 0; }
    public boolean entityWindCheckShelter() { return (flags & 256) != 0; }
    public boolean windyPeriod() { return (flags & 1) != 0; }
    public boolean stormFront() { return (flags & 2) != 0; }
    public boolean createWindmillsEnabled() { return (flags & 4) != 0; }
    public boolean treasureBalloonsWindEnabled() { return (flags & 32) != 0; }
    public boolean smallShipsWindEnabled() { return (flags & 16) != 0; }
    public boolean alekiShipsWindEnabled() { return (flags & 128) != 0; }
}
