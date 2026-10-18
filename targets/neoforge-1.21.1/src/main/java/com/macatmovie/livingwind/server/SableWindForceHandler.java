package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindSnapshot;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Server-only Living Wind integration. Uses Sable's force group and
 * pre-physics event, NEVER directly moves an airship, changes Sable settings,
 * modifies Create: Deep Seas, or affects bodies without wind-catching blocks.
 *
 * This class is loaded only by SableWindIntegration when Sable is installed.
 */
public final class SableWindForceHandler implements SubLevelObserver {
    private static final Logger LOG = LogUtils.getLogger();
    private static final TagKey<Block> ENVELOPES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("aeronautics", "envelope"));
    private static final TagKey<Block> SAILS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("create", "windmill_sails"));

    // Wind pressure was calibrated on Sable contraptions. Map Living Wind's
    // 0..2 strength into the 0..100 reference range before squaring the wind.
    // The 0.30x playtested force is the 1.0x config baseline.
    private static final double BASE_PRESSURE = 0.000003;
    private static final double SABLE_WIND_SCALE = 50.0;
    private static final double MAX_FORCE = 5000.0;
    private static final long MAX_PROFILE_SCAN_VOLUME = 300_000L;
    private static final ForceGroup WIND_GROUP = new ForceGroup(
            Component.translatable("livingwind.sable.force.name"),
            Component.translatable("livingwind.sable.force.description"), 0x74BFE0, true);

    // Optional Sable block-change bridge (installed by the Sable-only mixin). Only
    // one handler is registered per game process, and no scans occur inside the hook.
    private static volatile SableWindForceHandler activeInstance;

    private final Map<SubLevelPhysicsSystem, ServerLevel> levelsByPhysics = new ConcurrentHashMap<>();
    private final Map<UUID, SailProfile> profiles = new ConcurrentHashMap<>();
    // Sable's physics event can fire several times per Minecraft tick. Capture
    // the server-authoritative wind snapshot once per game tick instead of
    // creating one new WindSnapshot and accessing SavedData on every substep.
    private final Map<ServerLevel, WindSnapshot> windByLevel = new ConcurrentHashMap<>();
    // High Seas / Deep Seas already simulate sail thrust. Never add Living Wind's
    // own sail-only push on top of that system; balloon-based airships still work.
    private final boolean marineSailPhysicsInstalled =
            net.neoforged.fml.ModList.get().isLoaded("create_submarine")
            || net.neoforged.fml.ModList.get().isLoaded("deepseas")
            || net.neoforged.fml.ModList.get().isLoaded("highseas")
            || net.neoforged.fml.ModList.get().isLoaded("create_high_seas");
    private boolean disabled;
    private int failures;

    private SableWindForceHandler() {}

    public static void start(IEventBus modBus) {
        SableWindForceHandler instance = new SableWindForceHandler();
        // ForceGroups must be registered or Aeronautics' force diagram can fail to encode them.
        modBus.addListener(RegisterEvent.class, instance::registerForceGroup);
        // Manual event registration is essential: loading event classes via automatic
        // subscribers would hard-depend on Sable even when the mod is absent.
        NeoForge.EVENT_BUS.addListener(ForgeSableSubLevelContainerReadyEvent.class, instance::onReady);
        NeoForge.EVENT_BUS.addListener(ForgeSablePrePhysicsTickEvent.class, instance::onPrePhysicsTick);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, instance::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, instance::onLevelTick);
        activeInstance = instance;
    }

    /**
     * Sable reports real block-state edits through SableCommonEvents.handleBlockChange.
     * The optional mixin calls this only after Sable has updated the plot bounds
     * and physics data. Invalidate the affected ship, then let the next physics
     * update rebuild its profile at most once, no matter how many blocks were
     * placed/removed during construction. Never scan from a block-edit callback.
     *
     * Only edits affecting the cached profile matter: adding/removing a block
     * changes total occupancy and potentially bounds; replacing blocks can change
     * whether they are wind catchers. Redstone state changes and other ordinary
     * block property updates do not cause rescanning every physics tick.
     */
    public static void onSableBlockChanged(ServerLevel level, LevelChunk chunk,
            BlockState oldState, BlockState newState) {
        SableWindForceHandler handler = activeInstance;
        if (handler == null || handler.disabled || handler.profiles.isEmpty() || oldState == newState) return;

        if (oldState.isAir() == newState.isAir()
                && oldState.is(ENVELOPES) == newState.is(ENVELOPES)
                && oldState.is(SAILS) == newState.is(SAILS)) return;

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        var plot = container.getPlot(chunk.getPos());
        if (plot == null) return;
        handler.profiles.remove(plot.getSubLevel().getUniqueId());
    }

    private void registerForceGroup(RegisterEvent event) {
        event.register(ForceGroups.REGISTRY_KEY, LivingWind.id("aeronautics_wind"), () -> WIND_GROUP);
    }

    private void onReady(ForgeSableSubLevelContainerReadyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SubLevelContainer container = event.getContainer();
        if (container instanceof ServerSubLevelContainer server) {
            levelsByPhysics.put(server.physicsSystem(), level);
            container.addObserver(this);
        }
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!WindConfig.AERONAUTICS_WIND_ENABLED.get()) {
            windByLevel.remove(level);
            return;
        }
        windByLevel.put(level, WindSavedData.get(level).snapshot(level.getGameTime()));
    }

    private void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        levelsByPhysics.values().removeIf(associated -> associated == level);
        windByLevel.remove(level);
        // Profiles are per-sub-level, removed individually by onSubLevelRemoved.
        // Clear remaining stale profiles when the last tracked level unloads.
        if (levelsByPhysics.isEmpty()) profiles.clear();
    }

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
        profiles.remove(subLevel.getUniqueId());
    }

    private void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        if (disabled || !WindConfig.AERONAUTICS_WIND_ENABLED.get()) return;
        double setting = WindConfig.AERONAUTICS_WIND_MULTIPLIER.get();
        if (setting <= 0.0) return;
        try {
            ServerLevel level = levelsByPhysics.get(event.getPhysicsSystem());
            if (level == null) return;
            WindSnapshot wind = windByLevel.get(level);
            if (wind == null) return;
            if (wind.strength() <= 0.001f) return;
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) return;
            for (ServerSubLevel ship : container.getAllSubLevels()) {
                apply(ship, level, wind, setting);
            }
        } catch (RuntimeException | LinkageError error) {
            if (++failures <= 3) LOG.warn("Living Wind: Sable force update failed (attempt {}).", failures, error);
            if (failures >= 3) {
                disabled = true;
                LOG.error("Living Wind: disabled Sable force integration after repeated failures.");
            }
        }
    }

    private void apply(ServerSubLevel ship, ServerLevel level, WindSnapshot wind, double setting) {
        SailProfile profile = profiles.computeIfAbsent(ship.getUniqueId(), ignored -> inspect(ship));
        if (profile == SailProfile.EMPTY || (marineSailPhysicsInstalled && !profile.hasEnvelope())) return;

        MassData mass = ship.getMassTracker();
        if (mass == null || mass.getMass() <= 0.0 || mass.getCenterOfMass() == null) return;

        Pose3dc pose = ship.logicalPose();
        Vector3d localPosition = profile.center();
        net.minecraft.world.phys.Vec3 worldPosition = pose.transformPosition(
                new net.minecraft.world.phys.Vec3(localPosition.x, localPosition.y, localPosition.z));
        // Sample Living Wind's synchronized server state; no second wind simulation.
        // Respect the existing optional altitude multiplier at actual craft altitude.
        double strength = wind.strength() * altitudeMultiplier(level, worldPosition.y);
        if (strength <= 0.0) return;
        Vector3dc localDirection = normalizedLocalWind(pose, wind.directionDegrees());
        if (localDirection == null) return;

        // Project an inexpensive cached bounding box into the wind direction.
        double area = profile.areaFacing(localDirection);
        double referenceWind = strength * SABLE_WIND_SCALE;
        double force = SableWindMath.pressure(BASE_PRESSURE, setting, referenceWind,
                area, profile.coverage(), MAX_FORCE);
        if (!(force > 0.0) || !Double.isFinite(force)) return;

        ship.getOrCreateQueuedForceGroup(WIND_GROUP)
                .applyAndRecordPointForce(localPosition, new Vector3d(localDirection).mul(force));
    }

    private static Vector3dc normalizedLocalWind(Pose3dc pose, float directionDegrees) {
        // Living Wind's direction convention: 0 = +Z, 90 = -X.
        net.minecraft.world.phys.Vec3 worldDirection = new net.minecraft.world.phys.Vec3(
                SableWindMath.windX(directionDegrees), 0.0, SableWindMath.windZ(directionDegrees));
        net.minecraft.world.phys.Vec3 local = pose.transformNormalInverse(worldDirection);
        double length = local.length();
        if (!(length > 1.0e-6)) return null;
        return new Vector3d(local.x / length, local.y / length, local.z / length);
    }

    private static double altitudeMultiplier(ServerLevel level, double y) {
        if (!WindConfig.ALTITUDE_SCALING_ENABLED.get() || level.dimension() != net.minecraft.world.level.Level.OVERWORLD)
            return 1.0;
        double lowY = WindConfig.ALTITUDE_MIN_Y.get();
        double highY = WindConfig.ALTITUDE_MAX_Y.get();
        double fraction = highY <= lowY ? (y >= highY ? 1.0 : 0.0)
                : Math.max(0.0, Math.min(1.0, (y - lowY) / (highY - lowY)));
        return WindConfig.ALTITUDE_MIN_MULTIPLIER.get() * (1.0 - fraction)
                + WindConfig.ALTITUDE_MAX_MULTIPLIER.get() * fraction;
    }

    /** Scan once per stable profile; block changes invalidate the affected body's cache. */
    private static SailProfile inspect(ServerSubLevel ship) {
        BoundingBox3ic bounds = ship.getPlot().getBoundingBox();
        long sx = (long) bounds.maxX() - bounds.minX() + 1;
        long sy = (long) bounds.maxY() - bounds.minY() + 1;
        long sz = (long) bounds.maxZ() - bounds.minZ() + 1;
        if (sx <= 0 || sy <= 0 || sz <= 0 || sx * sy * sz > MAX_PROFILE_SCAN_VOLUME) {
            return SailProfile.EMPTY;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int total = 0;
        int catchers = 0;
        int envelopeCount = 0;
        double xSum = 0.0, ySum = 0.0, zSum = 0.0;
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockState state = ship.getLevel().getBlockState(pos.set(x, y, z));
                    if (state.isAir()) continue;
                    total++;
                    boolean envelope = state.is(ENVELOPES);
                    if (envelope || state.is(SAILS)) {
                        if (envelope) envelopeCount++;
                        catchers++;
                        xSum += x + 0.5;
                        ySum += y + 0.5;
                        zSum += z + 0.5;
                    }
                }
            }
        }
        if (catchers == 0 || total == 0) return SailProfile.EMPTY;
        return new SailProfile((double) catchers / total, envelopeCount > 0,
                sx * 0.5, sy * 0.5, sz * 0.5,
                new Vector3d(xSum / catchers, ySum / catchers, zSum / catchers));
    }

    private record SailProfile(double coverage, boolean hasEnvelope, double hx, double hy, double hz, Vector3d center) {
        private static final SailProfile EMPTY = new SailProfile(0, false, 0, 0, 0, new Vector3d());

        double areaFacing(Vector3dc wind) {
            return SableWindMath.projectedArea(hx, hy, hz, wind.x(), wind.y(), wind.z());
        }
    }
}
