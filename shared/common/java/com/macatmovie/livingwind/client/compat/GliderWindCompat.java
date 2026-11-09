package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Optional, client-predicted glider wind. The three supported mods keep ownership of
 * their glide mechanics; Living Wind only adds a capped, smoothly changing wind
 * velocity. The reflected state checks are resolved once and never load a missing
 * mod's classes in Living Wind's static initializer.
 *
 * This deliberately does not apply to Elytra or ordinary airborne players.
 */
public final class GliderWindCompat {
    private static final double PREVIOUS_AIR_FRICTION = 0.91;
    private static final double SMOOTHING = 0.18;
    private static boolean initialized;
    private static Method vcGliderActive;
    private static Method reliableWasGliding;
    private static Method paragliderMovementGet;
    private static Method paragliderMovementState;
    private static Method paragliderStateActive;
    private static Object paragliderFlag;
    private static boolean paragliderUsesFlag;

    // The previous tick's separate wind contribution is removed after approximate
    // vanilla air friction before the new one is applied. This prevents wind from
    // multiplying player velocity every tick or building an unbounded impulse.
    private static Player lastPlayer;
    private static double lastDriftX;
    private static double lastDriftZ;

    private GliderWindCompat() {}

    public static void reset() {
        lastPlayer = null;
        lastDriftX = lastDriftZ = 0.0;
    }

    /**
     * @return true if the dedicated glider wind handled this player (also at zero
     * wind); prevents adding normal Entity Wind to the same gliding player twice.
     */
    public static boolean tickClientPlayer(Player player) {
        if (player != lastPlayer) reset();
        if (!ClientWindState.gliderWindEnabled() || ClientWindState.gliderWindInfluence() <= 0.0f
                || !player.isAlive() || player.isSpectator() || player.getAbilities().flying
                || player.isPassenger() || player.onGround() || player.isFallFlying()
                || player.isInWaterOrBubble() || player.isInLava() || player.onClimbable()
                || !isUsingSupportedGlider(player)) {
            reset();
            return false;
        }

        lastPlayer = player;
        Vec3 velocity = player.getDeltaMovement();
        // Estimate the controller-provided movement without our previous drift.
        double baseX = velocity.x - lastDriftX * PREVIOUS_AIR_FRICTION;
        double baseZ = velocity.z - lastDriftZ * PREVIOUS_AIR_FRICTION;
        double baseSpeed = Math.hypot(baseX, baseZ);
        double windX = ClientWindState.localWindX();
        double windZ = ClientWindState.localWindZ();
        double windSpeed = Math.hypot(windX, windZ);
        double targetX = 0.0, targetZ = 0.0;

        if (windSpeed > 0.0001) {
            double windUnitX = windX / windSpeed;
            double windUnitZ = windZ / windSpeed;
            // 2.0 is the nominal full-strength reference. Extreme gusts remain capped.
            double intensity = Math.min(1.0, windSpeed / 2.0) * ClientWindState.gliderWindInfluence();
            // The expected cruising speed depends on the glider mod and its upgrades.
            // Use its current unmodified velocity, with a floor for hands-off drifting.
            double referenceSpeed = Math.min(0.22, Math.max(0.10, baseSpeed));
            double alignment = baseSpeed > 0.015
                    ? Math.max(-1.0, Math.min(1.0, (baseX * windUnitX + baseZ * windUnitZ) / baseSpeed))
                    : 0.0;
            // Full tailwind aims for about 2.5x normal cruising speed, and a full
            // headwind about 0.2x, without instantly reversing steering direction.
            // Crosswinds retain sideways drift. These are tuning targets, not hard
            // guarantees for the different movement systems of all three mods.
            double speedFactor = 1.15 + 0.35 * alignment;
            double driftSpeed = Math.min(0.40, referenceSpeed * speedFactor * intensity);
            if (alignment < -0.25 && baseSpeed > 0.015) {
                driftSpeed = Math.min(driftSpeed, baseSpeed * 0.8 / Math.max(0.001, -alignment));
            }
            targetX = windUnitX * driftSpeed;
            targetZ = windUnitZ * driftSpeed;
        }
        double nextX = lastDriftX + (targetX - lastDriftX) * SMOOTHING;
        double nextZ = lastDriftZ + (targetZ - lastDriftZ) * SMOOTHING;
        player.setDeltaMovement(baseX + nextX, velocity.y, baseZ + nextZ);
        if (Math.abs(nextX - lastDriftX * PREVIOUS_AIR_FRICTION) > 0.0001
                || Math.abs(nextZ - lastDriftZ * PREVIOUS_AIR_FRICTION) > 0.0001) {
            player.hasImpulse = true;
        }
        lastDriftX = nextX;
        lastDriftZ = nextZ;
        return true;
    }

    private static boolean isUsingSupportedGlider(Player player) {
        if (!initialized) initialize();
        if (vcGliderActive != null) {
            try {
                if (Boolean.TRUE.equals(vcGliderActive.invoke(null, player))) return true;
            } catch (ReflectiveOperationException | LinkageError e) { vcGliderActive = null; }
        }
        if (reliableWasGliding != null) {
            try {
                if (Boolean.TRUE.equals(reliableWasGliding.invoke(null, player))) return true;
            } catch (ReflectiveOperationException | LinkageError e) { reliableWasGliding = null; }
        }
        if (paragliderMovementGet != null) {
            try {
                Object movement = paragliderMovementGet.invoke(null, player);
                Object state = paragliderMovementState.invoke(movement);
                if (Boolean.TRUE.equals(paragliderUsesFlag
                        ? paragliderStateActive.invoke(state, paragliderFlag)
                        : paragliderStateActive.invoke(state))) return true;
            } catch (ReflectiveOperationException | LinkageError e) { paragliderMovementGet = null; }
        }
        return false;
    }

    private static void initialize() {
        initialized = true;
        try {
            vcGliderActive = Class.forName("net.venturecraft.gliders.util.GliderUtil")
                    .getMethod("isGlidingWithActiveGlider", LivingEntity.class);
        } catch (ReflectiveOperationException | LinkageError ignored) { /* Gliders not installed */ }
        try {
            reliableWasGliding = Class.forName("com.evandev.reliable_gliders.api.GlidingState")
                    .getMethod("wasGliding", Player.class);
        } catch (ReflectiveOperationException | LinkageError ignored) { /* Reliable Gliders not installed */ }
        try {
            Class<?> movement = Class.forName("tictim.paraglider.api.movement.Movement");
            Class<?> state = Class.forName("tictim.paraglider.api.movement.PlayerState");
            Method movementGet = movement.getMethod("get", Player.class);
            Method movementState = movement.getMethod("state");
            Method stateActive;
            Object flag = null;
            boolean useFlag = false;
            try {
                // NeoForge 1.21.1's convenience flag method.
                stateActive = state.getMethod("paragliding");
            } catch (NoSuchMethodException newerNotAvailable) {
                // Forge 1.20.1 uses has(ResourceLocation) and a static flag.
                Class<?> flags = Class.forName("tictim.paraglider.api.movement.ParagliderPlayerStates$Flags");
                Field field = flags.getField("FLAG_PARAGLIDING");
                flag = field.get(null);
                stateActive = state.getMethod("has", flag.getClass());
                useFlag = true;
            }
            paragliderMovementGet = movementGet;
            paragliderMovementState = movementState;
            paragliderStateActive = stateActive;
            paragliderFlag = flag;
            paragliderUsesFlag = useFlag;
        } catch (ReflectiveOperationException | LinkageError ignored) { /* Paraglider not installed */ }
    }
}
