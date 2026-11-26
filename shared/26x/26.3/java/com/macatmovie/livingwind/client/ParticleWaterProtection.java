package com.macatmovie.livingwind.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;

/**
 * Prevents atmospheric wind from displacing submerged particles, without requiring
 * a separate blacklist entry for every underwater particle type.
 *
 * Particle.move() may run on third-party async particle workers. Those workers only
 * access the concurrent cache and enqueue a block coordinate. All fluid and chunk
 * queries happen on the Minecraft client thread; even cache misses never touch the
 * world from worker threads. Positions are exact blocks (not 2x2x2 shelter cells),
 * and the cached water surface height distinguishes submerged particles from
 * splashes floating immediately above a flowing-water surface.
 */
public final class ParticleWaterProtection {
    private static final int MAX_ENTRIES = 2048;
    private static final int MAX_PENDING = 512;
    private static final int MAX_QUEUED_QUERIES_PER_TICK = 96;
    private static final int MAX_DIRECT_QUERIES_PER_TICK = 48;
    private static final int WET_CACHE_TICKS = 12;
    private static final int DRY_CACHE_TICKS = 40;
    // Incrementally clean only larger caches. Smaller caches are bounded already,
    // and isFresh() prevents stale readings from being used regardless of cleanup.
    private static final int CLEANUP_START_SIZE = 512;
    private static final int CLEANUP_SCAN_PER_PASS = 64;
    private static final int CLEANUP_INTERVAL_TICKS = 2;
    private static final int CLEANUP_EXPIRE_TICKS = DRY_CACHE_TICKS * 2;
    private static final ConcurrentHashMap<Long, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Set<Long> PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> REQUESTS = new ConcurrentLinkedQueue<>();
    private static volatile ClientLevel currentLevel;
    private static volatile int tick;
    private static int directQueries;
    // Used exclusively on the client tick thread; worker threads only use CACHE,
    // PENDING and REQUESTS. The weakly consistent iterator tolerates cache puts.
    private static Iterator<Map.Entry<Long, Entry>> cleanupCursor;

    /** Height within a block (0 means no water); updates are published atomically. */
    private record Entry(float waterHeight, int measuredAt) {}

    private ParticleWaterProtection() {}

    /** Called only from the normal client tick thread, even with Async Particles installed. */
    public static void tick(ClientLevel level, int gameTick) {
        if (currentLevel != level) {
            clear();
            currentLevel = level;
        }
        tick = gameTick;
        directQueries = 0;

        for (int i = 0; i < MAX_QUEUED_QUERIES_PER_TICK; i++) {
            Long key = REQUESTS.poll();
            if (key == null) break;
            PENDING.remove(key);
            Entry existing = CACHE.get(key);
            if (existing != null && isFresh(existing, gameTick)) continue;
            CACHE.put(key, measure(level, key, gameTick));
        }

        cleanupCache(gameTick);
    }

    /**
     * Avoid a full ConcurrentHashMap.removeIf() sweep on the client thread.
     * Scan at most a small number of entries every other tick, and do not scan
     * low-population caches at all. Expiration checks in isSubmerged() still
     * control correctness; this only reclaims memory.
     */
    private static void cleanupCache(int gameTick) {
        int size = CACHE.size();
        if (size < CLEANUP_START_SIZE) {
            cleanupCursor = null;
        } else if (gameTick % CLEANUP_INTERVAL_TICKS == 0) {
            int examined = 0;
            while (examined < CLEANUP_SCAN_PER_PASS) {
                if (cleanupCursor == null) cleanupCursor = CACHE.entrySet().iterator();
                if (!cleanupCursor.hasNext()) {
                    cleanupCursor = null;
                    break;
                }
                Map.Entry<Long, Entry> candidate = cleanupCursor.next();
                // Conditional remove avoids deleting a freshly remeasured reading.
                if (gameTick - candidate.getValue().measuredAt() > CLEANUP_EXPIRE_TICKS) {
                    CACHE.remove(candidate.getKey(), candidate.getValue());
                }
                examined++;
            }
        }

        // Preserve the original hard cap, even if a sudden wave of new particles
        // fills the cache faster than the incremental expiration scan can drain it.
        int overflow = CACHE.size() - MAX_ENTRIES;
        if (overflow > 0) {
            Iterator<Map.Entry<Long, Entry>> evictions = CACHE.entrySet().iterator();
            while (overflow > 0 && evictions.hasNext()) {
                Map.Entry<Long, Entry> candidate = evictions.next();
                if (CACHE.remove(candidate.getKey(), candidate.getValue())) overflow--;
            }
            cleanupCursor = null;
        }
    }

    /**
     * Precise client-tick-only check for the few actual native emission candidates.
     * Unlike the asynchronous particle lookup, a newly waterlogged block must not
     * fail open while waiting in the water-cache queue. Also publishes the reading
     * so the newly created particle inherits correct water protection immediately.
     * Do not call this from particle tick/render or Async Particles workers.
     */
    public static boolean isSubmergedAtEmitter(ClientLevel level, double x, double y, double z) {
        int blockY = Mth.floor(y);
        long key = BlockPos.asLong(Mth.floor(x), blockY, Mth.floor(z));
        BlockPos pos = BlockPos.of(key);
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return true;
        Entry measured = measure(level, key, tick);
        CACHE.put(key, measured);
        return belowSurface(measured, blockY, y);
    }

    /** Safe from either the client thread or async particle workers. */
    public static boolean isSubmerged(double x, double y, double z) {
        ClientLevel level = currentLevel;
        if (level == null) return false;
        int blockY = Mth.floor(y);
        long key = BlockPos.asLong(Mth.floor(x), blockY, Mth.floor(z));
        int now = tick;
        Entry entry = CACHE.get(key);
        if (entry != null && isFresh(entry, now)) return belowSurface(entry, blockY, y);

        // On the client thread, measure a limited number of misses immediately, so
        // newly-created underwater particles are protected from their very first tick.
        // Workers *never* read fluid state, even when their cache entry is missing.
        if (Minecraft.getInstance().isSameThread() && directQueries < MAX_DIRECT_QUERIES_PER_TICK) {
            directQueries++;
            Entry measured = measure(level, key, now);
            CACHE.put(key, measured);
            return belowSurface(measured, blockY, y);
        }

        // Bounded, deduplicated work for the client thread. An existing (slightly
        // stale) underwater reading is preferable to a sudden one-frame wind kick.
        if (PENDING.size() < MAX_PENDING && PENDING.add(key)) REQUESTS.offer(key);
        return entry != null && belowSurface(entry, blockY, y);
    }

    private static boolean isFresh(Entry entry, int now) {
        int maxAge = entry.waterHeight() > 0.0f ? WET_CACHE_TICKS : DRY_CACHE_TICKS;
        return now - entry.measuredAt() <= maxAge && now >= entry.measuredAt();
    }

    private static boolean belowSurface(Entry entry, int blockY, double y) {
        return entry.waterHeight() > 0.0f && y < blockY + entry.waterHeight() - 0.015;
    }

    private static Entry measure(ClientLevel level, long key, int now) {
        BlockPos pos = BlockPos.of(key);
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return new Entry(0.0f, now);
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)) return new Entry(0.0f, now);
        return new Entry(Mth.clamp(fluid.getHeight(level, pos), 0.0f, 1.0f), now);
    }

    /** Clears cached positions and queued work on disconnect/dimension changes. */
    public static void clear() {
        currentLevel = null;
        CACHE.clear();
        PENDING.clear();
        REQUESTS.clear();
        cleanupCursor = null;
        directQueries = 0;
        tick = 0;
    }
}
