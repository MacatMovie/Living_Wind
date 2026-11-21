package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Optional wind for Icy's Better Horses' deployed stabilizer. This uses only
 * vanilla classes at linkage time; Better Horses is never a hard dependency.
 * The horse, not its passenger, receives the horizontal drift.
 */
public final class IcyHorseGliderWindCompat {
    // Add only a small per-tick acceleration, letting Icy's Better Horses and
    // vanilla friction handle movement. Never subtract predicted drift or rewrite
    // the complete speed; that produced oscillation between the client and server.
    private static final double ACCELERATION_PER_WIND = 0.006;
    private static final double MAX_ACCELERATION = 0.018;
    private static boolean initialized;
    private static Class<?> horseDataClass;
    private static Method stabilizerState;

    private IcyHorseGliderWindCompat() {}

    /** OPEN and HALF_OPEN both produce the stabilizer's gliding descent. */
    public static boolean isGliding(AbstractHorse horse) {
        if (!initialized) initialize();
        if (horseDataClass == null || !horseDataClass.isInstance(horse)) return false;
        try {
            Object state = stabilizerState.invoke(horse);
            if (!(state instanceof Enum<?> active)) return false;
            return "OPEN".equals(active.name()) || "HALF_OPEN".equals(active.name());
        } catch (ReflectiveOperationException | LinkageError e) {
            // Failed calls must not compromise unrelated horses or future ticks.
            stabilizerState = null;
            horseDataClass = null;
            return false;
        }
    }

    private static void initialize() {
        initialized = true;
        try {
            Class<?> data = Class.forName("icy.betterhorses.net.IHorseData", false,
                    IcyHorseGliderWindCompat.class.getClassLoader());
            Method method = data.getMethod("bh_getStabilizerState");
            horseDataClass = data;
            stabilizerState = method;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Icy's Better Horses is not installed.
        }
    }

    /** Dedicated glider movement can operate even when generic Entity Wind is off. */
    public static boolean enabledOnServer() {
        return WindConfig.GLIDER_WIND_ENABLED.get() && WindConfig.GLIDER_WIND_INFLUENCE.get() > 0.0;
    }

    /** Avoid double forces from Living Wind's normal mob physics. */
    public static boolean handlesMobWind(AbstractHorse horse) {
        return enabledOnServer() && isGliding(horse);
    }

    /** Called after Better Horses updates the stabilizer.
     *
     * Player-controlled mounts are client-authoritative in vanilla Minecraft:
     * the rider simulates vehicle motion locally and sends position updates to
     * the server. Applying extra wind on the server as well causes the horse to
     * disagree with its driver's predicted position and produces visible snapping.
     *
     * Therefore the controlling rider's client alone applies wind to a ridden
     * horse. Unridden horses remain server-authoritative. Other clients only
     * receive the normal vehicle position synchronization.
     */
    public static void tick(AbstractHorse horse) {
        if (!horse.isAlive() || horse.onGround() || horse.isPassenger()
                || horse.isInWaterOrBubble() || horse.isInLava()
                || !isGliding(horse)) return;

        final boolean serverSide = horse.level() instanceof ServerLevel;
        final boolean riddenByPlayer = horse.getControllingPassenger() instanceof Player;
        if (!shouldSimulateHere(serverSide, riddenByPlayer,
                horse.isControlledByLocalInstance())) return;

        double windX, windZ, influence;
        if (horse.level() instanceof ServerLevel level) {
            if (!enabledOnServer() || !level.canSeeSky(horse.blockPosition().above())) return;
            var wind = WindApi.getAt(level, horse.blockPosition().above());
            windX = wind.windX();
            windZ = wind.windZ();
            influence = WindConfig.GLIDER_WIND_INFLUENCE.get();
        } else {
            // Wind is added only on the controlling rider's client. Neither the
            // server nor remote observers apply a competing horizontal force.
            if (!ClientWindState.gliderWindEnabled()
                    || !horse.level().canSeeSky(horse.blockPosition().above())) return;
            windX = ClientWindState.windXAt(horse.getX(), horse.getY(), horse.getZ());
            windZ = ClientWindState.windZAt(horse.getX(), horse.getY(), horse.getZ());
            influence = ClientWindState.gliderWindInfluence();
        }
        if (influence <= 0.0 || !Double.isFinite(windX) || !Double.isFinite(windZ)) return;
        double accelX = windX * influence * ACCELERATION_PER_WIND;
        double accelZ = windZ * influence * ACCELERATION_PER_WIND;
        double accelLength = Math.hypot(accelX, accelZ);
        if (accelLength < 0.00001) return;
        if (accelLength > MAX_ACCELERATION) {
            accelX *= MAX_ACCELERATION / accelLength;
            accelZ *= MAX_ACCELERATION / accelLength;
        }
        Vec3 movement = horse.getDeltaMovement();
        horse.setDeltaMovement(movement.x + accelX, movement.y, movement.z + accelZ);
        // Do NOT force hasImpulse/hurtMarked each tick. Icy's Better Horses
        // already handles its own rider synchronization and descent changes.
    }

    /** Pure ownership rule; kept independent of Minecraft internals for testing. */
    static boolean shouldSimulateHere(boolean serverSide, boolean riddenByPlayer,
                                      boolean locallyControlled) {
        return riddenByPlayer ? !serverSide && locallyControlled : serverSide;
    }
}
