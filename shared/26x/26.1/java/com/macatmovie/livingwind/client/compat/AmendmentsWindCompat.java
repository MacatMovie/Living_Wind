package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;

/**
 * Client-side wind force for Amendments' own pendulum animations.
 * Does not replace their angle, damping, collision impulse, or sign-attachment rules.
 * Never loads Amendments classes: only the optional mixin targets them.
 */
public final class AmendmentsWindCompat {
    private static final String WALL_LANTERN_CLASS =
            "net.mehvahdjukaar.amendments.common.block.WallLanternBlock";

    private AmendmentsWindCompat() {}

    public static double windImpulse(Level level, BlockPos pos, BlockState state) {
        if (!ClientWindConfig.AMENDMENTS_WIND_COMPAT.get() ||
                !ClientWindState.hasServerState() || level == null || pos == null || state == null ||
                level != Minecraft.getInstance().level || !level.getFluidState(pos).isEmpty()) return 0.0;

        double axisX, axisZ;
        if (state.getBlock() instanceof WallHangingSignBlock) {
            var axis = state.getValue(WallHangingSignBlock.FACING).getClockWise();
            axisX = axis.getStepX();
            axisZ = axis.getStepZ();
        } else if (state.getBlock() instanceof CeilingHangingSignBlock) {
            // Match HangingSignTileExtension.getRotationAxis in both Amendments releases.
            // ATTACHED signs are handled by Amendments itself (no pendulum tick).
            double radians = Math.toRadians(90 + RotationSegment.convertToDegrees(
                    state.getValue(CeilingHangingSignBlock.ROTATION)));
            axisX = Math.sin(radians);
            axisZ = Math.cos(radians);
        } else if (WALL_LANTERN_CLASS.equals(state.getBlock().getClass().getName()) &&
                state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            // Match WallLanternBlockTile.getRotationAxis; avoid affecting other blocks.
            var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            axisX = facing.getStepX();
            axisZ = facing.getStepZ();
        } else return 0.0;

        double strength = ClientWindState.strengthAt(pos.getX() + 0.5,
                pos.getY() + 0.5, pos.getZ() + 0.5);
        if (strength <= 0.0001) return 0.0;
        double exposure = WindShelter.exposureForStaticAnimation(pos);
        if (exposure <= 0.0001) return 0.0;

        float degrees = ClientWindState.directionDegrees();
        double projection = AmendmentsWindMath.projection(
                WindMath.windX(1.0f, degrees), WindMath.windZ(1.0f, degrees), axisX, axisZ);
        double phase = level.getGameTime() * 0.17 +
                pos.getX() * 0.69 + pos.getY() * 0.22 + pos.getZ() * 0.45;
        return AmendmentsWindMath.impulse(strength,
                ClientWindConfig.AMENDMENTS_WIND_MULTIPLIER.get(), exposure, projection, phase);
    }
}
