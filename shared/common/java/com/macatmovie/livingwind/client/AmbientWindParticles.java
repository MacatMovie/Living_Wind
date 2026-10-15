package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Cheap stochastic ambient emitters. We sample a handful of positions instead of scanning a cube
 * around the player. Emitted particles are tiny custom pixels, not vanilla block-breaking debris.
 */
public final class AmbientWindParticles {
    private static final Direction[] EXPOSED_FACES = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN
    };

    private AmbientWindParticles() {}

    public static void tick(ClientLevel level, Player player) {
        if (!ClientWindConfig.AMBIENT_PARTICLES.get()) return;

        float wind = ClientWindState.altitudeStrength();
        float minWind = ClientWindConfig.AMBIENT_MIN_WIND.get().floatValue();
        if (wind < minWind) return;

        int samples = ClientWindConfig.AMBIENT_SAMPLES_PER_TICK.get();
        if (samples <= 0) return;

        RandomSource random = level.getRandom();
        int radius = ClientWindConfig.AMBIENT_RADIUS.get();
        BlockPos origin = player.blockPosition();

        for (int i = 0; i < samples; i++) {
            // Most attempts sample exposed terrain/canopy tops. A smaller share samples the nearby
            // volume so leaf/plant particles can also originate from visible tree and plant faces.
            boolean surfaceSample = random.nextFloat() < 0.68f;
            BlockPos pos = surfaceSample
                ? randomSurfacePosition(level, origin, radius, random)
                : randomNearbyPosition(level, origin, radius, random);
            if (pos == null) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            EmitterKind kind = classify(state);
            if (kind == EmitterKind.NONE) continue;
            if (!surfaceSample && kind != EmitterKind.LEAF && kind != EmitterKind.GRASS) continue;

            Direction face = surfaceSample ? Direction.UP : findExposedFace(level, pos, random);
            if (face == null) continue;

            // Ambient activity scales strongly with wind. Below minWind it is completely disabled;
            // just above the threshold it is intentionally sparse, while strong wind can produce
            // roughly 3x the baseline chance without increasing terrain-sampling work.
            float normalized = Mth.clamp(
                (wind - minWind) / Math.max(0.10f, 1.35f - minWind), 0.0f, 1.0f
            );
            float windActivity = (float) Math.pow(normalized, 1.45);
            float density = ClientWindConfig.AMBIENT_DENSITY_MULTIPLIER.get().floatValue();
            float chance = kind.baseChance * density * (0.02f + windActivity * 2.98f);
            if (random.nextFloat() > chance) continue;

            double[] spawn = spawnOnFace(pos, face, random);
            // Check the actual emission position before performing any potentially
            // expensive biome/texture color calculation. Unknown shelter is not
            // evidence of outside wind; underwater ambient motes never spawn.
            float exposure = WindShelter.nativeEmitterExposureAt(level, spawn[0], spawn[1], spawn[2]);
            if (exposure < 0.20f) continue;
            if (ParticleWaterProtection.isSubmergedAtEmitter(level, spawn[0], spawn[1], spawn[2])) continue;

            int color = kind == EmitterKind.LEAF
                ? WindFoliageColorCache.leafColor(state, level, pos, random,
                    ClientWindConfig.LEAF_TEXTURE_COLORS.get())
                : emitterColor(level, pos, state);
            float brightness = 0.84f + random.nextFloat() * 0.25f;
            double red = Mth.clamp(((color >> 16) & 0xFF) / 255.0f * brightness, 0.0f, 1.0f);
            double green = Mth.clamp(((color >> 8) & 0xFF) / 255.0f * brightness, 0.0f, 1.0f);
            double blue = Mth.clamp((color & 0xFF) / 255.0f * brightness, 0.0f, 1.0f);
            level.addParticle(ModParticles.AMBIENT_PIXEL.get(), spawn[0], spawn[1], spawn[2], red, green, blue);
        }
    }

    private static BlockPos randomSurfacePosition(ClientLevel level, BlockPos origin, int radius, RandomSource random) {
        int x = origin.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = origin.getZ() + random.nextInt(radius * 2 + 1) - radius;
        BlockPos columnCheck = new BlockPos(x, origin.getY(), z);
        if (!level.hasChunkAt(columnCheck)) return null;

        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (Math.abs(y - origin.getY()) > 16) return null;
        BlockPos pos = new BlockPos(x, y, z);
        return level.canSeeSky(pos.above()) ? pos : null;
    }

    private static BlockPos randomNearbyPosition(ClientLevel level, BlockPos origin, int radius, RandomSource random) {
        int localRadius = Math.min(radius, 14);
        int x = origin.getX() + random.nextInt(localRadius * 2 + 1) - localRadius;
        int y = origin.getY() + random.nextInt(21) - 7;
        int z = origin.getZ() + random.nextInt(localRadius * 2 + 1) - localRadius;
        BlockPos pos = new BlockPos(x, y, z);
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) ? pos : null;
    }

    private static Direction findExposedFace(ClientLevel level, BlockPos pos, RandomSource random) {
        int start = random.nextInt(EXPOSED_FACES.length);
        for (int i = 0; i < EXPOSED_FACES.length; i++) {
            Direction direction = EXPOSED_FACES[(start + i) % EXPOSED_FACES.length];
            BlockPos beside = pos.relative(direction);
            BlockState neighbor = level.getBlockState(beside);
            if (neighbor.isAir() || neighbor.getCollisionShape(level, beside).isEmpty()) return direction;
        }
        return null;
    }

    private static double[] spawnOnFace(BlockPos pos, Direction face, RandomSource random) {
        double x = pos.getX() + 0.5 + face.getStepX() * 0.515;
        double y = pos.getY() + 0.5 + face.getStepY() * 0.515;
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.515;
        double a = (random.nextDouble() - 0.5) * 0.78;
        double b = (random.nextDouble() - 0.5) * 0.78;

        if (face.getStepX() != 0) {
            y += a;
            z += b;
        } else if (face.getStepY() != 0) {
            x += a;
            z += b;
        } else {
            x += a;
            y += b;
        }
        return new double[] {x, y, z};
    }

    private static int emitterColor(ClientLevel level, BlockPos pos, BlockState state) {
        // Minecraft's own particle/block color lookup includes biome-dependent tint where a block
        // supplies one (notably grass and foliage) and falls back appropriately for untinted blocks.
        int color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
        if (color == -1) color = state.getMapColor(level, pos).col;
        return color & 0xFFFFFF;
    }

    private static EmitterKind classify(BlockState state) {
        if (state.is(WindBlockTags.LEAF_EMITTERS)) return EmitterKind.LEAF;
        // The default grass tag explicitly contains grass_block, short_grass, tall_grass, fern and large_fern.
        // WORLD_SURFACE sampling therefore naturally gives dense fields more eligible emitters.
        if (state.is(WindBlockTags.GRASS_EMITTERS)) return EmitterKind.GRASS;
        if (state.is(WindBlockTags.SNOW_EMITTERS)) return EmitterKind.SNOW;
        if (state.is(WindBlockTags.SAND_EMITTERS)) return EmitterKind.SAND;
        if (state.is(WindBlockTags.DUST_EMITTERS)) return EmitterKind.DUST;
        return EmitterKind.NONE;
    }

    private enum EmitterKind {
        NONE(0.0f),
        LEAF(0.075f),
        GRASS(0.055f),
        SNOW(0.065f),
        SAND(0.045f),
        DUST(0.032f);

        final float baseChance;
        EmitterKind(float baseChance) {
            this.baseChance = baseChance;
        }
    }
}
