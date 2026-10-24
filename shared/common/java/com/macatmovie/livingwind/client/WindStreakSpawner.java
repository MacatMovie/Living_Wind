package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.particle.WindStreakParticle;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** A few camera-near, purely client-side visual streaks. No entities, paths or trail particles. */
public final class WindStreakSpawner {
    private static float spawnCredit;
    private static ClientLevel previousLevel;

    private WindStreakSpawner() {}

    public static void reset() {
        previousLevel = null;
        spawnCredit = 0.0f;
        WindStreakParticle.resetActiveCount();
    }

    public static void tick(ClientLevel level, Player player) {
        if (previousLevel != level) {
            previousLevel = level;
            spawnCredit = 0.0f;
            WindStreakParticle.resetActiveCount();
        }
        if (!ClientWindConfig.WIND_STREAKS_ENABLED.get()) {
            spawnCredit = 0.0f;
            return;
        }

        float wind = ClientWindState.altitudeStrength();
        float minWind = ClientWindConfig.WIND_STREAKS_MIN_STRENGTH.get().floatValue();
        float density = ClientWindConfig.WIND_STREAKS_DENSITY.get().floatValue();
        int maxActive = ClientWindConfig.WIND_STREAKS_MAX_ACTIVE.get();
        if (wind < minWind || density <= 0.0f || maxActive <= 0) return;
        if (WindStreakParticle.activeCount() >= maxActive) return;

        // More scattered, longer-lived streaks: halve the previous spawn rate,
        // retaining the same minimum wind threshold and user density multiplier.
        // Rate/s at the default threshold (0.4), density (1):
        // wind 0.5 ~= 0.7, 1.0 ~= 1.8, 1.5 ~= 3.0, 2.0 ~= 4.1.
        // The accumulator avoids one-second bursts, especially at low densities.
        float aboveThreshold = Math.max(0.0f, wind - minWind);
        float streaksPerSecond = 0.5f * Mth.clamp(0.85f + 4.6f * aboveThreshold, 0.0f, 8.4f);
        spawnCredit = Math.min(1.7f, spawnCredit + density * streaksPerSecond / 20.0f);
        if (spawnCredit < 1.0f) return;
        spawnCredit -= 1.0f;

        RandomSource random = level.getRandom();
        Vec3 look = player.getLookAngle();
        double horizontal = Math.hypot(look.x, look.z);
        double forwardX = horizontal > 0.01 ? look.x / horizontal : 0.0;
        double forwardZ = horizontal > 0.01 ? look.z / horizontal : 1.0;
        // Remain in a forward-facing arc, but spread to the periphery and slightly
        // outside the view. Keep the original 6-20 block forward distance.
        double side = (random.nextDouble() - 0.5) * 31.0;
        double ahead = 6.0 + random.nextDouble() * 14.0;
        double x = player.getX() + forwardX * ahead - forwardZ * side;
        // Most streaks remain near eye level; some now pass well overhead.
        // A slightly deeper lower edge also helps at cliffs and overhangs.
        double vertical = random.nextDouble() < 0.78
            ? -4.5 + random.nextDouble() * 11.5
            : 7.0 + random.nextDouble() * 7.0;
        double y = player.getEyeY() + vertical;
        double z = player.getZ() + forwardZ * ahead + forwardX * side;
        BlockPos at = BlockPos.containing(x, y, z);
        if (level.isOutsideBuildHeight(at) || !level.hasChunkAt(at)) return;
        if (!level.getBlockState(at).isAir()) return;
        // Only one candidate per spawn, and only here; never inspect blocks in particle ticks.
        if (WindShelter.nativeEmitterExposureAt(level, x, y, z) < 0.75f) return;
        if (ParticleWaterProtection.isSubmergedAtEmitter(level, x, y, z)) return;
        // Respect vanilla graphics particle reduction (MINIMAL deliberately means none).
        if (Minecraft.getInstance().options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) return;
        level.addParticle(ModParticles.WIND_STREAK.get(), x, y, z, 0.0, 0.0, 0.0);
    }
}
