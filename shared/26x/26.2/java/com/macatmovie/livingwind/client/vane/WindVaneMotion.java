package com.macatmovie.livingwind.client.vane;

import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import com.macatmovie.livingwind.compat.InstrumentSpatialContext;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.util.Mth;

/** Inertial client-only vane animation, using existing wind packets and the cached shelter grid.
 * A slow continuous breeze oscillation prevents the stop-and-go look of infrequent direction updates.
 * No server tick, scan, or packet is added for the decorative vanes. */
public final class WindVaneMotion {
    private WindVaneMotion() {}

    public static float angle(WindVaneBlockEntity vane, double frameTick) {
        double previous = vane.getLastRenderTick();
        vane.setLastRenderTick(frameTick);
        if (!ClientWindState.hasServerState()) return vane.getRotationDegrees();

        var frame = InstrumentSpatialContext.at(vane.getLevel(), vane.getBlockPos(), (float) (frameTick % 1.0));
        // A swiveling vane retains its WORLD heading as the parent ship rotates.
        // Compensate that frame change immediately; the damped spring still governs
        // actual wind changes and its gentle natural sway.
        if (frame.moving()) {
            float reference = frame.localBearing(0.0f);
            if (vane.hasSubLevelReference()) {
                float turn = WindMath.shortestDeltaDegrees(vane.getLastSubLevelReference(), reference);
                vane.setRotationDegrees(WindMath.normalizeDegrees(vane.getRotationDegrees() + turn));
            }
            vane.recordSubLevelReference(reference);
        } else {
            vane.clearSubLevelReference();
        }

        float exposure = frame.moving() ? WindShelter.exposureAt(frame.worldPosition())
                                        : WindShelter.exposureForStaticAnimation(frame.worldPosition());
        float strength = Math.max(0.0f, (frame.moving() ? ClientWindState.strengthAtAltitude(frame.worldPosition().getY(),
                vane.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD)
                : ClientWindState.altitudeStrength()) * exposure);
        if (strength <= 0.025f) {
            // A sheltered or completely calm vane holds its last heading and stops wobbling.
            vane.setRotationVelocity(0.0f);
            return vane.getRotationDegrees();
        }

        // Wind directions in Living Wind represent the direction the air flows TO.
        // Preserve the orientation confirmed in-game for the single-plane bird arrow.
        float base = WindMath.normalizeDegrees(frame.localBearing(ClientWindState.directionDegrees()) + 180.0f);
        float windScale = Mth.clamp(strength / 2.0f, 0.0f, 1.0f);

        // Small, deterministic swaying of a physical weather vane. The phase depends on its
        // position so many nearby vanes don't appear to rotate in perfect lockstep.
        long seed = vane.getBlockPos().asLong();
        double phase = ((seed ^ (seed >>> 19)) & 1023L) * (Math.PI * 2.0 / 1024.0);
        double t = frameTick;
        float amplitude = (0.8f + 4.5f * windScale) * Mth.clamp(strength / 0.22f, 0.0f, 1.0f);
        float sway = amplitude * (float) (0.68 * Math.sin(t * 0.038 + phase)
                                                + 0.32 * Math.sin(t * 0.017 + phase * 1.47));
        float target = WindMath.normalizeDegrees(base + sway);

        if (!vane.isInitialized() || !Double.isFinite(previous) || frameTick - previous > 40.0) {
            vane.setInitialized(true);
            vane.setRotationDegrees(target);
            vane.setRotationVelocity(0.0f);
            return target;
        }

        float dt = (float) Math.max(0.0, Math.min(2.0, frameTick - previous));
        if (dt <= 0.0f) return vane.getRotationDegrees();

        // A damped angular spring gives natural acceleration and deceleration, slightly overshooting
        // a changed wind heading instead of travelling at a constant speed and stopping dead.
        float omega = 0.105f + 0.052f * windScale;
        float dampingRatio = 0.90f;  // slightly underdamped, without persistent wobble
        float delta = WindMath.shortestDeltaDegrees(vane.getRotationDegrees(), target);
        float velocity = vane.getRotationVelocity();
        float acceleration = omega * omega * delta - 2.0f * dampingRatio * omega * velocity;
        velocity = Mth.clamp(velocity + acceleration * dt, -7.5f, 7.5f);
        float next = WindMath.normalizeDegrees(vane.getRotationDegrees() + velocity * dt);
        vane.setRotationVelocity(velocity);
        vane.setRotationDegrees(next);
        return next;
    }
}
