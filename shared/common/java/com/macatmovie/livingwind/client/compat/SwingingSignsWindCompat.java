package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

/** Client-only bridge; keeps Swinging Hanging Signs' spring/push physics untouched. */
public final class SwingingSignsWindCompat {
    private SwingingSignsWindCompat() {}

    public static float ambientSway(float original, BlockPos pos, BlockState state, float partialTick) {
        if (!ClientWindConfig.SWINGING_SIGNS_COMPAT.get() || !ClientWindState.hasServerState())
            return original;
        Minecraft minecraft = Minecraft.getInstance();
        if (pos == null || state == null || minecraft.level == null) return original;

        double strength = ClientWindState.strengthAtAltitude(pos.getY(),
                minecraft.level.dimension() == Level.OVERWORLD)
                * ClientWindConfig.SWINGING_SIGNS_WIND_MULTIPLIER.get();
        if (!Double.isFinite(strength) || strength <= 0.0001) return 0.0f;
        float exposure = WindShelter.exposureForStaticAnimation(pos);
        strength = Mth.clamp(strength * exposure, 0.0, 2.5);
        if (strength < 0.0001) return 0.0f;

        // Match Swinging Signs' own yaw convention (same axis as its SignSpring.pushTarget).
        float yawDegrees;
        if (state.getBlock() instanceof CeilingHangingSignBlock) {
            yawDegrees = RotationSegment.convertToDegrees(state.getValue(CeilingHangingSignBlock.ROTATION));
        } else if (state.getBlock() instanceof WallHangingSignBlock) {
            yawDegrees = state.getValue(WallHangingSignBlock.FACING).toYRot();
        } else return original;

        // Living Wind degrees indicate the direction wind travels toward.
        // Project that vector on the sign's one permitted swinging axis.
        double projection = Math.cos(Math.toRadians(ClientWindState.directionDegrees() - yawDegrees));
        if (Math.abs(projection) < 0.00001) return 0.0f;

        // Fixed-frequency oscillation: changing strength never jumps the phase.
        // A little steady lean plus irregular flutter. No per-sign state or extra physics tick.
        double time = minecraft.level.getGameTime() + partialTick;
        double phase = (pos.asLong() * 0.00000019) % (Math.PI * 2.0);
        double flutter = 0.69 + 0.23 * Math.sin(time * 0.065 + phase)
                             + 0.08 * Math.sin(time * 0.13 + phase * 0.7);
        // Stronger visible wind sway, retaining the original shape and saturation curve.
        // Apply the 3x boost after clamping so even high winds remain exactly 3x stronger.
        return 3.0f * (float) Mth.clamp(projection * strength * 0.13 * flutter, -0.32, 0.32);
    }
}
