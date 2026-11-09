package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Optional gameplay wind. Movement remains under vanilla collision/gravity/drag;
 * this adds a bounded horizontal impulse and never teleports entities.
 * Players and player-steered boats are predicted locally; unoccupied boats,
 * dropped items, mobs and projectiles move on the server.
 */
public final class EntityWindMotion {
    private EntityWindMotion() {}

    public static void tickServer(Entity entity) {
        // A cheap master gate keeps this mixin effectively inert by default.
        if (!WindConfig.ENTITY_WIND_ENABLED.get() || !(entity.level() instanceof ServerLevel level)
                || !entity.isAlive() || entity.isPassenger() || entity.isInLava()) return;

        double groundedMultiplier;
        double airborneMultiplier;
        double baseImpulse;
        double maxDownwind;
        double groundFactor = 0.12;
        if (entity instanceof Player) return; // Client-predicted, avoids rubberbanding.
        if (entity instanceof Boat boat) {
            if (!WindConfig.ENTITY_WIND_BOATS.get() || boat.getControllingPassenger() instanceof Player) return;
            // Water is normal for boats; only skip fully submerged boats or bubble columns.
            if (boat.level().getBlockState(boat.blockPosition()).is(Blocks.BUBBLE_COLUMN) || fullySubmerged(level, boat)) return;
            groundedMultiplier = airborneMultiplier = WindConfig.ENTITY_WIND_BOAT_MULTIPLIER.get();
            baseImpulse = 0.007;
            maxDownwind = 0.23;
            groundFactor = 1.0; // Boat drift on water must not use mob ground attenuation.
        } else if (entity instanceof ItemEntity item) {
            if (!WindConfig.ENTITY_WIND_DROPPED_ITEMS.get() || fullySubmerged(level, item)) return;
            groundedMultiplier = airborneMultiplier = WindConfig.ENTITY_WIND_DROPPED_ITEM_MULTIPLIER.get();
            baseImpulse = 0.010;
            maxDownwind = 0.24;
            groundFactor = 0.45; // Grounded items move less, airborne/floating items drift freely.
        } else if (entity instanceof Mob) {
            if (!WindConfig.ENTITY_WIND_MOBS.get() || entity.isInWaterOrBubble()) return;
            groundedMultiplier = WindConfig.ENTITY_WIND_MOB_GROUNDED_MULTIPLIER.get();
            airborneMultiplier = WindConfig.ENTITY_WIND_MOB_MULTIPLIER.get();
            baseImpulse = 0.010;
            maxDownwind = 0.24;
        } else if (entity instanceof Projectile) {
            if (!WindConfig.ENTITY_WIND_PROJECTILES.get() || entity.isInWaterOrBubble()
                    || WindConfig.ENTITY_WIND_PROJECTILE_MULTIPLIER.get() <= 0.0
                    || ProjectileWindBlacklist.excludes(entity)) return;
            groundedMultiplier = airborneMultiplier = WindConfig.ENTITY_WIND_PROJECTILE_MULTIPLIER.get();
            baseImpulse = 0.008;
            maxDownwind = 0.42;
        } else return;
        if (groundedMultiplier <= 0.0 && airborneMultiplier <= 0.0) return;

        // Optional server-side shelter gate; when disabled, skip this per-entity sky lookup.
        BlockPos exposurePos = entity.blockPosition().above();
        if (WindConfig.ENTITY_WIND_CHECK_SHELTER.get() && !level.canSeeSky(exposurePos)) return;
        // Only evaluate weather when entity wind is enabled and this type is eligible.
        double weatherMultiplier = EntityWindWeather.serverMultiplier(level);
        if (weatherMultiplier <= 0.0) return;
        WindSnapshot wind = WindApi.getAt(level, exposurePos);
        apply(entity, wind.windX(), wind.windZ(), groundedMultiplier * weatherMultiplier,
            airborneMultiplier * weatherMultiplier, baseImpulse, maxDownwind, groundFactor);
    }

    private static boolean fullySubmerged(ServerLevel level, Entity entity) {
        // Entity.blockPosition() can be inside the top water block on the surface.
        // Checking the next block permits surface drift but not submerged item/boat motion.
        return level.getFluidState(entity.blockPosition().above()).is(FluidTags.WATER);
    }

    /** Called from the local player's client tick for rider-controlled boats. */
    public static void tickClientBoat(Boat boat) {
        if (!ClientWindState.entityWindBoats() || ClientWindState.entityWindBoatMultiplier() <= 0.0f
                || !boat.isAlive() || boat.isInLava() || boat.level().getBlockState(boat.blockPosition()).is(Blocks.BUBBLE_COLUMN)
                || boat.level().getFluidState(boat.blockPosition().above()).is(FluidTags.WATER)
                || (ClientWindState.entityWindCheckShelter()
                    && !boat.level().canSeeSky(boat.blockPosition().above()))) return;
        double multiplier = ClientWindState.entityWindBoatMultiplier()
            * ClientWindState.entityWindWeatherMultiplier(boat.level());
        if (multiplier <= 0.0) return;
        // When entity shelter checks are off, use unattenuated wind rather than
        // inheriting the rider's separate audio/particle shelter measurement.
        boolean checkShelter = ClientWindState.entityWindCheckShelter();
        apply(boat, checkShelter ? ClientWindState.localWindX() : ClientWindState.altitudeWindX(),
            checkShelter ? ClientWindState.localWindZ() : ClientWindState.altitudeWindZ(),
            multiplier, multiplier, 0.007, 0.23, 1.0);
    }

    /** Called once at the end of the local player's client tick, after shelter updates. */
    public static void tickClientPlayer(Player player) {
        if (!ClientWindState.entityWindPlayers() || player.isCreative() || player.isSpectator()
                || !player.isAlive() || player.isPassenger() || player.isInWaterOrBubble() || player.isInLava()) return;
        double groundedMultiplier = ClientWindState.entityWindPlayerGroundedMultiplier();
        double airborneMultiplier = ClientWindState.entityWindPlayerMultiplier();
        if (groundedMultiplier <= 0.0 && airborneMultiplier <= 0.0) return;
        double weatherMultiplier = ClientWindState.entityWindWeatherMultiplier(player.level());
        if (weatherMultiplier <= 0.0) return;
        boolean checkShelter = ClientWindState.entityWindCheckShelter();
        if (checkShelter && !player.level().canSeeSky(player.blockPosition().above())) return;
        // Preserve existing sheltered wind when enabled; skip it for entity movement only when disabled.
        apply(player, checkShelter ? ClientWindState.localWindX() : ClientWindState.altitudeWindX(),
                checkShelter ? ClientWindState.localWindZ() : ClientWindState.altitudeWindZ(),
                groundedMultiplier * weatherMultiplier, airborneMultiplier * weatherMultiplier,
                0.012, 0.24, 0.12);
    }

    private static void apply(Entity entity, double windX, double windZ,
                              double groundedMultiplier, double airborneMultiplier, double baseImpulse, double maxDownwind, double groundFactor) {
        double strength = Math.hypot(windX, windZ);
        if (strength <= 0.015) return;
        double unitX = windX / strength;
        double unitZ = windZ / strength;
        Vec3 motion = entity.getDeltaMovement();
        double alongWind = motion.x * unitX + motion.z * unitZ;
        boolean grounded = entity.onGround();
        // Retain the original grounded attenuation and scale it independently.
        // Default grounded multiplier 2.5 gives precisely 2.5x the previous ground force;
        // default airborne multiplier 1.0 preserves the previous jumping/falling behavior.
        if (entity instanceof Projectile && grounded) return;
        double multiplier = grounded ? groundedMultiplier : airborneMultiplier;
        if (multiplier <= 0.0) return;
        double airFactor = grounded ? groundFactor : 1.0;
        double desired = Math.min(maxDownwind * 2.0, maxDownwind * strength * multiplier) * airFactor;
        double impulse = baseImpulse * strength * multiplier * airFactor;
        // Soft terminal drift: never stack unlimited downwind speed onto projectiles or players.
        impulse = Math.min(impulse, Math.max(0.0, desired - alongWind));
        if (impulse <= 0.00001) return;
        entity.setDeltaMovement(motion.add(unitX * impulse, 0.0, unitZ * impulse));
        entity.hasImpulse = true;
    }
}
