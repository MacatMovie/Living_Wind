package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindSnapshot;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Optional Superb Warfare 0.8.9.2 aircraft integration (Forge 1.20.1 and
 * NeoForge 1.21.1). Called at the end of VehicleEntity.travel(), after the
 * aircraft engine has applied its own lift, throttle, and drag, but before
 * VehicleEntity.baseTick() performs its normal gravity/movement pass.
 *
 * This applies a small horizontal wind acceleration rather than modifying
 * engine throttle, rotation, lift, or the final entity position. The original
 * vehicle movement and collision code remains authoritative.
 *
 * No Superb Warfare dependency or imported class is required. Kotlin's public
 * getVehicleType() accessor identifies AIRPLANE vehicles; ground vehicles,
 * helicopters and airships are left untouched by this aircraft-only setting.
 */
public final class SuperbWarfareAircraftWindBridge {
    private static final String VEHICLE_CLASS =
        "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity";
    private static final double ACCELERATION_PER_STRENGTH = 0.0045D;
    private static final double MAX_ACCELERATION_PER_TICK = 0.025D;
    private static volatile Method vehicleTypeGetter;
    private static volatile boolean missingVehicleTypeGetter;

    private SuperbWarfareAircraftWindBridge() {}

    /** Cache once per vehicle in the mixin; reflection only runs at classification. */
    public static boolean isAirplane(Entity vehicle) {
        if (vehicle == null || missingVehicleTypeGetter) return false;
        try {
            Method getter = vehicleTypeGetter;
            if (getter == null) {
                synchronized (SuperbWarfareAircraftWindBridge.class) {
                    getter = vehicleTypeGetter;
                    if (getter == null) {
                        getter = Class.forName(VEHICLE_CLASS, false, vehicle.getClass().getClassLoader())
                            .getMethod("getVehicleType");
                        vehicleTypeGetter = getter;
                    }
                }
            }
            Object type = getter.invoke(vehicle);
            return type instanceof Enum<?> enumType && enumType.name().equals("AIRPLANE");
        } catch (ReflectiveOperationException | LinkageError error) {
            // Don't interfere with aircraft physics if another SBW release
            // changes the method. Avoid retrying reflection every tick.
            missingVehicleTypeGetter = true;
            return false;
        }
    }

    /** Add wind as modest horizontal acceleration to the existing velocity. */
    public static void afterTravel(Entity vehicle, boolean enabled, double multiplier) {
        if (!enabled || multiplier <= 0.0D || vehicle == null || vehicle.isRemoved()
                || vehicle.onGround() || vehicle.isInWater() || vehicle.isInLava()
                || !(vehicle.level() instanceof ServerLevel server)
                || WindDimensionBlacklist.contains(server)) return;

        WindSnapshot wind = WindApi.getAt(server, vehicle.blockPosition());
        Vec3 drift = acceleration(wind.windX(), wind.windZ(), multiplier);
        if (drift == Vec3.ZERO) return;
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(drift));
        vehicle.needsSync = true;
    }

    /** Package-visible pure wind conversion, suitable for unit checks. */
    static Vec3 acceleration(double windX, double windZ, double multiplier) {
        if (!Double.isFinite(windX) || !Double.isFinite(windZ)
                || !Double.isFinite(multiplier) || multiplier <= 0.0D) return Vec3.ZERO;
        double x = windX * ACCELERATION_PER_STRENGTH * multiplier;
        double z = windZ * ACCELERATION_PER_STRENGTH * multiplier;
        double magnitude = Math.hypot(x, z);
        if (!Double.isFinite(magnitude) || magnitude < 0.0000001D) return Vec3.ZERO;
        if (magnitude > MAX_ACCELERATION_PER_TICK) {
            double factor = MAX_ACCELERATION_PER_TICK / magnitude;
            x *= factor;
            z *= factor;
        }
        return new Vec3(x, 0.0D, z);
    }
}
