package com.macatmovie.livingwind.client;

import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
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
 * Exact block cells avoid sampling outside a one-block-thick wall or across a low roof.
 * Nearby particles have a separate priority queue/cache to prevent distant particle storms
 * from starving the cells a player actually sees. Both caches are bounded and worker-safe.
 */
public final class WindShelter {
    private static final int MAX_ZONES = 768;
    private static final int MAX_REQUESTS = 384;
    private static final int MAX_AGE_TICKS = 55;
    // Newly seen enclosed rooms should not wait behind hundreds of outside particle requests.
    // Synchronous direct work is allowed ONLY on Minecraft's client thread and is tightly capped.
    private static final int MAX_DIRECT_QUERIES_PER_TICK = 4;
    // Local cells are protected from the heavy turnover of distant smoke/rain particles.
    // Priority is based on position, NOT on the player's shelter state; outdoor particles
    // just outside a house still receive their own accurate outdoor survey.
    private static final int LOCAL_RADIUS_XZ = 7;
    private static final int LOCAL_RADIUS_Y = 5;
    private static final int MAX_LOCAL_ZONES = 512;
    private static final int MAX_LOCAL_REQUESTS = 256;
    private static final int LOCAL_TRIM_INTERVAL = 20;
    // A second protected, distance-prioritized ring covers candles/smoke across
    // larger rooms. The old 7x5 local hot-zone retains first priority. Previously
    // EVERYTHING beyond seven blocks fell into the 3-surveys/tick background queue,
    // which was overwhelmed by persistent particle storms in large buildings.
    // This is not a wind-distance cutoff: particles beyond it still get surveys.
    private static final int NEAR_RADIUS_XZ = 40;
    private static final int NEAR_RADIUS_Y = 22;
    private static final int MAX_NEAR_ZONES = 2048;
    private static final int MAX_NEAR_REQUESTS = 512;
    private static final int NEAR_SURVEYS_PER_TICK = 16;
    private static final int NEAR_TRIM_INTERVAL = 20;
    // Stationary block animations must not inherit the particle cache's aggressive
    // expiration and eviction behavior. A wind gust produces many particle requests.
    private static final int MAX_STATIC_ZONES = 256;
    private static final int STATIC_REFRESH_TICKS = 40;
    private static final int STATIC_IDLE_TICKS = 160;
    // Confident, reusable *3D* sheltered regions. Distinguish both deep caves and
    // well enclosed interior rooms without scanning every smoke particle cell.
    // Regions adjacent to sky openings remain ambiguous and get exact-cell surveys.
    // Every world check is bounded and runs on the client thread only.
    private static final int CAVE_SHIFT = 2; // 4 x 4 x 4 blocks per region
    // A full region can straddle a one-block-thick exterior wall. Subdivide only
    // *ambiguous nearby* regions into eight 2x2x2 cells, and publish a bitmask of
    // individually verified interior cells. Workers read the mask, never world data.
    private static final int WALL_CELL_SHIFT = 1;
    private static final float WALL_CELL_SHELTER_THRESHOLD = 0.19f;
    private static final int[] WALL_SAMPLE_OFFSETS = {0,0,0, 1,0,1, 1,1,0, 0,1,1};
    private static final int CAVE_PROBE_RISE = 12; // a high probe distinguishes caves from roofs
    private static final int CAVE_MAX_REGIONS = 2048;
    private static final int CAVE_MAX_PENDING = 256;
    private static final int CAVE_LOCAL_RADIUS_XZ = 40;
    private static final int CAVE_LOCAL_RADIUS_Y = 32;
    // Strongly sheltered regions can retain their values longer than single moving
    // particle cells. Refresh promptly enough for newly opened doors / edited roofs.
    private static final int CAVE_LIFETIME_TICKS = 90;
    private static final int CAVE_REFRESH_GRACE_TICKS = 60;
    // Outer sky probes check for nearby cave mouths within six blocks. The
    // region's 16 internal columns are checked too, so a one-block skylight
    // shaft cannot accidentally become a confirmed windless cave region.
    private static final int[] CAVE_PROBE_OFFSETS = {
        -6, 0, 6, 0, 0, -6, 0, 6,
        -6, -6, -6, 6, 6, -6, 6, 6
    };
    // Suppress expensive shelter work for particles far beyond normal view distance.
    // This matters for persistent/modded effects 200+ blocks from the camera.
    private static final int MAX_WIND_DISTANCE_XZ = 128;
    private static final int MAX_WIND_DISTANCE_Y = 96;
    private static final ConcurrentHashMap<Long, CaveRegion> CAVE_REGIONS = new ConcurrentHashMap<>();
    private static final Set<Long> CAVE_PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<Long> CAVE_LOCAL_PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> CAVE_LOCAL_REQUESTS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> CAVE_REQUESTS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<Long, Entry> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Entry> LOCAL_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Entry> NEAR_CACHE = new ConcurrentHashMap<>();
    private static final Set<Long> NEAR_PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> NEAR_REQUESTS = new ConcurrentLinkedQueue<>();
    private static final Set<Long> LOCAL_PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> LOCAL_REQUESTS = new ConcurrentLinkedQueue<>();
    private static final Set<Long> PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> REQUESTS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<Long, StaticEntry> STATIC_CACHE = new ConcurrentHashMap<>();
    private static final Set<Long> STATIC_PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Long> STATIC_REQUESTS = new ConcurrentLinkedQueue<>();
    private static volatile boolean enabled = true;
    private static volatile float effectiveness = 1.0f;
    private static volatile boolean lowCost;
    private static volatile int tick;
    private static volatile ClientLevel currentLevel;
    private static volatile int observerX, observerY, observerZ;
    private static volatile boolean observerReady;
    private static volatile boolean hasSkylight = true;
    private static int directQueries;
    private static int directCaveQueries;

    private static final class Entry {
        final float exposure;
        final int tick;
        // Writes are throttled; only needed to protect frequently sampled local cells
        // when the bounded priority cache must evict older measurements.
        volatile int lastUsedTick;
        Entry(float exposure, int tick) {
            this.exposure = exposure;
            this.tick = tick;
            this.lastUsedTick = tick;
        }
    }

    // A region can be wholly sheltered OR have verified smaller sheltered cells
    // on the indoor side of a wall. The mask cannot shelter the outside half.
    private record CaveRegion(boolean sheltered, int wallInteriorMask, int measuredTick) {
        boolean shelteredAt(int x, int y, int z) {
            if (sheltered) return true;
            int bit = ((x >> WALL_CELL_SHIFT) & 1)
                | (((z >> WALL_CELL_SHIFT) & 1) << 1)
                | (((y >> WALL_CELL_SHIFT) & 1) << 2);
            return (wallInteriorMask & (1 << bit)) != 0;
        }
        boolean hasShelter() { return sheltered || wallInteriorMask != 0; }
    }

    /** Position-based, not dependent on whether the player is inside or outside. */
    private static long caveKey(int x, int y, int z) {
        return BlockPos.asLong(x >> CAVE_SHIFT, y >> CAVE_SHIFT, z >> CAVE_SHIFT);
    }

    private static boolean caveNearObserver(int x, int y, int z) {
        return observerReady && Math.abs((long)x - observerX) <= CAVE_LOCAL_RADIUS_XZ
            && Math.abs((long)y - observerY) <= CAVE_LOCAL_RADIUS_Y
            && Math.abs((long)z - observerZ) <= CAVE_LOCAL_RADIUS_XZ;
    }

    private static boolean withinUsefulDistance(int x, int y, int z) {
        return !observerReady || (Math.abs((long)x - observerX) <= MAX_WIND_DISTANCE_XZ
            && Math.abs((long)y - observerY) <= MAX_WIND_DISTANCE_Y
            && Math.abs((long)z - observerZ) <= MAX_WIND_DISTANCE_XZ);
    }

    /**
     * A high-confidence, shared shelter result takes the place of hundreds of exact
     * particle-cell surveys. We check BOTH deep cave cover and nearer roofed rooms;
     * a region close to an opening remains ambiguous and uses ordinary four-ray
     * shelter surveys. Never relies on the player's shelter status.
     */
    private static CaveRegion surveyCaveRegion(ClientLevel level, long packed, int now) {
        BlockPos region = BlockPos.of(packed);
        int minX = region.getX() << CAVE_SHIFT;
        int minZ = region.getZ() << CAVE_SHIFT;
        int minY = region.getY() << CAVE_SHIFT;
        int cx = minX + 2, cz = minZ + 2;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        // Deep caves: requiring no sky within a high, wide box avoids classifying
        // roofed courtyards or an exposed cave mouth as fully enclosed.
        if (skyBlockedAround(level, minX, minZ, cx, cz,
                minY + (1 << CAVE_SHIFT) + CAVE_PROBE_RISE, probe)) {
            return new CaveRegion(true, 0, now);
        }

        // Buildings: a deep-cave-only survey would reject *every* ordinary roof.
        // Survey at the TOP of this region's four-block-high volume instead. If
        // all internal columns and a 6-block perimeter have a roof overhead,
        // the standard six-step horizontal ray cannot see an outdoor opening
        // from the enclosed region. A window, porch or cave entrance near the
        // boundary fails this test, and keeps using precise shelter rays.
        if (skyBlockedAround(level, minX, minZ, cx, cz,
                minY + (1 << CAVE_SHIFT) - 1, probe)) {
            // Leaf-only canopies are porous. They should use the existing canopy
            // logic, rather than becoming windless indoor rooms.
            BlockPos center = new BlockPos(cx, minY + 1, cz);
            if (!openLeafCanopyAbove(level, center, 16)) {
                return new CaveRegion(true, 0, now);
            }
        }

        // The wide sky perimeter above is deliberately strict, but in a REAL house
        // it often extends OUTSIDE the walls. A sheltered 4x4x4 region near an
        // exterior wall can therefore fail it even while every particle within
        // that region has a perfect 0.0 four-ray shelter measurement.
        //
        // Refine ONLY nearby ambiguous regions, once per published region lifetime.
        // First verify every internal column at both Y boundaries is below a roof;
        // then sample four horizontal corners at both levels with our EXISTING
        // collision-aware survey. If even one sees wind (door/window/porch), this
        // region stays ambiguous and uses the original exact-cell survey path.
        // No worker-thread world queries and no per-particle raycasts.
        if (caveNearObserver(cx, minY + 2, cz)
                && roofedRegionInterior(level, minX, minY, minZ, probe)) {
            if (verifiedShelteredInterior(level, minX, minY, minZ)) {
                return new CaveRegion(true, 0, now);
            }
        }
        // The whole 4x4x4 region may span an exterior wall. Rather than forcing
        // indoor smoke to compete for an exact-cell survey on EVERY tick, only
        // refine the ambiguous region into its eight 2x2x2 microcells. A microcell
        // is shared as sheltered only when its own sky checks and collision-aware
        // ray measurements agree; exterior/opening cells remain ambiguous.
        int wallMask = 0;
        if (caveNearObserver(cx, minY + 2, cz)) {
            wallMask = classifyWallInteriorCells(level, minX, minY, minZ, probe);
        }
        return new CaveRegion(false, wallMask, now);
    }

    /**
     * A shared full region must be 100% sheltered; at a wall this is too strict.
     * Use 2x2x2 subcells only in the ambiguous nearby region, with a compact mask
     * that identifies the sheltered side without affecting the outdoor side.
     * This runs once per region refresh on the client thread, never per particle.
     */
    private static int classifyWallInteriorCells(ClientLevel level, int baseX, int baseY,
                                                 int baseZ, BlockPos.MutableBlockPos probe) {
        int mask = 0;
        for (int sy = 0; sy < 2; sy++) {
            for (int sz = 0; sz < 2; sz++) {
                for (int sx = 0; sx < 2; sx++) {
                    int x = baseX + (sx << WALL_CELL_SHIFT);
                    int y = baseY + (sy << WALL_CELL_SHIFT);
                    int z = baseZ + (sz << WALL_CELL_SHIFT);
                    if (!roofedWallCell(level, x, y, z, probe)) continue;
                    if (!verifiedWallCell(level, x, y, z)) continue;
                    int bit = sx | (sz << 1) | (sy << 2);
                    mask |= 1 << bit;
                }
            }
        }
        return mask;
    }

    /** Reject any microcell containing sky-visible space or unloaded columns. */
    private static boolean roofedWallCell(ClientLevel level, int x, int y, int z,
                                          BlockPos.MutableBlockPos probe) {
        for (int dy = 0; dy < 2; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 2; dx++) {
                    probe.set(x + dx, y + dy, z + dz);
                    if (level.isOutsideBuildHeight(probe) || !level.hasChunkAt(probe)
                        || level.canSeeSky(probe)) return false;
                }
            }
        }
        return true;
    }

    /**
     * Four staggered samples cover both heights and all horizontal corners.
     * The continuity filter resolves readings below 0.20 to no wind. Use a
     * slightly stricter 0.19 here so a DIRECT one-block opening to outside
     * (typical measurement ~0.195) is NOT shared as a sheltered microcell.
     * Borderline/open cells still follow the normal exact survey path.
     */
    private static boolean verifiedWallCell(ClientLevel level, int x, int y, int z) {
        // Do not use solid exterior wall blocks as wind samples. A survey started
        // *inside* a solid wall can see sky on the exterior face and wrongly veto
        // a perfectly protected interior cell immediately behind that wall.
        int checked = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < WALL_SAMPLE_OFFSETS.length; i += 3) {
            at.set(x + WALL_SAMPLE_OFFSETS[i],
                y + WALL_SAMPLE_OFFSETS[i + 1], z + WALL_SAMPLE_OFFSETS[i + 2]);
            BlockState state = level.getBlockState(at);
            if (!state.getCollisionShape(level, at).isEmpty()) continue;
            checked++;
            if (survey(level, at, lowCost) >= WALL_CELL_SHELTER_THRESHOLD) return false;
        }
        // An entirely solid 2x2x2 cell has no particle space worth classifying.
        return checked > 0;
    }

    /** Check the full 4x4 footprint at BOTH extremes of this 4-block-tall region. */
    private static boolean roofedRegionInterior(ClientLevel level, int x, int y, int z,
                                                 BlockPos.MutableBlockPos probe) {
        for (int dx = 0; dx < (1 << CAVE_SHIFT); dx++) {
            for (int dz = 0; dz < (1 << CAVE_SHIFT); dz++) {
                for (int levelIndex = 0; levelIndex < 2; levelIndex++) {
                    int dy = levelIndex == 0 ? 0 : (1 << CAVE_SHIFT) - 1;
                    probe.set(x + dx, y + dy, z + dz);
                    if (level.isOutsideBuildHeight(probe) || !level.hasChunkAt(probe)
                        || level.canSeeSky(probe)) return false;
                }
            }
        }
        return true;
    }

    /**
     * Conservative wall-aware refinement for roofed rooms near open-sky columns.
     * One failing sample preserves the normal partial-exposure path. A completed
     * result can be reused by EVERY particle within this region for 90 ticks.
     */
    private static boolean verifiedShelteredInterior(ClientLevel level, int x, int y, int z) {
        final int edge = (1 << CAVE_SHIFT) - 1;
        for (int levelIndex = 0; levelIndex < 2; levelIndex++) {
            int dy = levelIndex == 0 ? 0 : edge;
            for (int cxIndex = 0; cxIndex < 2; cxIndex++) {
                int dx = cxIndex == 0 ? 0 : edge;
                for (int czIndex = 0; czIndex < 2; czIndex++) {
                    int dz = czIndex == 0 ? 0 : edge;
                    if (survey(level, new BlockPos(x + dx, y + dy, z + dz), lowCost) > 0.05f)
                        return false;
                }
            }
        }
        return true;
    }

    /** Cheap sky probes only. No collision rays or chunk loading. Client thread. */
    private static boolean skyBlockedAround(ClientLevel level, int minX, int minZ,
                                            int cx, int cz, int y,
                                            BlockPos.MutableBlockPos probe) {
        for (int dx = 0; dx < (1 << CAVE_SHIFT); dx++) {
            for (int dz = 0; dz < (1 << CAVE_SHIFT); dz++) {
                probe.set(minX + dx, y, minZ + dz);
                if (level.isOutsideBuildHeight(probe) || !level.hasChunkAt(probe)
                        || level.canSeeSky(probe)) return false;
            }
        }
        for (int i = 0; i < CAVE_PROBE_OFFSETS.length; i += 2) {
            probe.set(cx + CAVE_PROBE_OFFSETS[i], y, cz + CAVE_PROBE_OFFSETS[i + 1]);
            if (level.isOutsideBuildHeight(probe) || !level.hasChunkAt(probe)
                    || level.canSeeSky(probe)) return false;
        }
        return true;
    }

    /**
     * Returns 1 for confirmed enclosed cave/room, 0 for ambiguous/exposed, -1 for pending.
     * Every region shares its result across many particles without worker world reads.
     */
    private static int caveRegionStatusOnMiss(int bx, int by, int bz, int now, boolean priority) {
        if (!hasSkylight) return 0;
        long regionKey = caveKey(bx, by, bz);
        CaveRegion result = CAVE_REGIONS.get(regionKey);
        int age = result == null ? Integer.MAX_VALUE : now - result.measuredTick;
        if (result != null && age >= 0 && age <= CAVE_LIFETIME_TICKS) {
            boolean protectedHere = result.shelteredAt(bx, by, bz);
            return protectedHere ? 1 : 0;
        }
        // Two fast new-region probes on the client thread, independent of particle count.
        ClientLevel level = currentLevel;
        if (priority && level != null && Minecraft.getInstance().isSameThread()
                && directCaveQueries < 2) {
            directCaveQueries++;
            result = surveyCaveRegion(level, regionKey, now);
            CAVE_REGIONS.put(regionKey, result);
            return result.shelteredAt(bx, by, bz) ? 1 : 0;
        }
        Set<Long> pending = priority ? CAVE_LOCAL_PENDING : CAVE_PENDING;
        if (!pending.contains(regionKey) && pending.size() < CAVE_MAX_PENDING
                && pending.add(regionKey)) {
            (priority ? CAVE_LOCAL_REQUESTS : CAVE_REQUESTS).offer(regionKey);
        }
        // A known covered region should not release a gust during its refresh.
        // Strictly bounded grace: edits to roofs/cave openings become visible soon.
        if (result != null && age >= 0
                && age <= CAVE_LIFETIME_TICKS + CAVE_REFRESH_GRACE_TICKS) {
            return result.shelteredAt(bx, by, bz) ? 1 : 0;
        }
        return -1;
    }

    /** Does not schedule any work; called while draining old exact-cell requests. */
    private static boolean cachedCaveShelter(int x, int y, int z, int now) {
        CaveRegion entry = CAVE_REGIONS.get(caveKey(x, y, z));
        return entry != null && entry.shelteredAt(x, y, z) && now >= entry.measuredTick
            && now - entry.measuredTick <= CAVE_LIFETIME_TICKS + CAVE_REFRESH_GRACE_TICKS;
    }

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

    /** Client-tick-only observer update; async particles read only published coordinates. */
    public static void setObserverPosition(double x, double y, double z) {
        observerX = Mth.floor(x);
        observerY = Mth.floor(y);
        observerZ = Mth.floor(z);
        observerReady = true;
    }

    private static boolean isNearObserver(int x, int y, int z) {
        return observerReady && Math.abs((long)x - observerX) <= LOCAL_RADIUS_XZ
            && Math.abs((long)y - observerY) <= LOCAL_RADIUS_Y
            && Math.abs((long)z - observerZ) <= LOCAL_RADIUS_XZ;
    }

    private static boolean isWithinNearRing(int x, int y, int z) {
        return observerReady && Math.abs((long)x - observerX) <= NEAR_RADIUS_XZ
            && Math.abs((long)y - observerY) <= NEAR_RADIUS_Y
            && Math.abs((long)z - observerZ) <= NEAR_RADIUS_XZ;
    }

    private static boolean isFresh(Entry entry, int now) {
        return entry != null && now >= entry.tick && now - entry.tick <= MAX_AGE_TICKS;
    }

    /** Prefer the newest stale measurement as the observer crosses cache tiers. */
    private static Entry newer(Entry first, Entry second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.tick >= second.tick ? first : second;
    }

    private static void touch(Entry entry, int now) {
        if (now - entry.lastUsedTick >= 8) entry.lastUsedTick = now;
    }

    /** Normal particles get priority access to the protected nearby cache. */
    public static float exposureAt(double x, double y, double z) {
        return exposureAtInternal(x, y, z, true, false);
    }

    /**
     * High-volume weather effects (Particle Rain / Pretty Rain) read any measurements
     * already published but only enqueue exact-cell misses into the background cache.
     * The same tiny shared cave regions can shelter deep underground weather without
     * consuming expensive four-ray surveys. Uncertain entrances remain precisely surveyed.
     * This method is worker-safe, just like the normal particle lookup.
     */
    public static float weatherExposureAt(double x, double y, double z) {
        return exposureAtInternal(x, y, z, false, false);
    }

    /**
     * Like exposureAt, but returns -1 when no usable measurement exists yet. The regular
     * particle movement hook uses this to preserve a particle's briefly known sheltered
     * state instead of interpreting a cache miss as positive outdoor evidence. Other
     * consumers retain their existing outdoor fallback for unmeasured ambiguous positions.
     */
    public static float trackedParticleExposureAt(double x, double y, double z) {
        return exposureAtInternal(x, y, z, true, true);
    }

    /**
     * Admission lookup for our own emitters, called only from their client tick.
     * A missing cache reading beneath a roof is NOT outdoor evidence. Fully
     * sky-exposed positions can, however, be admitted immediately without
     * waiting for a queue that might be busy with hundreds of rain particles.
     * This is one cheap sky check only on a candidate with no cached survey;
     * it never changes the shared particle-cache budgets or room algorithm.
     */
    public static float nativeEmitterExposureAt(ClientLevel level, double x, double y, double z) {
        float exposure = trackedParticleExposureAt(x, y, z);
        if (exposure >= 0.0f) return exposure;
        BlockPos at = BlockPos.containing(x, y, z);
        if (!level.isOutsideBuildHeight(at) && level.hasChunkAt(at)
            && level.dimensionType().hasSkyLight() && level.canSeeSky(at)) {
            // A sky-visible emitter is definitively outdoors. Publish that already
            // verified result so its newly spawned particle can read it even when
            // the background shelter queue is saturated. No extra world queries.
            CACHE.putIfAbsent(at.asLong(), new Entry(1.0f, tick));
            return 1.0f;
        }
        return -1.0f;
    }

    /** Game tick counter freezes while the singleplayer world is paused. */
    public static int shelterTick() { return tick; }

    /** Safe on the client thread and Async Particles workers; worker misses never query blocks. */
    private static float exposureAtInternal(double x, double y, double z,
                                            boolean prioritizeNearby, boolean markUnknown) {
        if (!enabled || effectiveness <= 0.0f) return 1.0f;
        final int bx = Mth.floor(x), by = Mth.floor(y), bz = Mth.floor(z);
        final long key = BlockPos.asLong(bx, by, bz);
        final int now = tick;
        final boolean priority = prioritizeNearby && isNearObserver(bx, by, bz);
        final boolean nearPriority = prioritizeNearby && !priority && isWithinNearRing(bx, by, bz);
        // Mods can keep particles ticking hundreds of blocks away.
        // Do not let those invisible particles monopolize any of our shelter queues.
        if (!withinUsefulDistance(bx, by, bz)) {

            return 0.0f;
        }

        Entry local = LOCAL_CACHE.get(key);
        Entry near = NEAR_CACHE.get(key);
        Entry background = CACHE.get(key);
        Entry entry = isFresh(local, now) ? local
            : isFresh(near, now) ? near
            : isFresh(background, now) ? background
            : newer(newer(local, near), background);
        if (isFresh(entry, now)) {

            touch(entry, now);
            return adjust(entry.exposure);
        }

        // Fast enclosed-region classification BEFORE enqueuing exact-cell surveys.
        // A sealed large room, as well as a deep cave, can reuse the same result
        // for thousands of particles even as they move between exact block cells.
        // Precise surveys still win near windows/doors and other ambiguous areas.
        int caveStatus = caveRegionStatusOnMiss(bx, by, bz, now, caveNearObserver(bx, by, bz));
        if (caveStatus == 1) {

            return adjust(0.0f);
        }
        if (caveStatus == -1) {
            // Defer expensive exact-cell requests until we know this region isn't
            // enclosed. Otherwise, moving smoke floods three expensive queues.
            if (entry != null && now >= entry.tick && now - entry.tick <= MAX_AGE_TICKS * 2) {

                touch(entry, now);
                return adjust(entry.exposure);
            }

            return markUnknown ? -1.0f : 1.0f;
        }

        // Client-thread misses get a small direct budget. Workers NEVER inspect chunks.
        ClientLevel level = currentLevel;
        if (prioritizeNearby && level != null && Minecraft.getInstance().isSameThread()
            && directQueries < MAX_DIRECT_QUERIES_PER_TICK) {
            directQueries++;
            BlockPos pos = BlockPos.of(key);
            if (!level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos)) {
                float measured = survey(level, pos, lowCost);
                (priority ? LOCAL_CACHE : nearPriority ? NEAR_CACHE : CACHE)
                    .put(key, new Entry(measured, now));

                return adjust(measured);
            }
        }

        if (priority) {
            // Prioritized visible cells must not wait behind 384 distant requests.
            if (!LOCAL_PENDING.contains(key) && LOCAL_PENDING.size() < MAX_LOCAL_REQUESTS
                && LOCAL_PENDING.add(key)) {
                LOCAL_REQUESTS.offer(key);

            }
        } else if (nearPriority) {
            // Separate ring budget: distant wind/rain particles cannot evict or
            // indefinitely starve candle/campfire shelter 10-40 blocks away.
            if (!NEAR_PENDING.contains(key) && NEAR_PENDING.size() < MAX_NEAR_REQUESTS
                && NEAR_PENDING.add(key)) {
                NEAR_REQUESTS.offer(key);

            }
        } else if (!PENDING.contains(key) && PENDING.size() < MAX_REQUESTS
            && PENDING.add(key)) {
            REQUESTS.offer(key);

        }
        // Stale-but-known exposure is safer than snapping a sheltered particle to
        // full outdoor wind during a short refresh delay. Newly unknown cells still
        // fail open until a survey arrives, preserving outdoor effects.
        if (entry != null && now >= entry.tick && now - entry.tick <= MAX_AGE_TICKS * 2) {

            touch(entry, now);
            return adjust(entry.exposure);
        }

        return markUnknown ? -1.0f : 1.0f;
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
        // Player exposure already requires a synchronous survey for audio. Publish that
        // measurement for particles too, instead of making them request it separately.
        float measured = survey(level, pos, lowCost);
        LOCAL_CACHE.put(pos.asLong(), new Entry(measured, tick));
        // Seed the player's proven cave region during the already-budgeted audio
        // survey. A few nearby particle cells can reuse this immediately, instead
        // of each starting with unknown exposure on the first cave tick.
        if (hasSkylight && measured < 0.10f) {
            long regionKey = caveKey(pos.getX(), pos.getY(), pos.getZ());
            CaveRegion existing = CAVE_REGIONS.get(regionKey);
            if (existing == null || tick - existing.measuredTick > CAVE_LIFETIME_TICKS) {
                CaveRegion region = surveyCaveRegion(level, regionKey, tick);
                CAVE_REGIONS.put(regionKey, region);
            }
        }
        return adjust(measured);
    }

    private static float adjust(float value) { return 1.0f - (1.0f - value) * effectiveness; }

    /**
     * Call once per ACTIVE client tick, before particle simulation. The caller must
     * not advance gameTick while the integrated server is paused: background client
     * tick events may still run, but particles do not move, so their measurements
     * must not expire just because the user is sitting in the ESC menu.
     */
    public static void tick(ClientLevel level, int gameTick) {
        if (currentLevel != level) {
            clear();
            currentLevel = level;
        }
        tick = gameTick;
        directCaveQueries = 0;
        directQueries = 0;
        hasSkylight = level.dimensionType().hasSkyLight();
        if (!enabled) return;
        // Prioritize enclosed-region requests. One region probe can eliminate
        // many exact-cell checks inside large buildings or cave systems.
        for (int i = 0; i < (lowCost ? 4 : 8); i++) {
            Long key = CAVE_LOCAL_REQUESTS.poll();
            if (key == null) break;
            try {
                CaveRegion existing = CAVE_REGIONS.get(key);
                if (existing != null && gameTick - existing.measuredTick <= CAVE_LIFETIME_TICKS) continue;
                BlockPos cell = BlockPos.of(key);
                int x = (cell.getX() << CAVE_SHIFT) + 2;
                int y = (cell.getY() << CAVE_SHIFT) + 2;
                int z = (cell.getZ() << CAVE_SHIFT) + 2;
                if (!withinUsefulDistance(x, y, z)) continue;
                CaveRegion region = surveyCaveRegion(level, key, gameTick);
                CAVE_REGIONS.put(key, region);
            } finally { CAVE_LOCAL_PENDING.remove(key); }
        }
        for (int i = 0; i < (lowCost ? 2 : 4); i++) {
            Long key = CAVE_REQUESTS.poll();
            if (key == null) break;
            try {
                CaveRegion existing = CAVE_REGIONS.get(key);
                if (existing != null && gameTick - existing.measuredTick <= CAVE_LIFETIME_TICKS) continue;
                BlockPos cell = BlockPos.of(key);
                if (!withinUsefulDistance(cell.getX() << CAVE_SHIFT,
                        cell.getY() << CAVE_SHIFT, cell.getZ() << CAVE_SHIFT)) continue;
                CaveRegion region = surveyCaveRegion(level, key, gameTick);
                CAVE_REGIONS.put(key, region);
            } finally { CAVE_PENDING.remove(key); }
        }
        if (gameTick % 40 == 0 || CAVE_REGIONS.size() > CAVE_MAX_REGIONS) {
            CAVE_REGIONS.entrySet().removeIf(e -> gameTick - e.getValue().measuredTick > CAVE_LIFETIME_TICKS * 2);
            int excess = CAVE_REGIONS.size() - CAVE_MAX_REGIONS;
            if (excess > 0) {
                ArrayList<Map.Entry<Long, CaveRegion>> oldest = new ArrayList<>(CAVE_REGIONS.entrySet());
                // Preserve confidently sheltered rooms/caves when a thousand
                // unrelated outdoor particle regions churn through the cache.
                // Within each category, remove the oldest measurement first.
                oldest.sort(Comparator
                    .comparing((Map.Entry<Long, CaveRegion> e) -> e.getValue().hasShelter())
                    .thenComparingInt(e -> e.getValue().measuredTick));
                for (Map.Entry<Long, CaveRegion> entry : oldest) {
                    if (excess <= 0) break;
                    if (CAVE_REGIONS.remove(entry.getKey(), entry.getValue())) excess--;
                }
            }
        }

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
        // Near-camera positions use their own queue and retain measured shelter
        // independently of outside particle churn. Avoid additional per-particle
        // world access; all surveys remain bounded to the client tick.
        int localBudget = lowCost ? 4 : 12;
        for (int i = 0; i < localBudget; i++) {
            Long key = LOCAL_REQUESTS.poll();
            if (key == null) break;
            BlockPos at = BlockPos.of(key);
            try {
                if (level.isOutsideBuildHeight(at) || !level.hasChunkAt(at)) continue;
                // When the player moves, old requests are not promoted into the hot cache.
                if (!isNearObserver(at.getX(), at.getY(), at.getZ())) continue;
                if (cachedCaveShelter(at.getX(), at.getY(), at.getZ(), gameTick)) continue;
                LOCAL_CACHE.put(key, new Entry(survey(level, at, lowCost), gameTick));

            } finally {
                LOCAL_PENDING.remove(key);
            }
        }
        // Medium-range visible smoke/torches get their own bounded exact-cell
        // budget; this is separate from both the immediate 7-block hot zone and
        // the low-priority background queue. All block reads stay on client tick.
        int nearBudget = lowCost ? 5 : NEAR_SURVEYS_PER_TICK;
        for (int i = 0; i < nearBudget; i++) {
            Long key = NEAR_REQUESTS.poll();
            if (key == null) break;
            BlockPos at = BlockPos.of(key);
            try {
                if (level.isOutsideBuildHeight(at) || !level.hasChunkAt(at)) continue;
                if (!isWithinNearRing(at.getX(), at.getY(), at.getZ())) continue;
                if (cachedCaveShelter(at.getX(), at.getY(), at.getZ(), gameTick)) continue;
                // Prefer a fresh existing reading if another path has meanwhile
                // populated this cell, rather than surveying unnecessarily.
                Entry local = LOCAL_CACHE.get(key);
                Entry recent = NEAR_CACHE.get(key);
                if (isFresh(local, gameTick) || isFresh(recent, gameTick)) continue;
                NEAR_CACHE.put(key, new Entry(survey(level, at, lowCost), gameTick));

            } finally {
                NEAR_PENDING.remove(key);
            }
        }
        int budget = lowCost ? 1 : 3;
        for (int i = 0; i < budget; i++) {
            Long key = REQUESTS.poll();
            if (key == null) break;
            BlockPos at = BlockPos.of(key);
            try {
                if (level.isOutsideBuildHeight(at) || !level.hasChunkAt(at)) continue;
                if (!withinUsefulDistance(at.getX(), at.getY(), at.getZ())) continue;
                if (cachedCaveShelter(at.getX(), at.getY(), at.getZ(), gameTick)) continue;
                CACHE.put(key, new Entry(survey(level, at, lowCost), gameTick));

            } finally {
                PENDING.remove(key);
            }
        }
        // Protected caches are independent; distant background storms cannot evict
        // the player's hot-zone or larger-room exact-cell readings.
        if (gameTick % LOCAL_TRIM_INTERVAL == 0 || LOCAL_CACHE.size() > MAX_LOCAL_ZONES + 256) {
            trimLocal(gameTick);
        }
        if (gameTick % NEAR_TRIM_INTERVAL == 0 || NEAR_CACHE.size() > MAX_NEAR_ZONES + 256) {
            trimNear(gameTick);
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

    private static void trimLocal(int gameTick) {
        // Removal is on the client thread and at most once per 20 ticks normally.
        // Worker lookups are lock-free and only update a volatile access timestamp.
        LOCAL_CACHE.entrySet().removeIf(e -> gameTick - e.getValue().lastUsedTick > MAX_AGE_TICKS * 3);
        int excess = LOCAL_CACHE.size() - MAX_LOCAL_ZONES;
        if (excess <= 0) return;
        ArrayList<Map.Entry<Long, Entry>> sorted = new ArrayList<>(LOCAL_CACHE.entrySet());
        sorted.sort(Comparator.comparingInt(e -> e.getValue().lastUsedTick));
        for (Map.Entry<Long, Entry> e : sorted) {
            if (excess <= 0) break;
            if (LOCAL_CACHE.remove(e.getKey(), e.getValue())) excess--;
        }
    }

    private static void trimNear(int gameTick) {
        NEAR_CACHE.entrySet().removeIf(e -> gameTick - e.getValue().lastUsedTick > MAX_AGE_TICKS * 3);
        int excess = NEAR_CACHE.size() - MAX_NEAR_ZONES;
        if (excess <= 0) return;
        ArrayList<Map.Entry<Long, Entry>> sorted = new ArrayList<>(NEAR_CACHE.entrySet());
        sorted.sort(Comparator.comparingInt(e -> e.getValue().lastUsedTick));
        for (Map.Entry<Long, Entry> e : sorted) {
            if (excess <= 0) break;
            if (NEAR_CACHE.remove(e.getKey(), e.getValue())) excess--;
        }
    }

    public static void clear() {
        CACHE.clear(); PENDING.clear(); REQUESTS.clear();
        NEAR_CACHE.clear(); NEAR_PENDING.clear(); NEAR_REQUESTS.clear();
        LOCAL_CACHE.clear(); LOCAL_PENDING.clear(); LOCAL_REQUESTS.clear();
        STATIC_CACHE.clear(); STATIC_PENDING.clear(); STATIC_REQUESTS.clear();
        CAVE_REGIONS.clear(); CAVE_PENDING.clear(); CAVE_LOCAL_PENDING.clear();
        CAVE_LOCAL_REQUESTS.clear(); CAVE_REQUESTS.clear();
        currentLevel = null; tick = 0; directQueries = 0; directCaveQueries = 0;
        observerReady = false; hasSkylight = true;

    }

    private static long key(double x, double y, double z) {
        return BlockPos.asLong(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    // The lightweight four-ray shelter survey uses exact-block sampling to avoid
    // incorrectly treating positions across a one-block wall as the same cell.
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
        float exposure = Mth.clamp(lateral * (0.72f + visible * 0.06f), 0.0f, 0.95f);
        // Leaves block Minecraft's regular canSeeSky check, even though a canopy
        // is porous rather than a house roof. Only check ambiguous low-exposure
        // cells, and only during this already budgeted shelter survey. A non-leaf
        // collision block encountered overhead vetoes the canopy exception, so
        // actual ceilings, caves and houses remain sheltered.
        if (exposure < 0.60f && openLeafCanopyAbove(level, at, cheap ? 9 : 16)) {
            exposure = 0.60f;
        }
        return exposure;
    }

    /**
     * Detect an open, leaf-only canopy directly above a shaded particle cell.
     * No extra cache, requests, or per-particle world access: this runs solely
     * inside the existing bounded client-thread shelter survey.
     */
    private static boolean openLeafCanopyAbove(ClientLevel level, BlockPos at, int maxRise) {
        BlockPos.MutableBlockPos probe = at.mutable();
        boolean passedLeaves = false;
        for (int dy = 1; dy <= maxRise; dy++) {
            probe.move(Direction.UP);
            if (level.isOutsideBuildHeight(probe) || !level.hasChunkAt(probe)) return false;
            BlockState state = level.getBlockState(probe);
            if (state.is(BlockTags.LEAVES)) {
                passedLeaves = true;
                continue;
            }
            // Do not let trees above a solid house roof turn indoor shelter off.
            if (!state.getCollisionShape(level, probe).isEmpty()) return false;
            // Only a leaf canopy with actual open sky beyond it is an exception;
            // decorative indoor leaves beneath a roof do not count.
            if (passedLeaves && level.canSeeSky(probe)) return true;
        }
        return false;
    }
}
