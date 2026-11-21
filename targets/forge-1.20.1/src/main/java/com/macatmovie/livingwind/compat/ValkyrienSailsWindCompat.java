package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

/**
 * Optional Valkyrien Sails wind-provider adapter (1.20.1 Forge/Fabric).
 *
 * Its sails run on a VS PHYSICS THREAD. Therefore a game-thread tick copies
 * primitive Living Wind values into immutable records; physics callbacks only
 * read these records. NEVER query SavedData, config specs, chunks or biome data
 * inside a Sails getter invoked by physics. No dependency on Valkyrien Sails.
 */
public final class ValkyrienSailsWindCompat {
    private record Sample(float strength, float direction, float multiplier,
                          boolean altitudeEnabled, int minY, int maxY,
                          float minScale, float maxScale) {
        float strengthAt(int y) {
            float scale = altitudeEnabled ? (maxY <= minY ? maxScale
                : Mth.lerp(Mth.clamp((y - minY) / (float) (maxY - minY), 0f, 1f), minScale, maxScale)) : 1f;
            return Mth.clamp(strength * scale * multiplier, 0f, 1f);
        }
    }

    private static final ConcurrentHashMap<ResourceKey<Level>, Sample> SERVER = new ConcurrentHashMap<>();
    private ValkyrienSailsWindCompat() {}

    /** Called only on the game's server thread, AFTER Living Wind's simulation tick. */
    public static void serverTick(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        if (!WindConfig.VALKYRIEN_SAILS_WIND_ENABLED.get()) {
            SERVER.remove(key);
            return;
        }
        WindSnapshot snapshot = WindApi.get(level); // includes dimension blacklist
        SERVER.put(key, new Sample(
            snapshot.strength(), snapshot.directionDegrees(),
            WindConfig.VALKYRIEN_SAILS_WIND_MULTIPLIER.get().floatValue(),
            WindConfig.ALTITUDE_SCALING_ENABLED.get() && key == Level.OVERWORLD,
            WindConfig.ALTITUDE_MIN_Y.get(), WindConfig.ALTITUDE_MAX_Y.get(),
            WindConfig.ALTITUDE_MIN_MULTIPLIER.get().floatValue(),
            WindConfig.ALTITUDE_MAX_MULTIPLIER.get().floatValue()));
    }

    /** Return null when disabled or not initialized, keeping Sails' own wind. */
    public static Float direction(Level level, Vec3 pos) {
        if (level == null || pos == null) return null;
        if (level.isClientSide()) {
            if (!ClientWindState.valkyrienSailsWindEnabled()) return null;
            return toSailsDegrees(ClientWindState.directionDegrees());
        }
        Sample s = SERVER.get(level.dimension());
        return s == null ? null : toSailsDegrees(s.direction);
    }

    public static Float strength(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        if (level.isClientSide()) {
            if (!ClientWindState.valkyrienSailsWindEnabled()) return null;
            float scaled = ClientWindState.strengthAt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5)
                * ClientWindState.valkyrienSailsWindMultiplier();
            return Mth.clamp(scaled, 0f, 1f);
        }
        Sample s = SERVER.get(level.dimension());
        return s == null ? null : s.strengthAt(pos.getY());
    }

    /** Living Wind: 0=+Z, 90=-X. Valkyrien Sails: 0=+X, 90=+Z. */
    public static float toSailsDegrees(float livingWindYaw) {
        return Mth.positiveModulo(livingWindYaw + 90f, 360f);
    }
}
