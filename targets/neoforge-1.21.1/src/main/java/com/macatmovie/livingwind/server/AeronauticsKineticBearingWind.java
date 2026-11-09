package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.config.WindConfig;
import com.mojang.logging.LogUtils;
import com.macatmovie.livingwind.wind.WindSnapshot;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import org.slf4j.Logger;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional Transmission & Linkage kinetic conversion bearing wind drive.
 *
 * Called only from that bearing's own Sable physics callback. We add real
 * angular impulse; its native resistance, physics RPM sampling, and Create
 * stress conversion are deliberately left alone. No dependency on its Java
 * classes, and no moving blocks are scanned in the hot physics path.
 */
public final class AeronauticsKineticBearingWind {
    private static final TagKey<Block> SAILS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("create", "windmill_sails"));
    // Simulated's symmetric sails are also tagged as windmill sails, but use
    // AXIS instead of FACING. Their two sides catch wind equally. Check this
    // tag first so they never fall through the regular one-sided sail path.
    private static final TagKey<Block> SYMMETRIC_SAILS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("simulated", "symmetric_sails"));
    private static final Logger LOG = LogUtils.getLogger();
    // The optional Simulated bearing superclass isn't a compile dependency.
    // Resolve its public attached-rotor UUID getter once, rather than reflecting
    // for every Sable physics substep. The lookup is only needed if the
    // Transmission & Linkage integration is actually loaded.
    private static final MethodHandle ATTACHED_ROTOR_ID = findAttachedRotorIdGetter();
    private static final AtomicBoolean REPORTED_ROTOR_ACCESS_FAILURE = new AtomicBoolean();
    private static final int MAX_SCAN_SAILS = 2048;
    private static final Direction[] SAIL_FACES = Direction.values();
    private static final Direction[] SYMMETRIC_NORMALS = {Direction.EAST, Direction.UP, Direction.SOUTH};
    private static final int MAX_SCAN_CHUNKS = 64;
    private static final long REFRESH_TICKS = 200;
    // Keep the original torque ceiling at <=0.5 wind. Above that, allow
    // stronger winds to deliver more torque instead of clipping at the
    // same maximum. The angular acceleration limiter still protects light
    // rotors from large instantaneous speed changes.
    private static final double BASE_MAX_TORQUE = 48000.0;
    private static final double MAX_TORQUE = 480000.0;
    private static final double TORQUE_PER_SAIL_RADIUS = 90.0;
    private static final double DRAG_PER_SAIL_RADIUS_SQUARED = 7.5;
    // Sable velocities are in blocks/meters per second; wind snapshots are
    // unitless. This affects *only* the new apparent-wind contribution from a
    // moving bearing, and leaves stationary rotors exactly as calibrated.
    // 1.0 Living Wind strength corresponds to about 16 m/s of air movement.
    private static final double WIND_SPEED_METERS_PER_SECOND = 16.0;
    // Wind-drive safety ceiling (80 RPM); does not forcibly brake external motion.
    private static final double MAX_WIND_DRIVEN_OMEGA = Math.PI * 2.0 * 80.0 / 60.0;
    // Keep the already-tested wind response from 0.0 to 0.5. Above 0.5,
    // allow progressively greater drive acceleration so faster winds can
    // overcome the bearing's fixed resistance rather than hitting the same
    // 2 rad/s² ceiling at every strength. Still bounded for lightweight rotors.
    private static final double BASE_ANGULAR_ACCELERATION = 2.0;
    private static final double EXTRA_ACCELERATION_PER_STRENGTH = 2.5;
    private static final double MAX_ANGULAR_ACCELERATION = 6.0;

    // The native Sable physics callback can run more frequently than 20 Hz.
    // The normal server tick publishes immutable snapshots for it to consume.
    private static final Map<ServerLevel, WindSnapshot> WIND = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ACTIVE_ROTORS = new ConcurrentHashMap<>();

    private AeronauticsKineticBearingWind() {}

    private static MethodHandle findAttachedRotorIdGetter() {
        try {
            Class<?> base = Class.forName("dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity",
                    false, AeronauticsKineticBearingWind.class.getClassLoader());
            return MethodHandles.publicLookup().findVirtual(base, "getSubLevelID", MethodType.methodType(UUID.class));
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOG.warn("Living Wind: Transmission & Linkage rotor ID lookup is unavailable; skipping bearing wind drive safely.", exception);
            return null;
        }
    }

    private static boolean matchesAttachedRotor(BlockEntity bearing, ServerSubLevel rotor) {
        if (ATTACHED_ROTOR_ID == null) return false;
        try {
            return rotor.getUniqueId().equals((UUID) ATTACHED_ROTOR_ID.invoke(bearing));
        } catch (Throwable failure) {
            if (REPORTED_ROTOR_ACCESS_FAILURE.compareAndSet(false, true)) {
                LOG.warn("Living Wind: cannot identify attached kinetic bearing rotor; skipping wind drive.", failure);
            }
            return false;
        }
    }

    public static void start() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, AeronauticsKineticBearingWind::afterTick);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, AeronauticsKineticBearingWind::onUnload);
    }

    private static void afterTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel server)) return;
        if (WindConfig.AERONAUTICS_KINETIC_BEARING_ENABLED.get()) {
            WIND.put(server, WindApi.get(server));
        } else {
            WIND.remove(server);
        }
    }

    private static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel server) {
            WIND.remove(server);
            ACTIVE_ROTORS.clear();
        }
    }

    public static boolean isDrivenRotor(ServerSubLevel ship, long gameTick) {
        Long last = ACTIVE_ROTORS.get(ship.getUniqueId());
        return last != null && gameTick >= last && gameTick - last <= 4L;
    }

    public static void onRotorRemoved(SubLevel ship) {
        ACTIVE_ROTORS.remove(ship.getUniqueId());
    }

    public static final class RotorCache {
        private long lastScanTick = Long.MIN_VALUE;
        private long lastDiagnosticsTick = Long.MIN_VALUE;
        private ServerSubLevel cachedShip;
        private ServerSubLevel cachedParent;
        private RigidBodyHandle cachedParentHandle;
        private RotorProfile profile = RotorProfile.NONE;
        private final Vector3d worldAxis = new Vector3d();
        private final Vector3d worldPivot = new Vector3d();
        private final Vector3d localWind = new Vector3d();
        private final Vector3d worldWind = new Vector3d();
        private final Vector3d localAxis = new Vector3d();
        private final Vector3d angularVelocity = new Vector3d();
        private final Vector3d parentAngularVelocity = new Vector3d();
        private final Vector3d parentLinearVelocity = new Vector3d();
        private final Vector3d parentPointVelocity = new Vector3d();
        private final Vector3d impulse = new Vector3d();
        private final Vector3d inertiaDirection = new Vector3d();

        public void physicsTick(BlockEntity bearing, ServerSubLevel ship, RigidBodyHandle handle, double timeStep) {
            if (!(bearing.getLevel() instanceof ServerLevel level) || !handle.isValid()
                    || !(timeStep > 0.0) || timeStep > 0.2
                    || !WindConfig.AERONAUTICS_KINETIC_BEARING_ENABLED.get()) return;

            WindSnapshot wind = WIND.get(level);
            if (wind == null || !matchesAttachedRotor(bearing, ship)) return;

            // The native bearing rejects physics callbacks for the wrong body.
            // Our TAIL mixin also runs after native early returns, so explicitly
            // check its attached rotor ID before touching *any* physics body.
            // In particular, never apply rotor torque to the parent airship.
            // Match the bearing's axis transformation, including bearings on
            // moving parent sublevels. In particular, a vertical-axis bearing
            // has a Y axis: the old wind/axis dot product always gave zero.
            Direction bearingFacing = axisOf(bearing);
            worldAxis.set(bearingFacing.getStepX(), bearingFacing.getStepY(), bearingFacing.getStepZ());
            SubLevel containing = Sable.HELPER.getContaining(bearing);
            if (containing != null) containing.logicalPose().transformNormal(worldAxis, worldAxis);
            if (worldAxis.lengthSquared() < 1.0e-8) return;
            worldAxis.normalize();
            ship.logicalPose().transformNormalInverse(worldAxis, localAxis);
            if (localAxis.lengthSquared() < 1.0e-8) return;
            localAxis.normalize();

            // Bearing block coordinates refer to Sable's plot grid when the
            // bearing is mounted on a ship. Resolve the *world* position for
            // altitude sampling and local apparent wind. A ground bearing's
            // coordinates remain unchanged.
            worldPivot.set(bearing.getBlockPos().getX() + 0.5,
                    bearing.getBlockPos().getY() + 0.5, bearing.getBlockPos().getZ() + 0.5);
            if (containing != null) containing.logicalPose().transformPosition(worldPivot, worldPivot);
            RigidBodyHandle parentHandle = containing instanceof ServerSubLevel parent && parent != ship
                    ? getParentHandle(parent) : null;
            if (parentHandle == null) {
                cachedParent = null;
                cachedParentHandle = null;
            }

            // Sable attaches a distinct sublevel to each assembled bearing.
            // We verified its identity above even if native physics returned
            // early before our TAIL mixin was invoked.
            long tick = level.getGameTime();
            if (needsRefresh(ship, tick)) {
                // The bearing pivot is in the rotor's local coordinates for
                // the cached sail lever arms (including asymmetrical rotors).
                Vector3d pivotLocal = ship.logicalPose().transformPositionInverse(worldPivot, new Vector3d());
                profile = inspect(ship, localAxis, pivotLocal);
                cachedShip = ship;
                lastScanTick = tick;
                if (WindConfig.AERONAUTICS_KINETIC_BEARING_DEBUG.get()) {
                    LOG.info("Living Wind rotor at {}: body {}, {} sails ({} regular, {} symmetric), radius {}, regular N/S/E/W/U/D: {}/{}/{}/{}/{}/{}, symmetric axes X/Y/Z: {}/{}/{}",
                            bearing.getBlockPos(), ship.getUniqueId(), profile.sails,
                            profile.directionalSails, profile.symmetricSails,
                            String.format(java.util.Locale.ROOT, "%.2f", profile.radius),
                            profile.faces[Direction.NORTH.ordinal()], profile.faces[Direction.SOUTH.ordinal()],
                            profile.faces[Direction.EAST.ordinal()], profile.faces[Direction.WEST.ordinal()],
                            profile.faces[Direction.UP.ordinal()], profile.faces[Direction.DOWN.ordinal()],
                            profile.symmetricFaces[Direction.EAST.ordinal()],
                            profile.symmetricFaces[Direction.UP.ordinal()],
                            profile.symmetricFaces[Direction.SOUTH.ordinal()]);
                }
            }
            if (profile.sails < 2 || profile.radius < 0.5) return;
            ACTIVE_ROTORS.put(ship.getUniqueId(), tick);

            // Living Wind's 0° points +Z and 90° points -X. Convert its
            // world-space wind to dimensionless speed at the bearing's *real*
            // altitude, then subtract the velocity of the ship *at the hub*.
            // This handles translation as well as the tangential velocity of
            // a ship that is itself turning. Sable's physics API reports m/s.
            // A stationary parent or a ground bearing retains identical drive.
            double ambientStrength = Math.max(0.0, Math.min(3.0, wind.strength()
                    * WindApi.altitudeMultiplier(level, (int) Math.floor(worldPivot.y))));
            double radians = Math.toRadians(wind.directionDegrees());
            worldWind.set(-Math.sin(radians) * ambientStrength, 0.0,
                    Math.cos(radians) * ambientStrength);
            double parentSpeed = 0.0;
            if (parentHandle != null) {
                parentHandle.getLinearVelocity(parentLinearVelocity);
                parentHandle.getAngularVelocity(parentAngularVelocity);
                // v(hub) = v(parent COM) + omega(parent) x (hub - parent origin).
                // Match Sable's own point-velocity convention: its pose position
                // is the rotation origin used by its getVelocity helper.
                parentPointVelocity.set(worldPivot).sub(containing.logicalPose().position());
                parentAngularVelocity.cross(parentPointVelocity, parentPointVelocity);
                parentPointVelocity.add(parentLinearVelocity);
                if (!Double.isFinite(parentPointVelocity.lengthSquared())) return;
                parentSpeed = parentPointVelocity.length();
                worldWind.sub(parentPointVelocity.mul(1.0 / WIND_SPEED_METERS_PER_SECOND));
            }
            if (!Double.isFinite(worldWind.lengthSquared())) return;
            double strength = Math.min(3.0, worldWind.length());
            if (strength < 1.0e-6) return;
            ship.logicalPose().transformNormalInverse(worldWind, localWind);
            if (localWind.lengthSquared() < 1.0e-8) return;
            localWind.normalize();

            // A Create sail's FACING is its front. Air hitting its BACK is not
            // allowed to contribute drive. Air that hits the front produces a
            // turning moment from sail-facing and its distance to the axle.
            // This also powers vertical-axis turbines without inventing
            // a vertical component of wind.
            double turningMoment = 0.0;
            for (Direction face : SAIL_FACES) {
                int n = profile.faces[face.ordinal()];
                if (n == 0) continue;
                double incoming = -(localWind.x * face.getStepX()
                        + localWind.y * face.getStepY() + localWind.z * face.getStepZ());
                if (incoming <= 0.0) continue;
                // Facing the oncoming wind is much more effective than a
                // grazing hit; a one-sided flat plate has no rear drive.
                double pressureAtFace = incoming * incoming;
                turningMoment += profile.moments[face.ordinal()] * pressureAtFace;

                // Gameplay approximation for flat conventional (horizontal-
                // axis) windmills. Cardinal-direction Create sails cannot
                // express actual blade pitch; a symmetric face-on rotor has
                // zero physical turning moment. Retain a stable, modest
                // clockwise/counter-clockwise drive when the front faces the
                // wind *along* a horizontal axle, but never from the back.
                // Directional lever-arm moments handle other configurations.
                if (Math.abs(localAxis.y) < 0.5) {
                    double normalAlongAxle = localAxis.x * face.getStepX()
                            + localAxis.y * face.getStepY() + localAxis.z * face.getStepZ();
                    if (Math.abs(normalAlongAxle) > 0.95) {
                        turningMoment += n * profile.radius * normalAlongAxle * pressureAtFace * 0.65;
                    }
                }
            }
            // Simulated symmetric sails have AXIS (not FACING) and no front or
            // back. Use a canonical positive normal for each axis, and SIGNED
            // incident pressure: the opposite side pushes with the opposite
            // force. This makes the outcome independent of how the sail is
            // placed/flipped, while retaining real lever-arm torque around
            // vertical or horizontal axles. The geometry is cached, so this
            // adds at most three dot products per physics substep.
            for (Direction normal : SYMMETRIC_NORMALS) {
                int n = profile.symmetricFaces[normal.ordinal()];
                if (n == 0) continue;
                double incoming = -(localWind.x * normal.getStepX()
                        + localWind.y * normal.getStepY() + localWind.z * normal.getStepZ());
                double signedPressure = incoming * Math.abs(incoming);
                turningMoment += profile.symmetricMoments[normal.ordinal()] * signedPressure;

                // Keep the existing modest gameplay drive for unpitched,
                // horizontal-axis rotors. Without actual pitch, equally sized
                // flat panels would otherwise create zero turning moment.
                // Unlike regular sails, pressure from the opposite side also
                // contributes, reversing the drive when the wind reverses.
                if (Math.abs(localAxis.y) < 0.5) {
                    double normalAlongAxle = localAxis.x * normal.getStepX()
                            + localAxis.y * normal.getStepY() + localAxis.z * normal.getStepZ();
                    if (Math.abs(normalAlongAxle) > 0.95) {
                        turningMoment += n * profile.radius * normalAlongAxle * signedPressure * 0.65;
                    }
                }
            }
            // The sail moments above provide the real directional drive, but
            // lightweight rotors frequently hit the angular acceleration cap
            // at strong wind. If that cap is constant, even very different
            // wind angles accelerate the rotor at the exact same rate. Form a
            // conservative upper bound for the cached sail geometry (no block
            // scanning) and use its fraction to preserve the alignment effect
            // in the high-wind safety limit. The low-wind limit is unchanged.
            double directionalEfficiency = profile.maximumMoment > 1.0e-8
                    ? Math.min(1.0, Math.abs(turningMoment) / profile.maximumMoment)
                    : 0.0;

            // Pressure follows the apparent wind speed and direction. The
            // previous stationary-rotor pressure curve is preserved exactly.
            double pressure = 0.30 * strength + 0.70 * strength * strength;
            double driveTorque = TORQUE_PER_SAIL_RADIUS * turningMoment * pressure
                    * WindConfig.AERONAUTICS_KINETIC_BEARING_MULTIPLIER.get();
            if (!Double.isFinite(driveTorque)) return;

            // The kinetic conversion bearing generates RPM from rotor speed
            // RELATIVE to the body it is mounted on. Use the exact same frame
            // for our aerodynamic drag and free-spin limit. Absolute rotor RPM
            // makes ship rotation artificially cancel or overspeed the drive.
            double omega = handle.getAngularVelocity(angularVelocity).dot(worldAxis)
                    - (parentHandle != null ? parentAngularVelocity.dot(worldAxis) : 0.0);
            // Without a drag term the rotor could accelerate indefinitely even
            // in constant wind, particularly with resistance set to zero.
            // Stop adding *wind* torque above the windmill's safe free-spin
            // range. External movement is left to the native bearing physics.
            if (Math.abs(omega) >= MAX_WIND_DRIVEN_OMEGA
                    && Math.signum(omega) == Math.signum(driveTorque)) {
                driveTorque = 0.0;
            }
            double dragTorque = DRAG_PER_SAIL_RADIUS_SQUARED * profile.sails * profile.radius * profile.radius
                    * omega * Math.abs(omega);
            double rawTorque = driveTorque - dragTorque;
            double higherWindFraction = Math.min(1.0, Math.max(0.0, (strength - 0.5) / 1.5));
            double torqueLimit = BASE_MAX_TORQUE + (MAX_TORQUE - BASE_MAX_TORQUE) * higherWindFraction;
            boolean torqueLimited = Math.abs(rawTorque) > torqueLimit;
            double torque = Math.max(-torqueLimit, Math.min(torqueLimit, rawTorque));
            if (!Double.isFinite(torque)) return;
            double angularImpulse = torque * timeStep;
            // Limit angular acceleration (rad/s²), rather than applying a
            // fixed angular velocity jump on EVERY physics substep. This gives
            // similar gust response at different Sable physics tick rates and
            // avoids instant spin-up for very light contraptions.
            if (ship.getMassTracker() == null || ship.getMassTracker().getInverseInertiaTensor() == null) return;
            ship.getMassTracker().getInverseInertiaTensor().transform(localAxis, inertiaDirection);
            double inverseInertia = localAxis.dot(inertiaDirection);
            if (!(inverseInertia > 1.0e-12) || !Double.isFinite(inverseInertia)) return;
            // A fixed cap here was masking the previous 3x torque change:
            // any strength that exceeded 2 rad/s² produced almost the same
            // drive against the native bearing's resistance. Preserve that
            // cap at <=0.5 wind and open it smoothly at higher strengths.
            double excessStrength = Math.max(0.0, strength - 0.5);
            double strengthAccelerationLimit = Math.min(MAX_ANGULAR_ACCELERATION,
                    BASE_ANGULAR_ACCELERATION + EXTRA_ACCELERATION_PER_STRENGTH * excessStrength);
            // At <=0.5 wind this is exactly the existing 2 rad/s^2 limiter.
            // Only the additional high-wind headroom scales with the actual
            // sail alignment, so a mostly edge-on wind cannot accelerate the
            // rotor as fast as a wind that efficiently catches its sails.
            // The 1.5 factor compensates for the upper bound being deliberately
            // conservative (individual faces cannot all face the wind at once).
            double directionFactor = Math.min(1.0, directionalEfficiency * 1.5);
            double maxAcceleration = BASE_ANGULAR_ACCELERATION
                    + (strengthAccelerationLimit - BASE_ANGULAR_ACCELERATION) * directionFactor;
            double maxImpulse = maxAcceleration * timeStep / inverseInertia;
            boolean accelerationLimited = Math.abs(angularImpulse) > maxImpulse;
            angularImpulse = Math.max(-maxImpulse, Math.min(maxImpulse, angularImpulse));

            if (WindConfig.AERONAUTICS_KINETIC_BEARING_DEBUG.get()
                    && (lastDiagnosticsTick == Long.MIN_VALUE || tick < lastDiagnosticsTick
                        || tick - lastDiagnosticsTick >= 100L)) {
                lastDiagnosticsTick = tick;
                LOG.info("Living Wind rotor drive at {}: shipMounted={}, worldY={}, wind={}, windDir={}, localWind=({},{},{}), worldAxis=({},{},{}), shipHubSpeed={}m/s, relativeRPM={}, moment={}, alignment={}, drive={}, drag={}, net={}, accelLimit={}, torqueCapped={}, accelCapped={}",
                        bearing.getBlockPos(), parentHandle != null,
                        String.format(java.util.Locale.ROOT, "%.1f", worldPivot.y),
                        String.format(java.util.Locale.ROOT, "%.2f", strength),
                        String.format(java.util.Locale.ROOT, "%.0f", wind.directionDegrees()),
                        String.format(java.util.Locale.ROOT, "%.2f", localWind.x),
                        String.format(java.util.Locale.ROOT, "%.2f", localWind.y),
                        String.format(java.util.Locale.ROOT, "%.2f", localWind.z),
                        String.format(java.util.Locale.ROOT, "%.2f", worldAxis.x),
                        String.format(java.util.Locale.ROOT, "%.2f", worldAxis.y),
                        String.format(java.util.Locale.ROOT, "%.2f", worldAxis.z),
                        String.format(java.util.Locale.ROOT, "%.2f", parentSpeed),
                        String.format(java.util.Locale.ROOT, "%.2f", omega * 60.0 / (Math.PI * 2.0)),
                        String.format(java.util.Locale.ROOT, "%.1f", turningMoment),
                        String.format(java.util.Locale.ROOT, "%.2f", directionFactor),
                        String.format(java.util.Locale.ROOT, "%.0f", driveTorque),
                        String.format(java.util.Locale.ROOT, "%.0f", dragTorque),
                        String.format(java.util.Locale.ROOT, "%.0f", torque),
                        String.format(java.util.Locale.ROOT, "%.2f", maxAcceleration),
                        torqueLimited, accelerationLimited);
            }
            if (Math.abs(angularImpulse) > 1.0e-12) {
                handle.applyAngularImpulse(impulse.set(localAxis).mul(angularImpulse));
            }
        }

        // Physics calls can happen more often than the Minecraft tick rate.
        // Reuse a live parent handle instead of obtaining it per substep.
        private RigidBodyHandle getParentHandle(ServerSubLevel parent) {
            if (cachedParent != parent || cachedParentHandle == null || !cachedParentHandle.isValid()) {
                cachedParent = parent;
                cachedParentHandle = RigidBodyHandle.of(parent);
            }
            return cachedParentHandle != null && cachedParentHandle.isValid() ? cachedParentHandle : null;
        }

        private boolean needsRefresh(ServerSubLevel ship, long tick) {
            return ship != cachedShip || tick < lastScanTick || tick - lastScanTick >= REFRESH_TICKS;
        }
    }

    private static Direction axisOf(BlockEntity bearing) {
        // Simulated's SwivelBearingBlock.FACING is an ordinary named Direction
        // property. Resolve by name so there is no hard dependency on Simulated.
        BlockState state = bearing.getBlockState();
        for (var property : state.getProperties()) {
            if (property.getName().equals("facing")) {
                Comparable<?> facing = state.getValues().get(property);
                if (facing instanceof Direction direction) return direction;
            }
        }
        return Direction.NORTH;
    }

    /**
     * Scan loaded/occupied Sable sections only, once per 200 game ticks.
     * Regular Create sails use FACING (one-sided); Simulated symmetric sails
     * use AXIS (two-sided). Cache their counts and signed lever arms separately
     * to avoid iterating moving blocks in the physics callback.
     */
    private static RotorProfile inspect(ServerSubLevel ship, Vector3d axis, Vector3d pivot) {
        int count = 0;
        int directionalCount = 0;
        int symmetricCount = 0;
        int scannedChunks = 0;
        int[] faces = new int[SAIL_FACES.length];
        double[] moments = new double[SAIL_FACES.length];
        int[] symmetricFaces = new int[SAIL_FACES.length];
        double[] symmetricMoments = new double[SAIL_FACES.length];
        double radiusSquaredSum = 0.0;
        for (PlotChunkHolder holder : ship.getPlot().getLoadedChunks()) {
            if (++scannedChunks > MAX_SCAN_CHUNKS) break;
            LevelChunk chunk = holder.getChunk();
            if (chunk == null || chunk.isEmpty()) continue;
            LevelChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                LevelChunkSection section = sections[sectionIndex];
                if (section == null || section.hasOnlyAir()
                        || !section.maybeHas(state -> state.is(SAILS) || state.is(SYMMETRIC_SAILS))) continue;
                int baseX = chunk.getPos().getMinBlockX();
                int baseY = SectionPos.sectionToBlockCoord(chunk.getMinSection() + sectionIndex);
                int baseZ = chunk.getPos().getMinBlockZ();
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState state = section.getBlockState(x, y, z);
                            // Simulated symmetric sails can share the Create
                            // windmill_sails tag. Prioritize their two-sided
                            // AXIS state over the ordinary FACING state.
                            boolean symmetric = state.is(SYMMETRIC_SAILS)
                                    && state.hasProperty(BlockStateProperties.AXIS);
                            if (!symmetric && (!state.is(SAILS)
                                    || !state.hasProperty(BlockStateProperties.FACING))) continue;
                            Direction normal;
                            if (symmetric) {
                                // AXIS is not oriented: +/- are identical.
                                normal = switch (state.getValue(BlockStateProperties.AXIS)) {
                                    case X -> Direction.EAST;
                                    case Y -> Direction.UP;
                                    case Z -> Direction.SOUTH;
                                };
                            } else {
                                normal = state.getValue(BlockStateProperties.FACING);
                            }
                            int index = normal.ordinal();
                            double rx = baseX + x + 0.5 - pivot.x;
                            double ry = baseY + y + 0.5 - pivot.y;
                            double rz = baseZ + z + 0.5 - pivot.z;
                            double axialOffset = rx * axis.x + ry * axis.y + rz * axis.z;
                            double radialSq = rx * rx + ry * ry + rz * rz - axialOffset * axialOffset;
                            if (radialSq < 0.0) radialSq = 0.0;
                            radiusSquaredSum += radialSq;
                            // axis dot (offset cross -normal): signed lever
                            // arm for pressure on the canonical front side.
                            double moment = axis.x * (rz * normal.getStepY() - ry * normal.getStepZ())
                                    + axis.y * (rx * normal.getStepZ() - rz * normal.getStepX())
                                    + axis.z * (ry * normal.getStepX() - rx * normal.getStepY());
                            if (symmetric) {
                                symmetricFaces[index]++;
                                symmetricMoments[index] += moment;
                                symmetricCount++;
                            } else {
                                faces[index]++;
                                moments[index] += moment;
                                directionalCount++;
                            }
                            if (++count >= MAX_SCAN_SAILS) break;
                        }
                        if (count >= MAX_SCAN_SAILS) break;
                    }
                    if (count >= MAX_SCAN_SAILS) break;
                }
                if (count >= MAX_SCAN_SAILS) break;
            }
            if (count >= MAX_SCAN_SAILS) break;
        }
        if (count < 2) return RotorProfile.NONE;
        double radius = Math.min(12.0, Math.sqrt(radiusSquaredSum / count));
        // Prevent far-flung sails from producing extreme torque.
        double maxMoment = count * radius;
        for (int i = 0; i < moments.length; i++) {
            moments[i] = Math.max(-maxMoment, Math.min(maxMoment, moments[i]));
            symmetricMoments[i] = Math.max(-maxMoment, Math.min(maxMoment, symmetricMoments[i]));
        }
        // Cache a geometry-only moment upper bound so the directional
        // acceleration limiter adds no sail loops to the physics callback.
        double maximumMoment = 0.0;
        for (Direction face : SAIL_FACES) {
            int i = face.ordinal();
            maximumMoment += Math.abs(moments[i]) + Math.abs(symmetricMoments[i]);
            if (Math.abs(axis.y) < 0.5) {
                double alongAxle = axis.x * face.getStepX()
                        + axis.y * face.getStepY() + axis.z * face.getStepZ();
                if (Math.abs(alongAxle) > 0.95) {
                    maximumMoment += (faces[i] + symmetricFaces[i]) * radius * 0.65;
                }
            }
        }
        return new RotorProfile(count, directionalCount, symmetricCount, radius,
                faces, moments, symmetricFaces, symmetricMoments, maximumMoment);
    }

    private record RotorProfile(int sails, int directionalSails, int symmetricSails,
            double radius, int[] faces, double[] moments,
            int[] symmetricFaces, double[] symmetricMoments, double maximumMoment) {
        private static final RotorProfile NONE = new RotorProfile(0, 0, 0, 0.0,
                new int[6], new double[6], new int[6], new double[6], 0.0);
    }
}
