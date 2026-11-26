package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.client.access.WindParticleAccess;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.macatmovie.livingwind.wind.WindMath;
import org.jetbrains.annotations.Nullable;

/**
 * SkyLands (NeoForge 1.21.1) compatibility without importing optional classes.
 * Cloud motion replaces, rather than adds to, SkyLands' diagonal native drift.
 * The block and cactus origin checks run ONLY on particle creation and ONLY
 * on the client main thread, not on async particle-movement workers.
 */
public final class SkyLandsParticleWindCompat {
    private static final Identifier BIG_CLOUD = Identifier.tryParse("skylands:cloud_barrier_3");
    private static final Identifier SMALL_CLOUD = Identifier.tryParse("skylands:cloud");
    private static final Identifier STAFF = Identifier.tryParse("skylands:cloud_staff");
    private static final Identifier CLOUD_BLOCK = Identifier.tryParse("skylands:cloud_block");
    private static final Identifier SOLERO = Identifier.tryParse("skylands:solero_cactus");
    private static final Identifier LUNERO = Identifier.tryParse("skylands:lunero_cactus");
    private static final Identifier VANILLA_FLAME = Identifier.tryParse("minecraft:flame");
    private static final Identifier VANILLA_SNOWFLAKE = Identifier.tryParse("minecraft:snowflake");
    private static final Identifier CACTUS_PIXEL = Identifier.tryParse("skylands:cactus_warm_pixel");
    private static final Identifier STORM_SMOKE = Identifier.tryParse("skylands:cloud_smoke");
    private static final Identifier STORM_SMOKE_1 = Identifier.tryParse("skylands:cloud_smoke_1");

    // World reads happen at spawn, not every particle movement tick. Cache the
    // local cactus proximity probe for 10 ticks to avoid repeated 125-block
    // scans when snowflake/flame emitters produce lots of particles.
    private record CactusProbe(int x, int y, int z, boolean flame) { }
    private static final Map<CactusProbe, Boolean> cactusProbeCache = new HashMap<>();
    private static ClientLevel cachedLevel;
    private static long cachedEpoch = Long.MIN_VALUE;

    private SkyLandsParticleWindCompat() { }

    public static boolean isCloud(@Nullable Identifier id) {
        return BIG_CLOUD.equals(id) || SMALL_CLOUD.equals(id);
    }

    /**
     * Called only at creation. Vanilla particles have no source identity: we
     * only exempt them when their spawn falls near the matching active cactus.
     * Use a bounded block search, not a world scan. Under async creation we
     * intentionally skip the lookup rather than accessing unloaded chunks.
     */
    public static boolean sourceAnchoredAtSpawn(@Nullable Identifier id, double x, double y, double z,
                                                 double vx, double vy, double vz) {
        // StormBlock.animateTick is the only SkyLands emitter giving cloud_smoke
        // an exact 0.65 horizontal velocity and 0.08 vertical velocity. Identify
        // the source by its spawn signature, NOT its shared particle ID: thrown
        // Smoke Flowers use these same particles with different velocities.
        // This avoids scanning 20-block-high storm volumes on every particle spawn.
        if ((STORM_SMOKE.equals(id) || STORM_SMOKE_1.equals(id))
                && Math.abs(vy - 0.08) < 1.0e-5
                && Math.abs(Math.hypot(vx, vz) - 0.65) < 1.0e-5) return true;
        if (!SMALL_CLOUD.equals(id) && !VANILLA_FLAME.equals(id) && !VANILLA_SNOWFLAKE.equals(id)) return false;
        if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(CACTUS_PIXEL)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return false;
        ClientLevel level = mc.level;
        if (level == null) return false;
        BlockPos origin = BlockPos.containing(x, y, z);
        if (SMALL_CLOUD.equals(id)) {
            if (mc.player == null || !STAFF.equals(BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem())))
                return false;
            if (!level.hasChunkAt(origin)) return false;
            return CLOUD_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(origin).getBlock()));
        }
        boolean flame = VANILLA_FLAME.equals(id);
        Identifier matching = flame ? SOLERO : LUNERO;
        long epoch = level.getGameTime() / 10;
        if (level != cachedLevel || epoch != cachedEpoch) {
            cachedLevel = level;
            cachedEpoch = epoch;
            cactusProbeCache.clear();
        }
        CactusProbe probe = new CactusProbe(origin.getX(), origin.getY(), origin.getZ(), flame);
        Boolean cached = cactusProbeCache.get(probe);
        if (cached != null) return cached;
        // Cactus packets scatter particles around their TOP segments; limit
        // candidate checks to 5x5x5, and do not load any unloaded chunks.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
            cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            if (!level.hasChunkAt(cursor)) continue;
            if (matching.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(cursor).getBlock()))) {
                cacheCactusProbe(probe, true);
                return true;
            }
        }
        cacheCactusProbe(probe, false);
        return false;
    }

    private static void cacheCactusProbe(CactusProbe probe, boolean near) {
        if (cactusProbeCache.size() >= 384) cactusProbeCache.clear();
        cactusProbeCache.put(probe, near);
    }

    /** Return NaN when the stock movement (and generic filter) should apply. */
    public static double replacementX(@Nullable Identifier id, @Nullable Particle particle) {
        return replacement(id, particle, true);
    }

    public static double replacementZ(@Nullable Identifier id, @Nullable Particle particle) {
        return replacement(id, particle, false);
    }

    private static double replacement(@Nullable Identifier id, @Nullable Particle particle, boolean xAxis) {
        if (!isCloud(id) || particle == null || !(particle instanceof WindParticleAccess access)
                || access.livingwind$isSkylandsSourceAnchored()
                || !ClientWindState.hasServerState() || ClientWindState.dimensionWindDisabled()
                || !ParticleWindFilter.shouldAffect(id, particle)
                || ParticleWaterProtection.isSubmerged(access.livingwind$getPositionX(),
                        access.livingwind$getPositionY(), access.livingwind$getPositionZ())) return Double.NaN;

        double px = access.livingwind$getPositionX();
        double py = access.livingwind$getPositionY();
        double pz = access.livingwind$getPositionZ();
        double vx = ClientWindState.windXAt(px, py, pz);
        double vz = ClientWindState.windZAt(px, py, pz);
        double length = Math.hypot(vx, vz);
        boolean bigCloud = BIG_CLOUD.equals(id);
        if (length < 0.0001) {
            if (!bigCloud) return 0.0;
            // Even perfectly calm weather retains the current wind direction
            // and a very slow, natural drift for these enormous long-lived clouds.
            vx = WindMath.windX(1.0f, ClientWindState.directionDegrees());
            vz = WindMath.windZ(1.0f, ClientWindState.directionDegrees());
            length = Math.hypot(vx, vz);
            if (length < 0.0001) return 0.0;
        }
        // Both large and small clouds replace SkyLands' own diagonal movement.
        // Larger clouds deliberately respond several times faster than before,
        // but retain a low speed cap and per-particle user multipliers.
        double windStrength = Math.hypot(ClientWindState.windXAt(px, py, pz),
                                         ClientWindState.windZAt(px, py, pz));
        double response = Math.max(0.0, ParticleWindFilter.skylandsParticleResponse(id));
        double speed = bigCloud
                ? (0.020 + 0.085 * Mth.clamp(windStrength, 0.0, 3.0)) * response
                : Mth.clamp(windStrength, 0.0, 1.7) * 0.009 * response;
        speed = Math.min(speed, bigCloud ? 0.15 : 0.014);
        return (xAxis ? vx : vz) / length * speed;
    }
}
