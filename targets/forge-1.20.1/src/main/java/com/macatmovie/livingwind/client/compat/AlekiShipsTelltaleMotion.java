package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Rendering-only smoothing for Nifty Ships' little mast telltale.
 * Ship wind/physics/HUD still update at Nifty Ships' original cadence.
 * Its original animation uses a discontinuous -2 * speed * age wave phase;
 * integrating phase instead keeps the fabric waving across speed changes.
 */
public final class AlekiShipsTelltaleMotion {
    // Only used on render thread; weak keys do not keep unloaded ships alive.
    private static final Map<Entity, Motion> MOTIONS = new WeakHashMap<>();
    private static final float DIRECTION_RESPONSE_TICKS = 5.0f;
    private static final float SPEED_RESPONSE_TICKS = 6.0f;
    private static final float TWO_PI = (float) (2.0 * Math.PI);
    private static Method speedGetter;
    private static boolean speedGetterUnavailable;

    private AlekiShipsTelltaleMotion() {}

    /** Invoked after the original telltale pose has been computed. */
    public static void smooth(Object sourceBoat, float partialTicks, ModelPart flag, ModelPart[] sections) {
        if (!(sourceBoat instanceof Entity boat)) return;
        if (!ClientWindState.alekiShipsWindEnabled()) {
            MOTIONS.remove(boat);
            return;
        }

        float time = boat.tickCount + Mth.clamp(partialTicks, 0.0f, 1.0f);
        // Native flag angle = world wind yaw - boat yaw - 180 degrees.
        float targetWorldYaw = Mth.wrapDegrees((float) Math.toDegrees(flag.yRot) + boat.getYRot() + 180.0f);
        float targetSpeed = windSpeed(boat);
        if (!Float.isFinite(targetWorldYaw) || !Float.isFinite(targetSpeed)) return;

        Motion motion = MOTIONS.get(boat);
        if (motion == null) {
            motion = new Motion(targetWorldYaw, targetSpeed, -2.0f * targetSpeed * time, time);
            MOTIONS.put(boat, motion);
        } else {
            float dt = time - motion.lastTime;
            if (dt < 0.0f || dt > 80.0f) {
                // Long culling or rewind: don't animate from stale world state.
                motion = new Motion(targetWorldYaw, targetSpeed, -2.0f * targetSpeed * time, time);
                MOTIONS.put(boat, motion);
            } else if (dt > 0.0f) {
                float turnAlpha = 1.0f - (float) Math.exp(-dt / DIRECTION_RESPONSE_TICKS);
                float speedAlpha = 1.0f - (float) Math.exp(-dt / SPEED_RESPONSE_TICKS);
                float turn = Mth.wrapDegrees(targetWorldYaw - motion.worldYaw);
                motion.worldYaw = Mth.wrapDegrees(motion.worldYaw + turn * turnAlpha);
                motion.speed = Mth.lerp(speedAlpha, motion.speed, targetSpeed);
                motion.phase = (float) Math.IEEEremainder(motion.phase - 2.0f * motion.speed * dt, TWO_PI);
                motion.lastTime = time;
            }
        }

        // Smooth only the world wind: boat turns must still affect flag yaw immediately.
        flag.yRot = (float) Math.toRadians(Mth.wrapDegrees(motion.worldYaw - boat.getYRot() - 180.0f));
        for (int i = 0; i < Math.min(6, sections.length); i++) {
            ModelPart section = sections[i];
            if (section == null) break;
            float falloff = Math.min(1.0f, 0.4f * (float) Math.log(i + 1.0f) + 0.1f);
            section.x = 2.0f * (float) Math.sin(0.6f * i + motion.phase) * falloff;
        }
    }

    private static float windSpeed(Entity boat) {
        if (!speedGetterUnavailable) {
            try {
                Method method = speedGetter;
                if (method == null || !method.getDeclaringClass().isInstance(boat)) {
                    method = boat.getClass().getMethod("getLocalWindSpeed");
                    speedGetter = method;
                }
                return Mth.clamp(((Number) method.invoke(boat)).floatValue(), 0.0f, 0.2f);
            } catch (ReflectiveOperationException | ClassCastException | LinkageError ex) {
                // Safe visual fallback for a future release with a changed getter.
                speedGetterUnavailable = true;
            }
        }
        float strength = ClientWindState.strengthAt(boat.getX(), boat.getY(), boat.getZ());
        return Mth.clamp(strength * ClientWindState.alekiShipsWindScale(), 0.0f, 0.2f);
    }

    private static final class Motion {
        private float worldYaw;
        private float speed;
        private float phase;
        private float lastTime;

        private Motion(float worldYaw, float speed, float phase, float lastTime) {
            this.worldYaw = worldYaw;
            this.speed = speed;
            this.phase = (float) Math.IEEEremainder(phase, TWO_PI);
            this.lastTime = lastTime;
        }
    }
}
