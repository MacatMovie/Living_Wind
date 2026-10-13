package com.macatmovie.livingwind.client.windmill;

import com.macatmovie.livingwind.block.entity.WindmillBlockEntity;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.util.Mth;

/** Client-only two-axis animation. Uses already-synchronized wind and the existing shelter cache. */
public final class WindmillMotion {
    private WindmillMotion() {}

    public static void update(WindmillBlockEntity mill, double tick) {
        double previous = mill.getLastRenderTick();
        mill.setLastRenderTick(tick);
        if (!ClientWindState.hasServerState()) return;

        float exposure = WindShelter.exposureForStaticAnimation(mill.getBlockPos());
        float strength = Mth.clamp(ClientWindState.altitudeStrength() * exposure, 0.0f, 2.0f);
        // The user-facing windmill should point its head along the wind travel direction,
        // not into the source. This flips the previous heading by exactly 180 degrees.
        float targetYaw = WindMath.normalizeDegrees(90.0f - ClientWindState.directionDegrees());
        long seed = mill.getBlockPos().asLong();
        double phase = ((seed ^ (seed >>> 17)) & 1023L) * (Math.PI * 2.0 / 1024);
        if (strength > 0.025f) {
            float sway = (0.4f + 1.8f * strength / 2f) * (float)(
                .75 * Math.sin(tick * .028 + phase) + .25 * Math.sin(tick * .011 + phase * 1.5));
            targetYaw = WindMath.normalizeDegrees(targetYaw + sway);
        }
        if (!mill.isInitialized() || !Double.isFinite(previous) || tick - previous > 40) {
            mill.setInitialized(true);
            mill.setYaw(targetYaw);
            mill.setYawVelocity(0);
            return;
        }
        float dt = (float)Math.max(0.0, Math.min(2.0, tick - previous));
        if (dt <= 0.0f) return;

        // Damped spring for the directional head; no snap, with gentle inertial overshoot.
        if (strength > 0.025f) {
            float omega = 0.095f + 0.047f * strength / 2.0f;
            float delta = WindMath.shortestDeltaDegrees(mill.getYaw(), targetYaw);
            float velocity = mill.getYawVelocity();
            velocity = Mth.clamp(velocity + (omega * omega * delta - 1.8f * omega * velocity) * dt, -6f, 6f);
            mill.setYawVelocity(velocity);
            mill.setYaw(WindMath.normalizeDegrees(mill.getYaw() + velocity * dt));
        } else {
            mill.setYawVelocity(0);
        }

        // Rotor target RPM grows continuously with wind strength, and coasts to a stop in calm.
        // Conversion: 1 RPM = 0.3 degrees per Minecraft tick (20 ticks/sec).
        float rpm = strength <= 0.012f ? 0f : 2f + 44f * strength; // up to 90 RPM
        float wanted = rpm * 0.3f;
        float alpha = 1.0f - (float)Math.exp(-dt / (wanted > mill.getRotorSpeed() ? 15.0f : 28.0f));
        float rate = Mth.lerp(alpha, mill.getRotorSpeed(), wanted);
        mill.setRotorSpeed(rate);
        mill.setRotorAngle(WindMath.normalizeDegrees(mill.getRotorAngle() + rate * dt));
    }
}
