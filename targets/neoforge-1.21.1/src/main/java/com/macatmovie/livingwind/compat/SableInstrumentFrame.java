package com.macatmovie.livingwind.compat;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;

/** Translate a Sable plot's instrument center and local horizontal axes to world space. */
final class SableInstrumentFrame {
    private SableInstrumentFrame() {}

    static InstrumentSpatialContext.Frame from(Pose3dc pose, BlockPos storedPosition) {
        Vector3d world = pose.transformPosition(new Vector3d(
            storedPosition.getX() + 0.5, storedPosition.getY() + 0.5, storedPosition.getZ() + 0.5));
        Vector3d right = pose.transformNormal(new Vector3d(1, 0, 0));
        Vector3d forward = pose.transformNormal(new Vector3d(0, 0, 1));
        // Coordinates inside a Sable plot are storage coordinates; world is the visible instrument location.
        return new InstrumentSpatialContext.Frame(
            BlockPos.containing(world.x, world.y, world.z), true,
            right.x, right.z, forward.x, forward.z);
    }
}
