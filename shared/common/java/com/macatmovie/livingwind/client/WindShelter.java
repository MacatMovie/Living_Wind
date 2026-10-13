package com.macatmovie.livingwind.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.tags.BlockTags;

/**
 * Tiny, bounded, demand-driven shelter grid. Particle threads only read published numbers and
 * enqueue missing zones; all level/chunk/collision queries run in ClientWindEvents on the client thread.
 * A 2-block cell avoids sharing one exposure value across most single-block walls and windows.
 */
public final class WindShelter {
    private static final int MAX_ZONES = 768;
    private static final int MAX_REQUESTS = 384;
    private static final int MAX_AGE_TICKS = 55;
    // Stationary block animations must not inherit the particle cache's aggressive
    // expiration and eviction behavior. A wind gust produces many particle requests.
    private static final int MAX_STATIC_ZONES = 256;
    private static final int STATIC_REFRESH_TICKS = 40;
    private static final int STATIC_IDLE_TICKS = 160;
    private static final ConcurrentHashMap<Long, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Set<Long> PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> REQUESTS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<Long, StaticEntry> STATIC_CACHE = new ConcurrentHashMap<>();
    private static final Set<Long> STATIC_PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> STATIC_REQUESTS = new ConcurrentLinkedQueue<>();
    private static volatile boolean enabled = true;
    private static volatile float effectiveness = 1.0f;
    private static volatile boolean lowCost;
    private static volatile int tick;
    private static ClientLevel currentLevel;

    private record Entry(float exposure, int tick) {}

    private static final class StaticEntry {
        volatile float exposure = 1.0f;
        volatile int measuredTick = Integer.MIN_VALUE;
        volatile int lastUsedTick;
    }
    private WindShelter() {}

    public static void configure(boolean useShelter, float effect, boolean lowQuality) {
        enabled = useShelter;
        effectiveness = Mth.clamp(effect, 0.0f, 1.0f);
        lowCost = lowQuality;
    }

    /** Safe from the render thread and from async particle workers; never reads the world. */
    public static float exposureAt(double x, double y, double z) {
        if (!enabled || effectiveness <= 0.0f) return 1.0f;
        long key = key(x, y, z);
        Entry entry = CACHE.get(key);
        int now = tick;
        if (entry != null && now - entry.tick <= MAX_AGE_TICKS) return adjust(entry.exposure);
        // Avoid issuing unbounded requests if a mod spawns massive numbers of particles.
        if (PENDING.size() < MAX_REQUESTS && PENDING.add(key)) REQUESTS.offer(key);
        // Never accidentally suppress particles outside a house just because the camera is inside.
        return entry != null ? adjust(entry.exposure) : 1.0f;
    }

    public static float exposureAt(BlockPos pos) {
        return exposureAt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
    }

    /**
     * Stable, exact-block shelter exposure for FWA chains, lanterns and bells.
     * Only reads published values during rendering. Surveys run on the client tick;
     * the last measured exposure survives periods of heavy particle-cache turnover.
     * Requests are throttled so repeated shader passes do not trigger extra work.
     */
    public static float exposureForStaticAnimation(BlockPos pos) {
        if (!enabled || effectiveness <= 0.0f) return 1.0f;
        long key = pos.asLong();
        StaticEntry entry = STATIC_CACHE.get(key);
        if (entry == null) {
            if (STATIC_CACHE.size() >= MAX_STATIC_ZONES) {
                // Do not grow indefinitely. Newly seen excess animations use the
                // ordinary particle shelter cache until an old static entry expires.
                return exposureAt(pos);
            }
            StaticEntry created = new StaticEntry();
            StaticEntry previous = STATIC_CACHE.putIfAbsent(key, created);
            entry = previous != null ? previous : created;
        }
        int now = tick;
        entry.lastUsedTick = now;
        if ((entry.measuredTick == Integer.MIN_VALUE || now - entry.measuredTick >= STATIC_REFRESH_TICKS)
            && STATIC_PENDING.add(key)) {
            STATIC_REQUESTS.offer(key);
        }
        return adjust(entry.exposure);
    }

    public static float playerExposure(ClientLevel level, BlockPos pos) {
        if (!enabled || effectiveness <= 0.0f) return 1.0f;
        return adjust(survey(level, pos, lowCost));
    }

    private static float adjust(float value) { return 1.0f - (1.0f - value) * effectiveness; }

    /** Call once per client tick, before particle simulation. */
    public static void tick(ClientLevel level, int gameTick) {
        if (currentLevel != level) {
            clear();
            currentLevel = level;
        }
        tick = gameTick;
        if (!enabled) return;
        // Dedicated tiny budget for exact-block FWA checks. This does not consume
        // the particle shelter budget, and never runs on particle/render workers.
        int staticBudget = lowCost ? 1 : 2;
        for (int i = 0; i < staticBudget; i++) {
            Long key = STATIC_REQUESTS.poll();
            if (key == null) break;
            STATIC_PENDING.remove(key);
            StaticEntry entry = STATIC_CACHE.get(key);
            if (entry == null || gameTick - entry.lastUsedTick > STATIC_IDLE_TICKS) continue;
            BlockPos pos = BlockPos.of(key);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
            entry.exposure = survey(level, pos, lowCost);
            entry.measuredTick = gameTick;
        }
        int budget = lowCost ? 2 : 6;
        for (int i = 0; i < budget; i++) {
            Long key = REQUESTS.poll();
            if (key == null) break;
            PENDING.remove(key);
            BlockPos cell = BlockPos.of(key);
            BlockPos center = new BlockPos(cell.getX() * 2 + 1, cell.getY() * 2 + 1, cell.getZ() * 2 + 1);
            if (level.isOutsideBuildHeight(center) || !level.hasChunkAt(center)) continue;
            CACHE.put(key, new Entry(survey(level, center, lowCost), gameTick));
        }
        // Inactive cells expire. A hard cap also bounds memory even with exotic particle engines.
        if (gameTick % 80 == 0 || CACHE.size() > MAX_ZONES) {
            STATIC_CACHE.entrySet().removeIf(e -> gameTick - e.getValue().lastUsedTick > STATIC_IDLE_TICKS);
            CACHE.entrySet().removeIf(e -> gameTick - e.getValue().tick > MAX_AGE_TICKS * 2);
            if (CACHE.size() > MAX_ZONES) {
                int extra = CACHE.size() - MAX_ZONES;
                for (Long key : CACHE.keySet()) {
                    if (extra-- <= 0) break;
                    CACHE.remove(key);
                }
            }
        }
    }

    public static void clear() {
        CACHE.clear(); PENDING.clear(); REQUESTS.clear();
        STATIC_CACHE.clear(); STATIC_PENDING.clear(); STATIC_REQUESTS.clear();
        currentLevel = null; tick = 0;
    }

    private static long key(double x, double y, double z) {
        return BlockPos.asLong(Mth.floor(x) >> 1, Mth.floor(y) >> 1, Mth.floor(z) >> 1);
    }

    /**
     * Sky plus horizontal openings, so a roof alone does not create indoor stillness.
     * Full collision blocks (including solid glass) stop airflow; no path tracing or chunk loading.
     */
    private static float survey(ClientLevel level, BlockPos at, boolean cheap) {
        if (!level.dimensionType().hasSkyLight()) return 1.0f;
        if (!level.hasChunkAt(at)) return 1.0f;
        if (level.canSeeSky(at)) return 1.0f;
        int distance = cheap ? 4 : 6;
        float open = 0.0f;
        int visible = 0;
        // Four rays at head height. The first solid barrier terminates each ray.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos.MutableBlockPos sample = at.mutable();
            float openness = 0.0f;
            for (int step = 1; step <= distance; step++) {
                sample.move(dir);
                if (!level.hasChunkAt(sample) || level.isOutsideBuildHeight(sample)) break;
                BlockState state = level.getBlockState(sample);
                // Glass panes and closed doors have non-full collision shapes but block airflow.
                // Leaves are porous, and a genuinely open door should pass wind.
                boolean porous = state.is(BlockTags.LEAVES)
                    || (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN));
                if (!porous && !state.getCollisionShape(level, sample).isEmpty()) break;
                if (level.canSeeSky(sample)) {
                    visible++;
                    openness = 1.0f - (step - 1) * 0.055f;
                    break;
                }
                // Reaching the probe limit without visible sky is inconclusive, not evidence
                // of an opening: a large but fully sealed house must remain windless.
            }
            open += openness;
        }
        if (visible == 4) return 0.95f;
        float lateral = open / 4.0f;
        // Fully sealed house ~0, covered porch / cave entrance partial, uncovered sky 1.
        return Mth.clamp(lateral * (0.72f + visible * 0.06f), 0.0f, 0.95f);
    }
}
