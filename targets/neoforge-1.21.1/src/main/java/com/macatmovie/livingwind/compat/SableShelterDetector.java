package com.macatmovie.livingwind.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** NeoForge 1.21.1 only. Load through SableShelterCompat when the optional Sable mod is present.
 * Reuses Sable's world-space raycasting across moving/rotated sub-level geometry.
 * No contraption block scans, player movement hacks, or per-particle queries. */
final class SableShelterDetector {
    private static final double ROOF_RISE = 10.0;
    private static final double SIDE_REACH = 5.0;

    private SableShelterDetector() {}

    static float playerExposure(ClientLevel level, Vec3 eye, float baseExposure, float effectiveness) {
        // No Sable container can exist while joining a world; avoid an early
        // lookup failure disabling this optional integration for the session.
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || container.getLoadedCount() == 0) return baseExposure;
        // Fast no-ship path. Sable queries loaded sub-level bounds, not their blocks.
        Iterable<SubLevel> nearby = Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(
            eye.x - SIDE_REACH, eye.y - 2.0, eye.z - SIDE_REACH,
            eye.x + SIDE_REACH, eye.y + ROOF_RISE, eye.z + SIDE_REACH));
        if (!nearby.iterator().hasNext()) return baseExposure;

        // An uncovered flight deck stays windy, even if its floor or railings are solid.
        // Check the short roof ray before probing the lateral walls.
        boolean roofed = blocksWind(level, eye, eye.add(0.0, ROOF_RISE, 0.0));
        if (!roofed) return baseExposure;

        int walls = 0;
        if (blocksWind(level, eye, eye.add(SIDE_REACH, 0.0, 0.0))) walls++;
        if (blocksWind(level, eye, eye.add(-SIDE_REACH, 0.0, 0.0))) walls++;
        if (blocksWind(level, eye, eye.add(0.0, 0.0, SIDE_REACH))) walls++;
        if (blocksWind(level, eye, eye.add(0.0, 0.0, -SIDE_REACH))) walls++;

        // Roof alone: substantially quieter, not soundless under an open canopy.
        // Four walls + roof: almost silent, like a sealed ordinary-world cabin.
        float sublevelExposure = 0.38f * (1.0f - 0.86f * (walls / 4.0f));
        // The usual indoor effectiveness slider also controls moving-ship shelter.
        float adjusted = 1.0f - (1.0f - sublevelExposure) * Mth.clamp(effectiveness, 0.0f, 1.0f);
        return Math.min(baseExposure, adjusted);
    }

    private static boolean blocksWind(ClientLevel level, Vec3 from, Vec3 to) {
        ClipContext clip = new ClipContext(from, to,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        // Otherwise a vanilla hill or house near an airship would be counted as
        // an airship wall. Existing WindShelter already handles normal blocks.
        ((ClipContextExtension) clip).sable$setIgnoreMainLevel(true);
        return level.clip(clip).getType() == HitResult.Type.BLOCK;
    }
}
