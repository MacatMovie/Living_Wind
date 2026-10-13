package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional wind direction bridge for Supplementaries 1.20.1 and 1.21.1.
 *
 * Supplementaries already renders its vane using getYaw(partialTicks). Override
 * ONLY that visual yaw, never its block state, ticker, sound or weather-based
 * redstone output. The @Pseudo target is absent in installations without the mod.
 */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.WindVaneBlockTile", remap = false)
public abstract class SupplementariesWindVaneMixin {
    @Unique private boolean livingwind$suppVaneInitialized;
    @Unique private float livingwind$suppVaneYaw;
    @Unique private double livingwind$suppVaneLastFrame = Double.NaN;
    @Unique private long livingwind$suppVaneSpecialSpinUntil = Long.MIN_VALUE;

    // Supplementaries 1.21.1 briefly spins vanes when charged by an explosion.
    // Let its original special animation play, then smoothly regain wind control.
    // This method is absent in 1.20.1; require=0 makes the hook optional.
    @Inject(method = "setWindCharged()V", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$suppVaneCharged(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide()) {
            livingwind$suppVaneSpecialSpinUntil = level.getGameTime() + 40L;
            livingwind$suppVaneInitialized = false;
        }
    }

    @Inject(method = "getYaw(F)F", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void livingwind$steerSupplementariesVane(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!ClientWindConfig.SUPPLEMENTARIES_WIND_VANE_COMPAT.get() || !ClientWindState.hasServerState()) {
            livingwind$suppVaneInitialized = false;
            livingwind$suppVaneLastFrame = Double.NaN;
            return; // The original Supplementaries animation stays in control.
        }

        BlockEntity tile = (BlockEntity) (Object) this;
        Level level = tile.getLevel();
        if (level == null || !level.isClientSide()) return;
        if (level.getGameTime() < livingwind$suppVaneSpecialSpinUntil) return;

        double frame = level.getGameTime() + Mth.clamp(partialTicks, 0.0f, 1.0f);
        if (!livingwind$suppVaneInitialized) {
            // Start at the actual rendered orientation; no snap on first frame.
            livingwind$suppVaneYaw = cir.getReturnValue();
            livingwind$suppVaneLastFrame = frame;
            livingwind$suppVaneInitialized = true;
        }

        float strength = ClientWindState.altitudeStrength()
            * WindShelter.exposureForStaticAnimation(tile.getBlockPos());
        float dt = Double.isFinite(livingwind$suppVaneLastFrame)
            ? (float) Math.max(0.0, Math.min(2.0, frame - livingwind$suppVaneLastFrame)) : 0.0f;
        livingwind$suppVaneLastFrame = frame;

        if (strength > 0.025f) {
            // Follow wind travel direction. The previous +180 degrees caused the reversed vane.
            float target = WindMath.normalizeDegrees(ClientWindState.directionDegrees());
            float step = (0.8f + 4.0f * Mth.clamp(strength, 0.0f, 2.0f)) * dt;
            livingwind$suppVaneYaw = WindMath.approachDegrees(livingwind$suppVaneYaw, target, step);
        }
        // Keep last bearing when calm/sheltered. No competing vanilla oscillations.
        cir.setReturnValue(livingwind$suppVaneYaw);
    }
}
