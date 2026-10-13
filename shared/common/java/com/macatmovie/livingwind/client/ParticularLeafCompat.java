package com.macatmovie.livingwind.client;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Soft Particular Reforged compatibility with no compile-time dependency.
 *
 * Particular's falling leaves are client particles created through ParticleEngine#createParticle.
 * The three-way foliage source setting suppresses Particular leaf creation at the
 * ParticleEngine entry point when Living Wind alone is selected (no particle is constructed).
 * When Particular is enabled, its leaf density can scale with Living Wind: weak wind
 * probabilistically suppresses spawns; strong wind duplicates accepted spawns.
 */
public final class ParticularLeafCompat {
    private static final ThreadLocal<SpawnContext> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CREATING_EXTRA = ThreadLocal.withInitial(() -> false);

    private ParticularLeafCompat() {}

    /** Return false only when the original Particular falling-leaf particle must not be constructed. */
    public static boolean beginCreate(
        ParticleOptions options,
        double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed
    ) {
        if (Boolean.TRUE.equals(CREATING_EXTRA.get())) return true;
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (!ParticleWindFilter.isParticularLeaf(id)) {
            CURRENT.remove();
            return true;
        }
        // The default is Living Wind only. Stop the Particular leaf before running
        // its particle factory and allocating an object, not just hiding it later.
        if (!ParticleWindFilter.shouldSpawnParticularLeaves()) {
            CURRENT.remove();
            return false;
        }
        float multiplier = ParticleWindFilter.particularLeafSpawnMultiplier(id);
        if (Math.abs(multiplier - 1.0f) < 1.0e-4f) {
            CURRENT.remove();
            return true;
        }
        CURRENT.set(new SpawnContext(id, options, x, y, z, xSpeed, ySpeed, zSpeed, multiplier));
        return true;
    }

    /** Return false to cancel ParticleEngine#add for this Particular leaf creation. */
    public static boolean allowCurrentAdd() {
        if (Boolean.TRUE.equals(CREATING_EXTRA.get())) return true;
        SpawnContext ctx = CURRENT.get();
        if (ctx == null) return true;

        float multiplier = ctx.multiplier;
        if (multiplier >= 1.0f) {
            ctx.accepted = true;
            return true;
        }

        boolean accept = ThreadLocalRandom.current().nextFloat() < Math.max(0.0f, multiplier);
        ctx.accepted = accept;
        return accept;
    }

    public static void finishCreate(ParticleEngine engine, @Nullable Particle original) {
        if (Boolean.TRUE.equals(CREATING_EXTRA.get())) return;
        SpawnContext ctx = CURRENT.get();
        CURRENT.remove();
        if (ctx == null || !ctx.accepted || original == null || ctx.multiplier <= 1.0f) return;

        float extraExpected = ctx.multiplier - 1.0f;
        int extras = Mth.floor(extraExpected);
        float fraction = extraExpected - extras;
        if (ThreadLocalRandom.current().nextFloat() < fraction) extras++;
        extras = Mth.clamp(extras, 0, 7); // hard safety cap even if a user sets an extreme config value
        if (extras <= 0) return;

        int tint = sourceLeafColor(ctx.x, ctx.y, ctx.z);
        float red = ((tint >> 16) & 0xFF) / 255.0f;
        float green = ((tint >> 8) & 0xFF) / 255.0f;
        float blue = (tint & 0xFF) / 255.0f;

        CREATING_EXTRA.set(true);
        try {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < extras; i++) {
                double jitterX = (random.nextDouble() - 0.5) * 0.22;
                double jitterY = (random.nextDouble() - 0.5) * 0.06;
                double jitterZ = (random.nextDouble() - 0.5) * 0.22;
                Particle extra = engine.createParticle(
                    ctx.options,
                    ctx.x + jitterX,
                    ctx.y + jitterY,
                    ctx.z + jitterZ,
                    ctx.xSpeed,
                    ctx.ySpeed,
                    ctx.zSpeed
                );
                if (extra != null) extra.setColor(red, green, blue);
            }
        } finally {
            CREATING_EXTRA.set(false);
        }
    }

    private static int sourceLeafColor(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return 0xFFFFFF;

        // Particular spawns a leaf just below the source leaves block (y = blockY - 0.05).
        BlockPos source = BlockPos.containing(x, y + 0.10, z);
        BlockState state = level.getBlockState(source);
        // Duplicated Particular leaves should inherit the same modded/biome foliage
        // tint rules as Living Wind's native leaves, not just hard-coded tint index 0.
        return WindFoliageColorCache.leafColor(state, level, source, level.getRandom(),
            com.macatmovie.livingwind.config.ClientWindConfig.LEAF_TEXTURE_COLORS.get());
    }

    private static final class SpawnContext {
        final ResourceLocation id;
        final ParticleOptions options;
        final double x;
        final double y;
        final double z;
        final double xSpeed;
        final double ySpeed;
        final double zSpeed;
        final float multiplier;
        boolean accepted = true;

        SpawnContext(
            ResourceLocation id,
            ParticleOptions options,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            float multiplier
        ) {
            this.id = id;
            this.options = options;
            this.x = x;
            this.y = y;
            this.z = z;
            this.xSpeed = xSpeed;
            this.ySpeed = ySpeed;
            this.zSpeed = zSpeed;
            this.multiplier = multiplier;
        }
    }
}
