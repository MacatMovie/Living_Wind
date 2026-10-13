package com.macatmovie.livingwind.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bounded client-side palette cache for leaves and flowers, including modded baked models.
 *
 * Minecraft foliage frequently uses GRAYSCALE textures whose actual color comes from
 * a tint index on the baked quad. Caching only RGB texture samples loses that index;
 * the resulting particles can turn white/gray (or use the wrong biome color).
 *
 * Store texture RGB AND its quad tint index. Expensive model/atlas sampling happens
 * once per state, while the inexpensive BlockColors lookup happens only when a particle
 * is emitted so biome-dependent colors stay accurate. No world data or colors are
 * cached across positions. Clear on resource-pack reload.
 */
public final class WindFoliageColorCache {
    private static final int MAX_STATES = 512;
    private static final int MAX_QUADS = 36;
    private static final int MAX_SPRITES = 7;
    private static final int MAX_SAMPLES_AXIS = 12;
    private static final Sample[] EMPTY = new Sample[0];
    private static final int WEIGHTED_SAMPLE_SLOTS = 8;

    private record StateKey(BlockState state, boolean preferPetals) {}
    private record SpriteKey(String name, int tintIndex) {}
    private record Sample(int rgb, int tintIndex) {}

    private static final LinkedHashMap<StateKey, Sample[]> PALETTES = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<StateKey, Sample[]> eldest) {
            return size() > MAX_STATES;
        }
    };

    private WindFoliageColorCache() {}

    public static synchronized void clear() {
        PALETTES.clear();
    }

    /**
     * Sample a model texture. Apply BlockColors ONLY where that model's quads request
     * a tint index; untinted textures (e.g. cherry petals) remain unmodified.
     * Returns -1 if the model has no usable palette.
     */
    public static int colorFor(BlockState state, ClientLevel level, BlockPos pos,
                                            RandomSource random, boolean preferPetals) {
        Sample[] samples = palette(state, preferPetals);
        if (samples.length == 0) return -1;
        Sample sample = samples[random.nextInt(samples.length)];
        if (sample.tintIndex() < 0) return sample.rgb();

        int tint = Minecraft.getInstance().getBlockColors()
            .getColor(state, level, pos, sample.tintIndex());
        if (tint == -1) {
            // Some custom model/tint combinations have no registered provider.
            // Do not let a marked-tint grayscale quad produce white leaves.
            tint = state.is(Blocks.BIRCH_LEAVES)
                ? FoliageColor.getBirchColor()
                : state.getMapColor(level, pos).col;
            // Modded tinted leaves may omit their color provider. When the map
            // color is also grayscale, prefer the vanilla default foliage green
            // rather than emitting white fragments from an uncolored mask.
            if (tint == -1 || (state.is(WindBlockTags.LEAF_EMITTERS) && isNearlyGray(tint)))
                tint = FoliageColor.getDefaultColor();
        }
        if (!preferPetals && shouldPreserveRawLeafColor(sample.rgb(), tint)) return sample.rgb();
        return multiply(sample.rgb(), tint);
    }

    /** Leaves-only entry point: prefers baked-model color but preserves the old fallback. */
    public static int leafColor(BlockState state, ClientLevel level, BlockPos pos,
                                RandomSource random, boolean textureColors) {
        if (textureColors) {
            int color = colorFor(state, level, pos, random, false);
            if (color != -1) {
                // Resource packs may replace vanilla birch with an un-tinted grayscale model.
                if (state.is(Blocks.BIRCH_LEAVES) && isNearlyGray(color))
                    return FoliageColor.getBirchColor() & 0xFFFFFF;
                return color;
            }
        }
        // Default BlockColors tint index, useful when texture sampling is disabled.
        int tint = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
        if (tint != -1) return tint & 0xFFFFFF;
        if (state.is(Blocks.BIRCH_LEAVES)) return FoliageColor.getBirchColor() & 0xFFFFFF;
        return state.getMapColor(level, pos).col & 0xFFFFFF;
    }


    private static boolean shouldPreserveRawLeafColor(int raw, int tint) {
        if (isNearlyGray(raw)) return false;

        float[] rawHsv = hsv(raw);
        float[] tintHsv = hsv(tint);
        boolean rawSaturated = rawHsv[1] >= 0.18f && rawHsv[2] >= 0.22f;
        if (!rawSaturated) return false;

        boolean rawIsGreenish = isGreenHue(rawHsv[0]);
        boolean tintIsGreenish = isGreenHue(tintHsv[0]) && tintHsv[1] >= 0.14f;
        if (!rawIsGreenish && tintIsGreenish) return true;

        float hueDiff = Math.abs(rawHsv[0] - tintHsv[0]);
        hueDiff = Math.min(hueDiff, 360.0f - hueDiff);
        return !rawIsGreenish && tintHsv[1] >= 0.18f && hueDiff >= 40.0f;
    }

    private static boolean isGreenHue(float hue) {
        return hue >= 55.0f && hue <= 170.0f;
    }

    private static float[] hsv(int color) {
        float r = ((color >>> 16) & 255) / 255.0f;
        float g = ((color >>> 8) & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta < 1.0e-5f) h = 0.0f;
        else if (max == r) h = 60.0f * (((g - b) / delta) % 6.0f);
        else if (max == g) h = 60.0f * (((b - r) / delta) + 2.0f);
        else h = 60.0f * (((r - g) / delta) + 4.0f);
        if (h < 0.0f) h += 360.0f;
        float s = max <= 0.0f ? 0.0f : delta / max;
        return new float[] { h, s, max };
    }

    private static boolean isNearlyGray(int color) {
        int r = (color >>> 16) & 255;
        int g = (color >>> 8) & 255;
        int b = color & 255;
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 12;
    }

    /** Per-channel multiply matches vanilla vertex tinting; don't replace texture detail. */
    private static int multiply(int rgb, int tint) {
        int r = (((rgb >>> 16) & 255) * ((tint >>> 16) & 255) + 127) / 255;
        int g = (((rgb >>> 8) & 255) * ((tint >>> 8) & 255) + 127) / 255;
        int b = ((rgb & 255) * (tint & 255) + 127) / 255;
        return (r << 16) | (g << 8) | b;
    }

    private static synchronized Sample[] palette(BlockState state, boolean preferPetals) {
        StateKey key = new StateKey(state, preferPetals);
        Sample[] samples = PALETTES.get(key);
        if (samples == null) {
            samples = extract(state, preferPetals);
            PALETTES.put(key, samples);
        }
        return samples;
    }

    private static Sample[] extract(BlockState state, boolean preferPetals) {
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            if (model == null) return EMPTY;
            Set<SpriteKey> seen = new HashSet<>();
            Set<String> seenTextureNames = new HashSet<>();
            Map<Long, Bucket> buckets = new HashMap<>();
            int[] visited = {0};

            sampleQuads(model.getQuads(state, null, RandomSource.create(0)), seen, seenTextureNames,
                buckets, preferPetals, visited);
            for (Direction direction : Direction.values()) {
                if (seen.size() >= MAX_SPRITES || visited[0] >= MAX_QUADS) break;
                sampleQuads(model.getQuads(state, direction, RandomSource.create(0)), seen,
                    seenTextureNames, buckets, preferPetals, visited);
            }
            // The particle icon is a fallback for unusual models/flowers. Never sample it a
            // second time as *untinted* if a tinted model quad already used that texture.
            TextureAtlasSprite icon = model.getParticleIcon();
            if (seen.size() < MAX_SPRITES && icon != null && icon.contents() != null
                    && !seenTextureNames.contains(icon.contents().name().toString())) {
                sample(icon, -1, seen, seenTextureNames, buckets, preferPetals);
            }
            if (buckets.isEmpty()) return EMPTY;

            List<Bucket> sorted = new ArrayList<>(buckets.values());
            sorted.sort(Comparator.comparingInt((Bucket b) -> b.count).reversed());
            // Foliage models can have both green-tinted leaf planes and untinted
            // wood/overlay geometry. Prefer the actual tinted leaf material whenever
            // possible; for flowers, retain the existing petal preference instead.
            boolean hasTinted = !preferPetals && sorted.stream().anyMatch(b -> b.tintIndex >= 0);
            List<Bucket> chosen = new ArrayList<>(Math.min(4, sorted.size()));
            for (Bucket bucket : sorted) {
                if (hasTinted && bucket.tintIndex < 0) continue;
                if (bucket.count < 2 && !chosen.isEmpty()) continue;
                chosen.add(bucket);
                if (chosen.size() >= 4) break;
            }
            if (chosen.isEmpty()) return EMPTY;

            int total = 0;
            for (Bucket bucket : chosen) total += Math.max(1, bucket.count);
            ArrayList<Sample> weighted = new ArrayList<>(WEIGHTED_SAMPLE_SLOTS);
            for (Bucket bucket : chosen) {
                int copies = Math.max(1,
                    Math.round((Math.max(1, bucket.count) / (float) total) * WEIGHTED_SAMPLE_SLOTS));
                for (int i = 0; i < copies && weighted.size() < WEIGHTED_SAMPLE_SLOTS; i++) {
                    weighted.add(new Sample(bucket.rgb(), bucket.tintIndex));
                }
            }
            while (weighted.size() < Math.min(WEIGHTED_SAMPLE_SLOTS, chosen.size() * 2)) {
                Bucket bucket = chosen.get(weighted.size() % chosen.size());
                weighted.add(new Sample(bucket.rgb(), bucket.tintIndex));
            }
            return weighted.toArray(Sample[]::new);
        } catch (RuntimeException ex) {
            // Some dynamic/custom modded models cannot expose standard baked quads.
            // Their particles still use the registered block-color or map-color fallback.
            return EMPTY;
        }
    }

    private static void sampleQuads(Iterable<BakedQuad> quads, Set<SpriteKey> seen,
                                    Set<String> seenTextureNames, Map<Long, Bucket> buckets,
                                    boolean preferPetals, int[] visited) {
        for (BakedQuad quad : quads) {
            if (++visited[0] > MAX_QUADS || seen.size() >= MAX_SPRITES) break;
            sample(quad.getSprite(), quad.isTinted() ? quad.getTintIndex() : -1,
                seen, seenTextureNames, buckets, preferPetals);
        }
    }

    private static void sample(TextureAtlasSprite sprite, int tintIndex, Set<SpriteKey> seen,
                               Set<String> seenTextureNames, Map<Long, Bucket> buckets,
                               boolean preferPetals) {
        if (sprite == null || sprite.contents() == null) return;
        String id = sprite.contents().name().toString();
        if (id.equals("minecraft:missingno") || !seen.add(new SpriteKey(id, tintIndex))) return;
        seenTextureNames.add(id);
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        if (width <= 0 || height <= 0) return;
        int sx = Math.min(MAX_SAMPLES_AXIS, width);
        int sy = Math.min(MAX_SAMPLES_AXIS, height);
        for (int iy = 0; iy < sy; iy++) {
            int y = (iy * height + height / 2) / sy;
            for (int ix = 0; ix < sx; ix++) {
                int x = (ix * width + width / 2) / sx;
                // Vanilla 1.20.1 / 1.21.1 getPixelRGBA() returns ABGR.
                int rgba = sprite.getPixelRGBA(0, x, y);
                if ((rgba >>> 24) < 120) continue;
                int r = rgba & 255, g = (rgba >>> 8) & 255, b = (rgba >>> 16) & 255;
                if (Math.max(r, Math.max(g, b)) < 72) continue;
                if (preferPetals && g > r * 1.15 && g > b * 1.10 && g - r > 15) continue;
                int rgbGroup = ((r >>> 5) << 6) | ((g >>> 5) << 3) | (b >>> 5);
                long key = ((long)(tintIndex + 1) << 9) | rgbGroup;
                Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(tintIndex));
                bucket.add(r, g, b);
            }
        }
    }

    private static final class Bucket {
        final int tintIndex;
        int count, red, green, blue;
        Bucket(int tintIndex) { this.tintIndex = tintIndex; }
        void add(int r, int g, int b) {
            count++;
            red += r;
            green += g;
            blue += b;
        }
        int rgb() {
            return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
        }
    }
}
