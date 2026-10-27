package com.macatmovie.livingwind.compat;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import java.lang.reflect.Field;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Realm RPG: Treasure Balloons v1.0.0 integration.
 *
 * The upstream UpdateProcedure sets cardinal velocity on every baseTick and
 * may replace the Y component when flying over obstacles. We run at its TAIL,
 * after all vanilla/upstream velocity writes, and REPLACE the default cardinal
 * flight with Living Wind velocity. No wind means no inflated-balloon movement.
 * Popped balloons keep their original fall, health, loot and despawning.
 * There is no linkage to any Treasure Balloons class unless it is installed.
 */
public final class TreasureBalloonsWindBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LivingWind/TreasureBalloons");
    private static final String BALLOON_CLASS = "net.mcreator.realmrpgballoons.entity.TreasureBalloonEntity";
    private static final double WIND_SPEED_PER_STRENGTH = 0.08;
    private static final double MAX_WIND_SPEED = 0.16;
    private static volatile EntityDataAccessor<?> balloonPopped;
    private static volatile boolean invalidAccessor;

    private TreasureBalloonsWindBridge() {}

    public static void afterMovement(Entity entity) {
        if (entity == null || !BALLOON_CLASS.equals(entity.getClass().getName())
                || entity.isRemoved() || !hasBalloon(entity)) return;

        final float strength;
        final float direction;
        final double multiplier;
        if (entity.level() instanceof ServerLevel server) {
            if (!WindConfig.TREASURE_BALLOONS_WIND_ENABLED.get() || WindDimensionBlacklist.contains(server)) return;
            multiplier = WindConfig.TREASURE_BALLOONS_WIND_MULTIPLIER.get();
            if (multiplier <= 0) return;
            WindSnapshot wind = WindApi.getAt(server, entity.blockPosition());
            strength = wind.strength();
            direction = wind.directionDegrees();
        } else {
            // Client prediction uses the same server-authoritative toggle/multiplier
            // and already-synchronized wind; no local config mismatch on servers.
            if (!ClientWindState.treasureBalloonsWindEnabled()) return;
            multiplier = ClientWindState.treasureBalloonsWindMultiplier();
            if (multiplier <= 0) return;
            strength = ClientWindState.strengthAtAltitude(entity.blockPosition().getY(),
                entity.level().dimension() == net.minecraft.world.level.Level.OVERWORLD);
            direction = ClientWindState.directionDegrees();
        }
        if (!Float.isFinite(strength) || !Float.isFinite(direction)) return;
        double push = Mth.clamp(strength * WIND_SPEED_PER_STRENGTH * multiplier, 0.0, MAX_WIND_SPEED);
        // The original procedure writes a 0.1-block/tick cardinal motion on
        // every tick. Do not add to it: wind is the ONLY source of movement.
        if (push < 0.0001) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hasImpulse = true;
            return;
        }
        double x = WindMath.windX((float) push, direction);
        double z = WindMath.windZ((float) push, direction);
        // Preserve climb-over-obstacle behavior, but react only to an obstacle
        // actually lying in the NEW downwind path, not the old cardinal path.
        double y = !entity.level().noCollision(entity, entity.getBoundingBox().move(x, 0, z)) ? 0.05 : 0.0;
        entity.setDeltaMovement(new Vec3(x, y, z));
        entity.hasImpulse = true;
    }

    /** 0=inflated; nonzero=popped. Resolve public accessor once, not per tick. */
    @SuppressWarnings("unchecked")
    private static boolean hasBalloon(Entity entity) {
        if (invalidAccessor) return false;
        EntityDataAccessor<?> accessor = balloonPopped;
        if (accessor == null) {
            synchronized (TreasureBalloonsWindBridge.class) {
                accessor = balloonPopped;
                if (accessor == null) {
                    try {
                        Field field = entity.getClass().getField("DATA_BalloonPopped");
                        accessor = (EntityDataAccessor<?>) field.get(null);
                        balloonPopped = accessor;
                    } catch (ReflectiveOperationException | ClassCastException error) {
                        invalidAccessor = true;
                        LOG.warn("Treasure Balloons pop-state accessor not found; leaving its flight untouched.", error);
                        return false;
                    }
                }
            }
        }
        // Read the same synchronized entity data as Treasure Balloons itself.
        try {
            Object value = entity.getEntityData().get((EntityDataAccessor<Integer>) accessor);
            return value instanceof Integer count && count == 0;
        } catch (RuntimeException error) {
            invalidAccessor = true;
            LOG.warn("Unable to read Treasure Balloons pop-state; leaving its flight untouched.", error);
            return false;
        }
    }
}
