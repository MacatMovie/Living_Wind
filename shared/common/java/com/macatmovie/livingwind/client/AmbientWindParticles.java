package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.compat.SeasonalWindCompat;
import com.macatmovie.livingwind.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
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
        boolean nether = level.dimension() == Level.NETHER;
        boolean twilightForest = "twilightforest".equals(level.dimension().location().getNamespace());
        SeasonalWindCompat.Snapshot season = ClientWindConfig.SEASONAL_PARTICLES_ENABLED.get()
            ? SeasonalWindCompat.snapshot(level) : null;

        for (int i = 0; i < samples; i++) {
            // Most attempts sample exposed terrain/canopy tops. A smaller share samples the nearby
            // volume so leaf/plant particles can also originate from visible tree and plant faces.
            boolean surfaceSample = !nether && random.nextFloat() < 0.68f;
            // The Twilight Forest canopy is frequently much higher than the player.
            // Half its surface attempts use the ground heightmap, which ignores leaves.
            // The usual shelter check still prevents pixels inside enclosed buildings.
            boolean underCanopy = surfaceSample && twilightForest && random.nextBoolean();
            BlockPos pos = nether
                ? (i % 2 == 0 ? randomNetherSurfacePosition(level, origin, radius, random)
                    : randomNearbyPosition(level, origin, radius, random))
                : surfaceSample ? randomSurfacePosition(level, origin, radius, random, underCanopy)
                    : randomNearbyPosition(level, origin, radius, random);
            if (pos == null) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            EmitterKind kind = classify(state, nether);
            // Ecliptic's winter snow coverage can be a rendered overlay, not a
            // physical snow-layer block. Use its public API only for exposed
            // sampled surfaces and only in winter (no additional world searches).
            if (!nether && surfaceSample && season != null && season.ecliptic()
                    && season.season() == SeasonalWindCompat.Season.WINTER
                    && ClientWindConfig.SEASONAL_SNOW_ENABLED.get()
                    && SeasonalWindCompat.snowCoveredAt(level, state, pos)) {
                kind = EmitterKind.SNOW;
            }
            if (kind == EmitterKind.NONE) continue;
            // Keep the existing overworld eligibility exactly as it was.
            if (!nether && !surfaceSample && kind != EmitterKind.LEAF && kind != EmitterKind.GRASS) continue;

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
            if (season != null && !nether) {
                String category = switch (kind) {
                    case LEAF -> "canopy";
                    case GRASS -> "grass";
                    case SNOW -> ClientWindConfig.SEASONAL_SNOW_ENABLED.get() ? "snow" : "none";
                    default -> "none";
                };
                chance *= SeasonalWindCompat.particleMultiplier(season, category,
                    ClientWindConfig.SEASONAL_PARTICLES_INTENSITY.get());
            }
            if (random.nextFloat() > chance) continue;

            double[] spawn = spawnOnFace(pos, face, random);
            // Check the actual emission position before performing any potentially
            // expensive biome/texture color calculation. Unknown shelter is not
            // evidence of outside wind; underwater ambient motes never spawn.
            float exposure = WindShelter.nativeEmitterExposureAt(level, spawn[0], spawn[1], spawn[2]);
            if (exposure < 0.20f) continue;
            if (ParticleWaterProtection.isSubmergedAtEmitter(level, spawn[0], spawn[1], spawn[2])) continue;

            int color = kind == EmitterKind.SNOW ? 0xECF6FF
                : kind == EmitterKind.LEAF
                ? WindFoliageColorCache.leafColor(state, level, pos, random,
                    ClientWindConfig.LEAF_TEXTURE_COLORS.get())
                : emitterColor(level, pos, state);
            float brightness = 0.84f + random.nextFloat() * 0.25f;
            double red = Mth.clamp(((color >> 16) & 0xFF) / 255.0f * brightness, 0.0f, 1.0f);
            double green = Mth.clamp(((color >> 8) & 0xFF) / 255.0f * brightness, 0.0f, 1.0f);
            double blue = Mth.clamp((color & 0xFF) / 255.0f * brightness, 0.0f, 1.0f);
            level.addParticle(ModParticles.AMBIENT_PIXEL.get(), spawn[0], spawn[1], spawn[2], red, green, blue);
            // Real exposed snow only: reuse the same sampled surface and particle type,
            // with at most one extra tiny pixel per successful snow sample in a winter gust.
            if (season != null && kind == EmitterKind.SNOW
                    && ClientWindConfig.SEASONAL_SNOW_ENABLED.get()
                    && season.season() == SeasonalWindCompat.Season.WINTER
                    && wind > 0.85f && random.nextFloat() < 0.40f) {
                level.addParticle(ModParticles.AMBIENT_PIXEL.get(),
                    spawn[0] + (random.nextFloat() - 0.5f) * 0.6,
                    spawn[1] + random.nextFloat() * 0.65,
                    spawn[2] + (random.nextFloat() - 0.5f) * 0.6,
                    0.91, 0.95, 1.0);
            }
        }
    }

    private static BlockPos randomSurfacePosition(ClientLevel level, BlockPos origin, int radius,
                                                   RandomSource random, boolean underCanopy) {
        int x = origin.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = origin.getZ() + random.nextInt(radius * 2 + 1) - radius;
        BlockPos columnCheck = new BlockPos(x, origin.getY(), z);
        if (!level.hasChunkAt(columnCheck)) return null;

        int y = level.getHeight(underCanopy ? Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
            : Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (Math.abs(y - origin.getY()) > 16) return null;
        BlockPos pos = new BlockPos(x, y, z);
        return underCanopy || level.canSeeSky(pos.above()) ? pos : null;
    }

    /**
     * Nether terrain has no usable sky-facing heightmap. Try just a handful of
     * cells in one nearby column, prioritizing surfaces around player height.
     * Every read is chunk-guarded; sampling count remains the configured budget.
     */
    private static BlockPos randomNetherSurfacePosition(ClientLevel level, BlockPos origin,
                                                        int radius, RandomSource random) {
        int localRadius = Math.min(radius, 12);
        int x = origin.getX() + random.nextInt(localRadius * 2 + 1) - localRadius;
        int z = origin.getZ() + random.nextInt(localRadius * 2 + 1) - localRadius;
        int startY = origin.getY() + random.nextInt(7) + 1;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int step = 0; step < 9; step++) {
            probe.set(x, startY - step, z);
            if (level.isOutsideBuildHeight(probe) || !level.hasChunkAt(probe)) break;
            BlockState state = level.getBlockState(probe);
            if (classify(state, true) != EmitterKind.NETHER_DUST) continue;
            // This also prevents choosing blocks buried in a solid Nether wall.
            BlockPos above = probe.above();
            if (level.hasChunkAt(above) && level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                return probe.immutable();
            }
        }
        return null;
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

    private static EmitterKind classify(BlockState state, boolean nether) {
        AmbientEmitterBlocks.Kind configured = AmbientEmitterBlocks.resolve(state);
        if (configured != null) return EmitterKind.valueOf(configured.name());
        if (nether && state.is(WindBlockTags.NETHER_AMBIENT_EMITTERS)) return EmitterKind.NETHER_DUST;
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
        DUST(0.032f),
        NETHER_DUST(0.065f);

        final float baseChance;
        EmitterKind(float baseChance) {
            this.baseChance = baseChance;
        }
    }
}
