package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.registry.ModParticles;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Bounded native-particle vegetation emitter sampler.
 *
 * A global particle budget alone causes isolated flowers and cacti to act like particle
 * fountains: if most cached candidates are unreachable, one block can take the entire budget.
 * Here the global budget sets only an upper limit. Sources are visited in rotating order and
 * each physical emitter has its own cooldown. Sparse categories receive smaller shares, so a
 * single plant cannot absorb the budget that would otherwise have been spread over a meadow.
 *
 * This is an independent design; no Weather2 source or assets are incorporated.
 */
public final class WindLeafSpawner {
    private static final ArrayList<Emitter> CANOPY = new ArrayList<>();
    private static final ArrayList<Emitter> PLANTS = new ArrayList<>();
    private static final ArrayList<Emitter> GRASS = new ArrayList<>();
    private static final int CANOPY_CAP = 176;
    private static final int PLANT_CAP = 128;
    private static final int GRASS_CAP = 160;
    private static final Direction[] FACES = Direction.values();
    private static final int SEARCH_LIMIT = 24;
    private static ClientLevel previousLevel;
    private static int ticks;
    private static int canopyCursor;
    private static int plantCursor;
    private static int grassCursor;
    private static float spawnCredit;
    private static float currentRate;
    private static BlockPos lastSurveyCenter;
    private static int neighborhoodSurveys;

    private enum Kind { NONE, CANOPY, PLANT, GRASS }

    private static final class Emitter {
        final BlockPos pos;
        int nextEmissionTick;

        Emitter(BlockPos pos) {
            this.pos = pos.immutable();
        }
    }

    private WindLeafSpawner() {}

    public static void reset() {
        CANOPY.clear();
        PLANTS.clear();
        GRASS.clear();
        previousLevel = null;
        ticks = 0;
        canopyCursor = 0;
        plantCursor = 0;
        grassCursor = 0;
        spawnCredit = 0;
        currentRate = 0;
        lastSurveyCenter = null;
        neighborhoodSurveys = 0;
    }

    public static int debugSourceCount() { return CANOPY.size(); }
    public static int debugPlantSourceCount() { return PLANTS.size(); }
    public static int debugGrassSourceCount() { return GRASS.size(); }
    public static int debugReadyCanopyCount() { return readyCount(CANOPY); }
    public static int debugReadyPlantCount() { return readyCount(PLANTS); }
    public static int debugReadyGrassCount() { return readyCount(GRASS); }
    public static float debugEmissionRate() { return currentRate; }

    private static int readyCount(ArrayList<Emitter> emitters) {
        int ready = 0;
        for (Emitter emitter : emitters) if (emitter.nextEmissionTick <= ticks) ready++;
        return ready;
    }

    public static void tick(ClientLevel level, Player player) {
        if (previousLevel != level) {
            reset();
            previousLevel = level;
        }
        if (!ClientWindConfig.LEAF_PARTICLES_ENABLED.get()) {
            currentRate = 0;
            spawnCredit = 0;
            return;
        }

        RandomSource random = level.getRandom();
        int radius = ClientWindConfig.LEAF_PARTICLE_RADIUS.get();
        BlockPos center = player.blockPosition();
        // Random probing alone can discover one flower long before the rest of its meadow.
        // Spend a small one-time additional sampling budget as the player moves into new areas.
        boolean enteredNewArea = lastSurveyCenter == null
                || Math.abs(center.getX() - lastSurveyCenter.getX()) >= 4
                || Math.abs(center.getZ() - lastSurveyCenter.getZ()) >= 4
                || Math.abs(center.getY() - lastSurveyCenter.getY()) >= 4;
        if (enteredNewArea) lastSurveyCenter = center.immutable();
        neighborhoodSurveys = 0;
        int samples = enteredNewArea ? 17 : 7;
        for (int i = 0; i < samples; i++) {
            // Most probes favor close-by flowers; other probes cover the full area and canopies.
            discover(level, center, i % 3 == 0 ? radius : Math.min(10, radius), random);
        }
        ++ticks;
        if (ticks % 60 == 0) {
            prune(level, center, radius, CANOPY, Kind.CANOPY, random);
            prune(level, center, radius, PLANTS, Kind.PLANT, random);
            prune(level, center, radius, GRASS, Kind.GRASS, random);
        }

        float wind = ClientWindState.altitudeStrength();
        float strength = Mth.clamp((wind - 0.10f) / 1.15f, 0, 1);
        float density = ClientWindConfig.LEAF_PARTICLE_DENSITY.get().floatValue();
        // Keep the same *maximum* total budget as v0.2.6, not an emission quota per block.
        currentRate = wind < 0.10f ? 0f : (0.09f + 1.65f * (float)Math.pow(strength, 1.55)) * density;
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        if (status == ParticleStatus.DECREASED) currentRate *= 0.55f;
        else if (status == ParticleStatus.MINIMAL) currentRate *= 0.22f;
        if (CANOPY.isEmpty() && PLANTS.isEmpty() && GRASS.isEmpty()) {
            spawnCredit = 0;
            return;
        }

        spawnCredit = Math.min(4.0f, spawnCredit + currentRate);
        int attempts = 0;
        while (spawnCredit >= 1.0f && attempts++ < 4) {
            spawnCredit -= 1.0f;
            // If every nearby source is on cooldown, *discard* that unit of budget.
            // Retrying it later would create a delayed burst from the first ready flower.
            spawnOne(level, center, radius, random);
        }
    }

    private static Kind classify(ClientLevel level, BlockPos pos, BlockState state) {
        if (state.is(WindBlockTags.LEAF_EMITTERS) || state.is(BlockTags.LEAVES))
            return ParticleWindFilter.shouldSpawnNativeLeaves() ? Kind.CANOPY : Kind.NONE;
        if (state.is(WindBlockTags.GRASS_EMITTERS)) return Kind.GRASS;
        if (state.is(BlockTags.FLOWERS) || state.getMapColor(level, pos) == MapColor.PLANT) return Kind.PLANT;
        return Kind.NONE;
    }

    private static ArrayList<Emitter> sources(Kind kind) {
        return switch (kind) {
            case CANOPY -> CANOPY;
            case PLANT -> PLANTS;
            case GRASS -> GRASS;
            default -> CANOPY;
        };
    }

    private static int sourceCap(Kind kind) {
        return switch (kind) {
            case CANOPY -> CANOPY_CAP;
            case PLANT -> PLANT_CAP;
            case GRASS -> GRASS_CAP;
            default -> 0;
        };
    }

    /** Treat stacked cactus / sugar cane blocks as one emitter instead of several. */
    private static BlockPos plantBase(ClientLevel level, BlockPos pos, BlockState state, Kind kind) {
        if (kind != Kind.PLANT) return pos;
        for (int n = 0; n < 8; n++) {
            BlockPos below = pos.below();
            if (level.isOutsideBuildHeight(below) || !level.hasChunkAt(below)
                    || level.getBlockState(below).getBlock() != state.getBlock()) break;
            pos = below;
        }
        return pos;
    }

    private static double distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return (double) dx * dx + (double) dy * dy + (double) dz * dz;
    }

    private static boolean addSource(ClientLevel level, BlockPos center, BlockPos pos,
                                     Kind kind, RandomSource random) {
        ArrayList<Emitter> emitters = sources(kind);
        for (Emitter existing : emitters) if (existing.pos.equals(pos)) return false;
        if (exposedFace(level, pos, random) == null) return false;
        if (emitters.size() >= sourceCap(kind)) {
            // Random eviction allowed a cached flower to be evicted and re-discovered
            // immediately, resetting its cooldown. Retain nearby emitters instead;
            // only replace a demonstrably more distant cached source.
            int farthestIndex = -1;
            double farthestDistance = -1;
            for (int i = 0; i < emitters.size(); i++) {
                BlockPos old = emitters.get(i).pos;
                double dist = distanceSquared(old, center);
                if (dist > farthestDistance) {
                    farthestDistance = dist;
                    farthestIndex = i;
                }
            }
            if (farthestIndex < 0 || distanceSquared(pos, center) + 25 >= farthestDistance) return false;
            emitters.remove(farthestIndex);
        }
        Emitter emitter = new Emitter(pos);
        // Stagger first emission so the first flower discovered is not instantly active.
        emitter.nextEmissionTick = ticks + switch (kind) {
            case CANOPY -> random.nextInt(10);
            case PLANT -> 12 + random.nextInt(35);
            case GRASS -> random.nextInt(24);
            default -> 0;
        };
        emitters.add(emitter);
        return true;
    }

    private static void discover(ClientLevel level, BlockPos center, int radius, RandomSource random) {
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
        if (!level.hasChunkAt(new BlockPos(x, center.getY(), z))) return;
        int y;
        float roll = random.nextFloat();
        if (roll < 0.60f) {
            y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        } else if (roll < 0.86f) {
            y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1 - random.nextInt(6);
        } else {
            y = center.getY() + random.nextInt(25) - 8;
        }
        if (Math.abs(y - center.getY()) > 24 || level.isOutsideBuildHeight(new BlockPos(x, y, z))) return;
        BlockPos pos = new BlockPos(x, y, z);
        for (int j = 0; j < 3; j++) {
            BlockPos test = j == 0 ? pos : pos.offset(random.nextInt(5) - 2,
                    random.nextInt(5) - 2, random.nextInt(5) - 2);
            if (level.isOutsideBuildHeight(test) || !level.hasChunkAt(test)) continue;
            BlockState state = level.getBlockState(test);
            Kind kind = classify(level, test, state);
            if (kind == Kind.NONE) continue;
            test = plantBase(level, test, state, kind);
            boolean newlyAdded = addSource(level, center, test, kind, random);
            // A discovered flower patch should populate several emitters, not just the first
            // lucky flower hit by a sparse random terrain probe. Only run on a NEW plant.
            if (newlyAdded && kind == Kind.PLANT && neighborhoodSurveys++ < 2)
                discoverPlantNeighbors(level, center, radius, test, random);
            break;
        }
    }

    private static void discoverPlantNeighbors(ClientLevel level, BlockPos center, int radius,
                                               BlockPos origin, RandomSource random) {
        // A 5x5 horizontal neighborhood picks up scattered flower beds (not just adjacent
        // blocks). At most two such surveys are run per tick to cap worst-case work.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos test = origin.offset(dx, dy, dz);
                    if (Math.abs(test.getY() - center.getY()) > 24
                            || Math.hypot(test.getX() - center.getX(), test.getZ() - center.getZ()) > radius + 2
                            || level.isOutsideBuildHeight(test) || !level.hasChunkAt(test)) continue;
                    BlockState state = level.getBlockState(test);
                    if (classify(level, test, state) != Kind.PLANT) continue;
                    addSource(level, center, plantBase(level, test, state, Kind.PLANT), Kind.PLANT, random);
                    break;
                }
            }
        }
    }

    private static void prune(ClientLevel level, BlockPos center, int radius, ArrayList<Emitter> found,
                              Kind expected, RandomSource random) {
        found.removeIf(emitter -> {
            BlockPos pos = emitter.pos;
            return Math.hypot(pos.getX() - center.getX(), pos.getZ() - center.getZ()) > radius + 2
                || Math.abs(pos.getY() - center.getY()) > 26
                || !level.hasChunkAt(pos)
                || classify(level, pos, level.getBlockState(pos)) != expected
                || exposedFace(level, pos, random) == null;
        });
    }

    private static Direction exposedFace(ClientLevel level, BlockPos pos, RandomSource random) {
        int start = random.nextInt(FACES.length);
        for (int i = 0; i < FACES.length; i++) {
            Direction side = FACES[(start + i) % FACES.length];
            BlockPos outside = pos.relative(side);
            if (!level.hasChunkAt(outside)) continue;
            BlockState state = level.getBlockState(outside);
            if (state.isAir() || state.getCollisionShape(level, outside).isEmpty()) return side;
        }
        return null;
    }

    private static float availableWeight(Kind kind) {
        // A category with only one cached flower should not consume the full meadow budget.
        // Full weights are restored when the player is around a healthy source population.
        return switch (kind) {
            case CANOPY -> ParticleWindFilter.shouldSpawnNativeLeaves()
                ? 0.52f * Math.min(1.0f, CANOPY.size() / 18.0f) : 0.0f;
            case PLANT -> 0.25f * Math.min(1.0f, PLANTS.size() / 18.0f);
            case GRASS -> 0.23f * Math.min(1.0f, GRASS.size() / 32.0f);
            default -> 0;
        };
    }

    private static void spawnOne(ClientLevel level, BlockPos center, int radius, RandomSource random) {
        float wc = availableWeight(Kind.CANOPY);
        float wp = availableWeight(Kind.PLANT);
        float wg = availableWeight(Kind.GRASS);
        float total = wc + wp + wg;
        if (total <= 0) return;
        float roll = random.nextFloat() * total;
        Kind first = roll < wc ? Kind.CANOPY : roll < wc + wp ? Kind.PLANT : Kind.GRASS;
        // Fallback to other categories when the selected category has no ready source.
        // Never fall back to the same source a second time during this budget attempt.
        if (trySpawn(level, center, radius, first, random)) return;
        Kind second = first == Kind.CANOPY
                ? (random.nextFloat() * (wp + wg) < wp ? Kind.PLANT : Kind.GRASS)
                : first == Kind.PLANT
                ? (random.nextFloat() * (wc + wg) < wc ? Kind.CANOPY : Kind.GRASS)
                : (random.nextFloat() * (wc + wp) < wc ? Kind.CANOPY : Kind.PLANT);
        if (trySpawn(level, center, radius, second, random)) return;
        Kind third = first != Kind.CANOPY && second != Kind.CANOPY ? Kind.CANOPY
                : first != Kind.PLANT && second != Kind.PLANT ? Kind.PLANT : Kind.GRASS;
        trySpawn(level, center, radius, third, random);
    }

    private static int getCursor(Kind kind) {
        return switch (kind) {
            case CANOPY -> canopyCursor;
            case PLANT -> plantCursor;
            case GRASS -> grassCursor;
            default -> 0;
        };
    }

    private static void setCursor(Kind kind, int cursor) {
        switch (kind) {
            case CANOPY -> canopyCursor = cursor;
            case PLANT -> plantCursor = cursor;
            case GRASS -> grassCursor = cursor;
            default -> { }
        }
    }

    private static boolean trySpawn(ClientLevel level, BlockPos center, int radius,
                                    Kind kind, RandomSource random) {
        if (kind == Kind.CANOPY && !ParticleWindFilter.shouldSpawnNativeLeaves()) return false;
        ArrayList<Emitter> found = sources(kind);
        int count = found.size();
        if (count == 0) return false;
        int cursor = Math.floorMod(getCursor(kind), count);
        int scan = Math.min(SEARCH_LIMIT, count);
        for (int n = 0; n < scan; n++) {
            int index = (cursor + n) % count;
            Emitter emitter = found.get(index);
            if (emitter.nextEmissionTick > ticks) continue;
            BlockPos source = emitter.pos;
            if (Math.hypot(source.getX() - center.getX(), source.getZ() - center.getZ()) > radius + 2
                    || !level.hasChunkAt(source)) continue;
            BlockState foliage = level.getBlockState(source);
            if (classify(level, source, foliage) != kind) continue;
            BlockPos emissionPos = source;
            if (kind == Kind.PLANT) {
                // Emit from different segments of a tall plant, while counting the entire
                // cactus/sugar cane column as one physical source with a single cooldown.
                int rise = random.nextInt(4);
                for (int step = 0; step < rise; step++) {
                    BlockPos above = emissionPos.above();
                    if (level.isOutsideBuildHeight(above) || !level.hasChunkAt(above)
                            || level.getBlockState(above).getBlock() != foliage.getBlock()) break;
                    emissionPos = above;
                }
            }
            Direction face = exposedFace(level, emissionPos, random);
            if (face == null && !emissionPos.equals(source)) {
                emissionPos = source;
                face = exposedFace(level, source, random);
            }
            if (face == null) continue;
            if (WindShelter.exposureAt(emissionPos.relative(face)) < 0.20f) continue;

            // Advance fairness cursor; no source can monopolize the next emission.
            setCursor(kind, (index + 1) % count);
            emitter.nextEmissionTick = ticks + switch (kind) {
                case CANOPY -> 12 + random.nextInt(19);  // 0.7–1.7/s per leaf block
                case PLANT -> 45 + random.nextInt(46);   // 0.2–0.45/s per flower or tall plant
                case GRASS -> 20 + random.nextInt(25);   // 0.45–1/s per groundcover block
                default -> 50;
            };
            int color = emitterColor(level, emissionPos, level.getBlockState(emissionPos), kind, random);
            float shade = 0.84f + random.nextFloat() * 0.22f;
            double red = Mth.clamp(((color >> 16) & 255) / 255f * shade, 0.06f, 1f);
            double green = Mth.clamp(((color >> 8) & 255) / 255f * shade, 0.06f, 1f);
            double blue = Mth.clamp((color & 255) / 255f * shade, 0.06f, 1f);
            double x = emissionPos.getX() + 0.5 + face.getStepX() * 0.58;
            double y = emissionPos.getY() + 0.5 + face.getStepY() * 0.58;
            double z = emissionPos.getZ() + 0.5 + face.getStepZ() * 0.58;
            double a = (random.nextDouble() - 0.5) * 0.78;
            double b = (random.nextDouble() - 0.5) * 0.78;
            if (face.getStepX() != 0) { y += a; z += b; }
            else if (face.getStepY() != 0) { x += a; z += b; }
            else { x += a; y += b; }
            level.addParticle(kind == Kind.GRASS ? ModParticles.WIND_GRASS.get() : ModParticles.WIND_LEAF.get(),
                    x, y, z, red, green, blue);
            return true;
        }
        // A saturated or unreachable group is not retried from the same first few entries.
        setCursor(kind, (cursor + scan) % count);
        return false;
    }

    /** Block tint stays authoritative; untinted foliage uses the cached texture palette. */
    private static int emitterColor(ClientLevel level, BlockPos pos, BlockState state, Kind kind, RandomSource random) {
        if (kind == Kind.CANOPY) {
            return WindFoliageColorCache.leafColor(state, level, pos, random,
                ClientWindConfig.LEAF_TEXTURE_COLORS.get());
        }
        int color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
        if (color != -1) return color & 0xFFFFFF;
        if (ClientWindConfig.LEAF_TEXTURE_COLORS.get()) {
            int textureColor = WindFoliageColorCache.colorFor(state, level, pos, random, kind == Kind.PLANT);
            if (textureColor != -1) return textureColor;
        }
        if (kind == Kind.PLANT) {
            String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (name.contains("poppy") || name.contains("rose") || name.contains("red_") || name.contains("crimson")) return 0xDF414A;
            if (name.contains("dandelion") || name.contains("sunflower") || name.contains("yellow")) return 0xF0C44D;
            if (name.contains("orange") || name.contains("torchflower")) return 0xEC914D;
            if (name.contains("cornflower") || name.contains("blue_orchid") || name.contains("blue_")) return 0x548DDB;
            if (name.contains("allium") || name.contains("lilac") || name.contains("purple") || name.contains("lavender")) return 0xB17AD0;
            if (name.contains("pink") || name.contains("peony") || name.contains("cherry")) return 0xECA0BB;
            if (name.contains("white") || name.contains("oxeye") || name.contains("lily_of_the_valley")) return 0xEAEDE0;
        }
        return state.getMapColor(level, pos).col & 0xFFFFFF;
    }
}
