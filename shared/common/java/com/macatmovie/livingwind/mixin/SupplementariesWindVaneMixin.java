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
 * Client-only visual wind bridge for Supplementaries' wind vane (Forge 1.20.1
 * and NeoForge 1.21.1). Supplementaries renders the vane with getYaw(partialTicks),
 * so steer that rendered angle without modifying its blockstate, ticker,
 * weather-based redstone output, or the explosion-triggered special spin.
 *
 * Use the same damped-spring approach as Living Wind's own vane: unlike a
 * constant-rate approach, it has continuous acceleration/deceleration and
 * a tiny, smoothly varying breeze sway between server direction updates.
 */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.WindVaneBlockTile", remap = false)
public abstract class SupplementariesWindVaneMixin {
    @Unique private boolean livingwind$suppVaneInitialized;
    @Unique private float livingwind$suppVaneYaw;
    @Unique private float livingwind$suppVaneVelocity;
    @Unique private double livingwind$suppVaneLastFrame = Double.NaN;
    @Unique private long livingwind$suppVaneSpecialSpinUntil = Long.MIN_VALUE;

    // The 1.21.1 vane can spin when wind-charged by an explosion. Suppress our
    // visual override until that animation ends. 1.20.1 has no such method.
    @Inject(method = "setWindCharged()V", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$suppVaneCharged(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide()) {
            livingwind$suppVaneSpecialSpinUntil = level.getGameTime() + 40L;
            livingwind$suppVaneInitialized = false;
            livingwind$suppVaneVelocity = 0.0f;
        }
    }

    @Inject(method = "getYaw(F)F", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void livingwind$steerSupplementariesVane(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!ClientWindConfig.SUPPLEMENTARIES_WIND_VANE_COMPAT.get() || !ClientWindState.hasServerState()) {
            livingwind$suppVaneInitialized = false;
            livingwind$suppVaneVelocity = 0.0f;
            livingwind$suppVaneLastFrame = Double.NaN;
            return; // Let Supplementaries render its normal animation.
        }

        BlockEntity tile = (BlockEntity) (Object) this;
        Level level = tile.getLevel();
        if (level == null || !level.isClientSide()) return;
        if (level.getGameTime() < livingwind$suppVaneSpecialSpinUntil) return;

        double frame = level.getGameTime() + Mth.clamp(partialTicks, 0.0f, 1.0f);
        double elapsed = frame - livingwind$suppVaneLastFrame;
        if (!livingwind$suppVaneInitialized || !Double.isFinite(elapsed) || elapsed < -0.5 || elapsed > 40.0) {
            // On first render (or after an extended pause / special spin),
            // continue from the vane's real visual bearing instead of snapping.
            livingwind$suppVaneYaw = WindMath.normalizeDegrees(cir.getReturnValue());
            livingwind$suppVaneVelocity = 0.0f;
            livingwind$suppVaneLastFrame = frame;
            livingwind$suppVaneInitialized = true;
        }

        float dt = (float) Math.max(0.0, Math.min(2.0, frame - livingwind$suppVaneLastFrame));
        livingwind$suppVaneLastFrame = frame;
        if (dt > 0.0f) {
            float strength = Math.max(0.0f, ClientWindState.altitudeStrength()
                * WindShelter.exposureForStaticAnimation(tile.getBlockPos()));
            if (strength <= 0.025f) {
                // A sheltered vane keeps its last bearing, like Living Wind's vane.
                livingwind$suppVaneVelocity = 0.0f;
            } else {
                float windScale = Mth.clamp(strength / 2.0f, 0.0f, 1.0f);
                long seed = tile.getBlockPos().asLong();
                double phase = ((seed ^ (seed >>> 19)) & 1023L) * (Math.PI * 2.0 / 1024.0);

                // Small continuous movement, so stable server headings never
                // look like a set of discrete incremental rotations.
                float amplitude = (0.7f + 2.8f * windScale)
                    * Mth.clamp(strength / 0.22f, 0.0f, 1.0f);
                float sway = amplitude * (float) (0.68 * Math.sin(frame * 0.038 + phase)
                    + 0.32 * Math.sin(frame * 0.017 + phase * 1.47));

                // Keep Supplementaries' already-tested facing convention; the
                // incoming yaw is the direction the air travels TO.
                float target = WindMath.normalizeDegrees(ClientWindState.directionDegrees() + sway);
                float omega = 0.105f + 0.052f * windScale;
                float dampingRatio = 0.90f;
                float delta = WindMath.shortestDeltaDegrees(livingwind$suppVaneYaw, target);
                float acceleration = omega * omega * delta
                    - 2.0f * dampingRatio * omega * livingwind$suppVaneVelocity;
                livingwind$suppVaneVelocity = Mth.clamp(livingwind$suppVaneVelocity + acceleration * dt,
                    -7.5f, 7.5f);
                livingwind$suppVaneYaw = WindMath.normalizeDegrees(
                    livingwind$suppVaneYaw + livingwind$suppVaneVelocity * dt);
            }
        }
        cir.setReturnValue(livingwind$suppVaneYaw);
    }
}
