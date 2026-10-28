package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Client-only, entirely optional Chimes animation and ambient sound support.
 * Chimes calculates all native interactions/rocking. We add temporary rotations
 * after its calculations, then restore every part before returning the renderer.
 */
public final class ChimesWindMotion {
    /** Two-block chimes have separate block entities, but must have ONE smooth wind phase.
     * The anchor is always the upper block. Keys are scoped to the current client Level.
     */
    private static final WeakHashMap<Level, Map<Long, Motion>> MOTION = new WeakHashMap<>();
    private static final int MAX_MOTIONS = 2048;
    private static final long STALE_NS = 30_000_000_000L;
    private static final class Motion {
        long lastNs;
        long lastSeenNs;
        double phase;
        float x, z;
    }

    public enum Type {
        BAMBOO, AMETHYST, COPPER, IRON, GLASS
    }

    private ChimesWindMotion() {}

    private static boolean active() {
        return ClientWindConfig.CHIMES_WIND_COMPAT.get() && ClientWindState.hasServerState();
    }

    private static float localWind(BlockPos pos, Level level) {
        if (level == null) return 0f;
        float exposed = WindShelter.exposureForStaticAnimation(pos);
        return Math.max(0f, ClientWindState.strengthAt(pos.getX(), pos.getY(), pos.getZ())
                * Mth.clamp(exposed, 0f, 1f));
    }

    /** Cache one motion per physical two-block chime, not per individual block entity.
     * Both halves receive the same target/phase, preventing independently lagged sways.
     */
    private static Motion motionFor(Level level, BlockPos pos, long nowNs) {
        synchronized (MOTION) {
            Map<Long, Motion> map = MOTION.computeIfAbsent(level, k -> new HashMap<>());
            Motion motion = map.computeIfAbsent(pos.asLong(), k -> new Motion());
            motion.lastSeenNs = nowNs;
            if (map.size() > MAX_MOTIONS) {
                map.entrySet().removeIf(entry -> nowNs - entry.getValue().lastSeenNs > STALE_NS);
                if (map.size() > MAX_MOTIONS) {
                    // Make this bounded even when the player rapidly explores thousands
                    // of chimes. Evict the oldest state rather than doing a chunk scan.
                    long oldestKey = 0L, oldestTime = Long.MAX_VALUE;
                    for (Map.Entry<Long, Motion> entry : map.entrySet()) {
                        if (entry.getValue().lastSeenNs < oldestTime) {
                            oldestTime = entry.getValue().lastSeenNs;
                            oldestKey = entry.getKey();
                        }
                    }
                    map.remove(oldestKey);
                }
            }
            return motion;
        }
    }

    private static boolean lowerHalf(BlockEntity be) {
        return be.getBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && be.getBlockState().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
    }

    /**
     * Adds short, smoothed wind motion to Chimes' native rocking. Both model halves
     * use the same world-space upper-block anchor, exposure and phase, so the
     * separate upper and lower block entities cannot calculate different sway.
     * Chimes' different baked model pivots already align in world space (the
     * lower root is shifted by a block); do NOT translate them independently.
     *
     * Rotations are restored after rendering (the renderer shares its models).
     */
    public static boolean apply(BlockEntity be, ModelPart[] parts,
                                float[] previousX, float[] previousZ,
                                float parentYaw, Type type) {
        if (!active() || be == null || be.getLevel() == null || parts == null
                || ClientWindConfig.CHIMES_ANIMATION_WIND_MULTIPLIER.get() <= 0) return false;
        boolean lower = lowerHalf(be);
        BlockPos anchor = lower ? be.getBlockPos().above() : be.getBlockPos();
        float strength = localWind(anchor, be.getLevel());
        float magnitude = Mth.clamp(strength * ClientWindConfig.CHIMES_ANIMATION_WIND_MULTIPLIER.get().floatValue(), 0f, 2.5f);
        float gust = Mth.clamp(ClientWindState.gustStrength(), 0f, 2f);
        // Four times the initial v1.3.3 wind-induced tilt (2x the last test),
        // including the cap. Preserve config scaling and smooth transitions.
        float amplitude = 4f * Mth.clamp(magnitude * (0.13f + 0.018f * gust), 0f, 0.34f);
        float wx = WindMath.windX(amplitude, ClientWindState.directionDegrees());
        float wz = WindMath.windZ(amplitude, ClientWindState.directionDegrees());
        // Chimes uses different model-part transform hierarchies. In particular,
        // BambooUpper and CarvedUpper have zRot=PI and a facing yRot on the SAME
        // parent. ModelPart applies Z before Y, so the resulting transform is
        // R_z(PI) * R_y(yaw), not R_y(yaw) * R_z(PI). The difference only changes
        // the sign of the east/west placements, which otherwise bend upwind.
        // Convert world wind displacement into the local string xRot/zRot axes.
        float co = (float)Math.cos(parentYaw);
        float si = (float)Math.sin(parentYaw);
        float targetX, targetZ;
        if (type == Type.BAMBOO) {
            // Inverse of the actual bamboo/carved-bamboo parent transform.
            // North/south placements keep their existing response; east/west
            // placements correctly follow the wind instead of opposing it.
            targetX = -si * wx + co * wz;
            targetZ = co * wx + si * wz;
        } else if (type == Type.COPPER) {
            float localX = co * wx - si * wz;
            float localZ = si * wx + co * wz;
            targetX = -localZ;
            targetZ = -localX;
        } else if (type == Type.AMETHYST || type == Type.IRON) {
            float localX = co * wx + si * wz;
            float localZ = si * wx - co * wz;
            targetX = localZ;
            targetZ = -localX;
        } else {
            // Glass bells have a separate hierarchy and aren't facing-based.
            // The north/south (world Z) response uses the opposite model-space
            // X rotation sign from the tall chimes. East/west sway is unchanged.
            float localX = co * wx - si * wz;
            float localZ = si * wx + co * wz;
            targetX = localZ;
            targetZ = localX;
        }
        // Amethyst has an inverted string pivot relative to the parent.
        // Bamboo's correction is handled directly by its proper transform above;
        // copper/bronze, iron, and glass remain exactly as previously tested.
        if (type == Type.AMETHYST) {
            targetX = -targetX;
            targetZ = -targetZ;
        }
        long now = System.nanoTime();
        Motion m = motionFor(be.getLevel(), anchor, now);
        if (m.lastNs == 0 || now < m.lastNs || now - m.lastNs > 2_000_000_000L) {
            m.x = targetX;
            m.z = targetZ;
            m.phase = anchor.getX() * 0.63 + anchor.getZ() * 0.41;
        } else {
            float dt = Mth.clamp((now - m.lastNs) * 1e-9f, 0f, 0.1f);
            float smoothing = 1f - (float)Math.exp(-dt / 0.28f);
            m.x += (targetX - m.x) * smoothing;
            m.z += (targetZ - m.z) * smoothing;
            // Accumulate the phase over elapsed time: NEVER multiply an absolute
            // timestamp by a wind-dependent frequency (jumps whenever wind changes).
            m.phase = (m.phase + dt * (1.1f + Math.min(2f, strength) * 0.35f)) % (Math.PI * 2);
        }
        m.lastNs = now;
        float wave = 0.86f + 0.14f * (float)Math.sin(m.phase);
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            previousX[i] = part.xRot;
            previousZ[i] = part.zRot;
            float variation = 0.78f + (i % 4) * 0.065f;
            float addX = m.x * variation * wave;
            float addZ = m.z * variation * wave;
            part.xRot += addX;
            part.zRot += addZ;
        }
        return true;
    }

    public static void restore(ModelPart[] parts,
                               float[] previousX, float[] previousZ) {
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            part.xRot = previousX[i];
            part.zRot = previousZ[i];
        }
    }

    /** Only ambient random-chime chances are changed; clicks, hits and ties are native. */
    public static float adjustedChance(float vanilla, Level level, BlockPos pos) {
        if (!active() || level == null) return vanilla;
        float intensity = ClientWindConfig.CHIMES_SOUND_WIND_MULTIPLIER.get().floatValue();
        if (intensity <= 0f) return vanilla;
        float wind = localWind(pos, level);
        float scale = Mth.clamp(0.20f + wind * 0.85f, 0.04f, 3.2f);
        float factor = Mth.clamp(1f + (scale - 1f) * intensity, 0.03f, 5f);
        return Mth.clamp(vanilla * factor, 0.00001f, 0.80f);
    }

    public static int adjustedDenominator(int original, Level level, BlockPos pos) {
        float prob = adjustedChance(1f / original, level, pos);
        return Mth.clamp(Math.round(1f / prob), 2, 100000);
    }
}
