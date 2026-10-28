package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.wind.WindBiomeModifiers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Immutable, periodically sampled client biome map. Particle hooks may run on AsyncParticles'
 * workers: those hooks only read this published array and NEVER access chunks or biome registries.
 * A small bilinear border between samples prevents a hard jump at biome boundaries.
 */
public final class ClientBiomeWindCache {
    private static final int STEP = 8;
    private static final int SIZE = 15;
    private static final int RADIUS = SIZE / 2;
    private static volatile Snapshot snapshot;
    private static int lastUpdateTick = Integer.MIN_VALUE;

    private record Snapshot(int originX, int originZ, int y, float[] samples) {}

    private ClientBiomeWindCache() {}

    /** Called on the render/client tick thread, never from particle workers. */
    public static void tick(ClientLevel level, Player player, int tick) {
        if (level.dimension() != Level.NETHER || ClientWindState.dimensionWindDisabled()
                || !level.registryAccess().registryOrThrow(Registries.BIOME)
                    .containsKey(WindBiomeModifiers.BLACK_ICE_GLACIERS.location())) {
            snapshot = null;
            ClientWindState.setBiomeWeight(0.0f);
            return;
        }
        ClientWindState.setBiomeWeight(
            level.getBiome(player.blockPosition()).is(WindBiomeModifiers.BLACK_ICE_GLACIERS) ? 1.0f : 0.0f);

        Snapshot old = snapshot;
        int px = player.getBlockX(), pz = player.getBlockZ(), py = player.getBlockY();
        // The snapshot moves with the player and refreshes when the biome may change vertically.
        if (old != null && tick - lastUpdateTick < 12
                && Math.abs(px - (old.originX + RADIUS * STEP)) < 7
                && Math.abs(pz - (old.originZ + RADIUS * STEP)) < 7
                && Math.abs(py - old.y) < 8) return;

        int x0 = px - RADIUS * STEP, z0 = pz - RADIUS * STEP;
        float[] sampled = new float[SIZE * SIZE];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < SIZE; z++) {
            int bz = z0 + z * STEP;
            for (int x = 0; x < SIZE; x++) {
                int bx = x0 + x * STEP;
                if (level.hasChunk(bx >> 4, bz >> 4)) {
                    sampled[z * SIZE + x] = level.getBiome(pos.set(bx, py, bz))
                        .is(WindBiomeModifiers.BLACK_ICE_GLACIERS) ? 1.0f : 0.0f;
                }
            }
        }
        snapshot = new Snapshot(x0, z0, py, sampled);
        lastUpdateTick = tick;
    }

    /** Fast lock-free lookup usable on the particle worker threads. */
    public static float glacierWeightAt(double x, double z) {
        Snapshot grid = snapshot;
        if (grid == null) return 0.0f;
        float gx = (float) ((x - grid.originX) / STEP);
        float gz = (float) ((z - grid.originZ) / STEP);
        if (gx < 0 || gz < 0 || gx >= SIZE - 1 || gz >= SIZE - 1) return 0.0f;
        int ix = (int) gx, iz = (int) gz;
        float fx = gx - ix, fz = gz - iz;
        int i = iz * SIZE + ix;
        float[] a = grid.samples;
        float top = a[i] + (a[i + 1] - a[i]) * fx;
        float bottom = a[i + SIZE] + (a[i + SIZE + 1] - a[i + SIZE]) * fx;
        return top + (bottom - top) * fz;
    }

    public static void reset() {
        snapshot = null;
        lastUpdateTick = Integer.MIN_VALUE;
    }
}
