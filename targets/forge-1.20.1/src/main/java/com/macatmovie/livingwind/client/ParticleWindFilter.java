package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.api.ParticleWindApi;
import com.macatmovie.livingwind.client.access.WindParticleAccess;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class ParticleWindFilter {
    private static final ResourceLocation AMBIENT_PIXEL_ID = LivingWind.id("ambient_pixel");
    private static final ResourceLocation WIND_LEAF_ID = LivingWind.id("wind_leaf");
    private static final ResourceLocation WIND_STREAK_ID = LivingWind.id("wind_streak");
    private static final ResourceLocation WIND_NEEDLES_ID = LivingWind.id("wind_needles");
    private static final ResourceLocation WIND_GRASS_ID = LivingWind.id("wind_grass");
    private static final ResourceLocation VANILLA_BLOCK_FRAGMENT_ID = new ResourceLocation("minecraft", "block");
    private static final TagKey<net.minecraft.core.particles.ParticleType<?>> WIND_IMMUNE_TAG =
        TagKey.create(Registries.PARTICLE_TYPE, LivingWind.id("wind_immune"));
    private static final ResourceLocation PRETTY_RAIN_RAIN_ID = new ResourceLocation("particlerain", "rain");
    private static final ResourceLocation PRETTY_RAIN_DUST_ID = new ResourceLocation("particlerain", "dust");
    private static final ResourceLocation PRETTY_RAIN_DUST_MOTE_ID = new ResourceLocation("particlerain", "dust_mote");
    private static final ResourceLocation PRETTY_RAIN_SHRUB_ID = new ResourceLocation("particlerain", "shrub");

    private static volatile Set<ResourceLocation> ids = Set.of();
    private static volatile Map<ResourceLocation, Float> responseMultipliers = Map.of();
    private static volatile ClientWindConfig.FilterMode mode = ClientWindConfig.FilterMode.BLACKLIST;
    private static volatile boolean enabled = true;
    private static volatile float force = 0.12f;
    private static volatile boolean prettyRainCompat = true;
    private static volatile boolean particleRainCompat = true;
    private static volatile float particleRainWindMultiplier = 0.55f;
    private static volatile float prettyRainWindMultiplier = 0.55f;
    private static final ResourceLocation PARTICULAR_LEAVES_MARKER = new ResourceLocation("particular", "oak_leaf");
    private static volatile ClientWindConfig.FallingLeavesSource fallingLeavesSource = ClientWindConfig.FallingLeavesSource.LIVING_WIND;
    private static volatile boolean particularInstalled;
    private static volatile boolean particularLeavesCompat = true;
    private static volatile float particularLeafMinDensity = 0.15f;
    private static volatile float particularLeafMaxDensity = 3.0f;
    private static volatile float particularLeafFullWind = 1.20f;
    private static volatile float ambientWindResponseScale = 2.0f;
    private static volatile float ambientLiftMultiplier = 1.0f;
    private static volatile float ambientSwayMultiplier = 1.0f;

    /** UI scale 1.0 is calibrated to the old user-tuned 3.0 value; current default is 2.0. */
    private static final float AMBIENT_RESPONSE_BASELINE = 3.0f;

    private ParticleWindFilter() {}

    // Called only on the normal client thread. The particle hook sees immutable published snapshots.
    public static void refresh() {
        enabled = ClientWindConfig.AFFECT_EXISTING_PARTICLES.get();
        force = ClientWindConfig.PARTICLE_WIND_FORCE.get().floatValue();
        mode = ClientWindConfig.PARTICLE_FILTER_MODE.get();
        prettyRainCompat = ClientWindConfig.PRETTY_RAIN_COMPAT.get();
        particleRainCompat = ClientWindConfig.PARTICLE_RAIN_COMPAT.get();
        particleRainWindMultiplier = ClientWindConfig.PARTICLE_RAIN_WIND_MULTIPLIER.get().floatValue();
        prettyRainWindMultiplier = ClientWindConfig.PRETTY_RAIN_WIND_MULTIPLIER.get().floatValue();
        fallingLeavesSource = ClientWindConfig.FALLING_LEAVES_SOURCE.get();
        // Avoid repeated registry lookups while sampling canopy emitters each tick.
        particularInstalled = BuiltInRegistries.PARTICLE_TYPE.containsKey(PARTICULAR_LEAVES_MARKER);
        particularLeavesCompat = ClientWindConfig.PARTICULAR_FALLING_LEAVES_COMPAT.get();
        particularLeafMinDensity = ClientWindConfig.PARTICULAR_LEAF_MIN_DENSITY.get().floatValue();
        particularLeafMaxDensity = ClientWindConfig.PARTICULAR_LEAF_MAX_DENSITY.get().floatValue();
        particularLeafFullWind = ClientWindConfig.PARTICULAR_LEAF_FULL_WIND_STRENGTH.get().floatValue();
        ambientWindResponseScale = ClientWindConfig.AMBIENT_WIND_RESPONSE_SCALE.get().floatValue();
        ambientLiftMultiplier = ClientWindConfig.AMBIENT_LIFT_MULTIPLIER.get().floatValue();
        ambientSwayMultiplier = ClientWindConfig.AMBIENT_SWAY_MULTIPLIER.get().floatValue();

        Set<ResourceLocation> next = new HashSet<>();
        for (String raw : ClientWindConfig.PARTICLE_FILTER.get()) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null) next.add(id);
        }
        // Built-ins are enabled only in BLACKLIST mode: whitelist means exactly the
        // player's chosen IDs. Cached once on config refresh, never per particle tick.
        if (mode == ClientWindConfig.FilterMode.BLACKLIST) {
            for (String raw : BuiltinParticleBlacklist.installedIds()) {
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id != null) next.add(id);
            }
        }
        ids = Set.copyOf(next);

        // Publish immutable built-ins plus user overrides. The built-in fallback also
        // covers worlds with a pre-existing config that predates these defaults.
        Map<ResourceLocation, Float> nextResponses = new HashMap<>(BuiltinParticleResponseMultipliers.DEFAULTS);
        for (String raw : ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.get()) {
            int eq = raw.lastIndexOf('=');
            if (eq <= 0 || eq >= raw.length() - 1) continue;
            ResourceLocation id = ResourceLocation.tryParse(raw.substring(0, eq).trim());
            if (id == null) continue;
            try {
                float multiplier = Float.parseFloat(raw.substring(eq + 1).trim());
                if (Float.isFinite(multiplier)) {
                    nextResponses.put(id, Mth.clamp(multiplier, 0.0f, 10.0f));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        responseMultipliers = Map.copyOf(nextResponses);
    }

    /**
     * Lets Living Wind supply the ONLY particle wind when Dynamic Surroundings is installed.
     * Disabling Living Wind's generic particle wind restores DS's own wind normally.
     * This is separate from the user's Dynamic Surroundings *audio* selection.
     */
    public static boolean controlsDynamicSurroundingsParticleWind() {
        return enabled && ClientWindState.hasServerState();
    }

    public static boolean shouldAffect(@Nullable ResourceLocation id, @Nullable Particle particle) {
        if (!enabled) return false;
        // A few visual-effect mods (including Visuality 3.x) use their own particle
        // registry or directly add particle instances, so an ID blacklist alone
        // cannot catch them. Uses a class-name cache; no optional mod is loaded.
        if (BuiltinParticleClassBlacklist.contains(particle)) return false;
        // Official Particle Rain 4.x drives its weather particles through its own wind manager.
        // Never apply a second generic push, whether the optional integration is on or off.
        // Most of its v4 effects are directly constructed, so their particle IDs are unavailable.
        if (particle != null && particle.getClass().getName().startsWith("pigcart.particlerain.particle.")) return false;
        // ParticleEngine#createParticle normally supplies an ID via our mixin. However,
        // Minecraft creates some hit/breaking block fragments as TerrainParticle directly,
        // bypassing that method. Treat those untyped fragments as minecraft:block so the
        // normal blacklist (including built-ins and user additions) applies to them.
        // This is a cheap class check, not a block/world lookup.
        if (id == null && particle instanceof TerrainParticle) id = VANILLA_BLOCK_FRAGMENT_ID;
        if (ParticleWindApi.isRuntimeImmune(id) || ParticleWindApi.isRuntimeImmune(particle) || isTaggedImmune(id)) return false;
        // Living Wind's own mote continuously steers itself; applying this generic hook as well
        // would double the force. Pretty Rain rain/sand have dedicated velocity compatibility,
        // while its rolling shrub/tumbleweed keeps its own speed and only borrows wind direction.
        // Every native foliage particle steers itself, including the new
        // conifer needles. Generic wind here would add a SECOND full-strength
        // push on top of the specially tuned needle movement.
        if (WIND_STREAK_ID.equals(id) || AMBIENT_PIXEL_ID.equals(id) || WIND_LEAF_ID.equals(id) || WIND_NEEDLES_ID.equals(id) || WIND_GRASS_ID.equals(id) || usesPrettyRainVelocityCompat(id) || usesPrettyRainDirectionOnlyCompat(id)) return false;
        return passesConfiguredFilter(id);
    }

    public static boolean usesPrettyRainVelocityCompat(@Nullable ResourceLocation id) {
        if (!enabled || !prettyRainCompat || id == null || !passesConfiguredFilter(id)) return false;
        return PRETTY_RAIN_RAIN_ID.equals(id) || PRETTY_RAIN_DUST_ID.equals(id) || PRETTY_RAIN_DUST_MOTE_ID.equals(id);
    }

    public static boolean isPrettyRainSand(@Nullable ResourceLocation id) {
        return PRETTY_RAIN_DUST_ID.equals(id) || PRETTY_RAIN_DUST_MOTE_ID.equals(id);
    }

    public static boolean usesPrettyRainDirectionOnlyCompat(@Nullable ResourceLocation id) {
        if (!enabled || !prettyRainCompat || id == null || !passesConfiguredFilter(id)) return false;
        return PRETTY_RAIN_SHRUB_ID.equals(id);
    }

    /**
     * Reorients a Pretty Rain shrub/tumbleweed to Living Wind while preserving the particle's
     * existing horizontal speed. This deliberately ignores wind strength: Pretty Rain remains
     * responsible for how fast the shrub rolls, bounces and slows; Living Wind only supplies
     * the horizontal direction.
     */
    public static void orientPrettyRainDirectionOnly(WindParticleAccess access) {
        if (ParticleWaterProtection.isSubmerged(access.livingwind$getPositionX(),
                access.livingwind$getPositionY(), access.livingwind$getPositionZ())
            || WindShelter.weatherExposureAt(access.livingwind$getPositionX(),
                access.livingwind$getPositionY(), access.livingwind$getPositionZ()) <= 0.01f) return;
        double originalX = access.livingwind$getVelocityX();
        double originalZ = access.livingwind$getVelocityZ();
        double speed = Math.hypot(originalX, originalZ);
        if (speed < 1.0e-7) return;

        double radians = Math.toRadians(ClientWindState.directionDegrees());
        double x = -Math.sin(radians) * speed;
        double z =  Math.cos(radians) * speed;

        // Pretty Rain's shrub currently removes itself if either horizontal coordinate does not
        // change for a tick. Avoid exact zero at cardinal directions without perceptibly changing
        // its direction or total speed.
        final double minComponent = Math.min(1.0e-4, speed * 0.001);
        if (Math.abs(x) < minComponent) x = Math.copySign(minComponent, x == 0.0 ? 1.0 : x);
        if (Math.abs(z) < minComponent) z = Math.copySign(minComponent, z == 0.0 ? 1.0 : z);

        double adjusted = Math.hypot(x, z);
        if (adjusted > 1.0e-7) {
            double scale = speed / adjusted;
            x *= scale;
            z *= scale;
        }
        access.livingwind$setHorizontalVelocity(x, z);
    }

    private static boolean isTaggedImmune(@Nullable ResourceLocation id) {
        if (id == null) return false;
        var type = BuiltInRegistries.PARTICLE_TYPE.get(id);
        return type != null && BuiltInRegistries.PARTICLE_TYPE.wrapAsHolder(type).is(WIND_IMMUNE_TAG);
    }

    private static boolean passesConfiguredFilter(@Nullable ResourceLocation id) {
        if (id == null) return mode == ClientWindConfig.FilterMode.BLACKLIST;
        boolean contains = ids.contains(id);
        return mode == ClientWindConfig.FilterMode.BLACKLIST ? !contains : contains;
    }

    /** Internal movement multiplier. UI scale 1.0 intentionally equals the previous hand-tuned 3.0. */
    public static float ambientWindResponseMultiplier() {
        return AMBIENT_RESPONSE_BASELINE * ambientWindResponseScale;
    }

    /** User-facing ambient response scale shown in config/debug. */
    public static float ambientWindResponseScale() {
        return ambientWindResponseScale;
    }

    public static float ambientLiftMultiplier() {
        return ambientLiftMultiplier;
    }

    public static float ambientSwayMultiplier() {
        return ambientSwayMultiplier;
    }

    /**
     * Official Particle Rain 4.x uses *cumulative* wind acceleration, not Pretty Rain's
     * direct per-tick horizontal velocity. Feed its WindManager a small force vector
     * matching the newer integration's physical units; never reuse the 0.55 velocity.
     * Called from Particle Rain's optional getWind hook, including async particle workers.
     */
    public static boolean drivesOfficialParticleRain() {
        return enabled && particleRainCompat && ClientWindState.hasServerState();
    }

    public static org.joml.Vector3f officialParticleRainWind(double x, double y, double z) {
        // The same cache-only shelter and water lookups used by the generic particle path.
        if (ParticleWaterProtection.isSubmerged(x, y, z)) return new org.joml.Vector3f();
        final float exposure = WindShelter.weatherExposureAt(x, y, z);
        // Particle Rain v4's built-in getWind peaks around 0.06 at default settings.
        // It is integrated over particle lifetime by the mod's own WindManager.
        final float scale = 0.03f * (particleRainWindMultiplier / 0.55f) * exposure;
        return new org.joml.Vector3f(
            ClientWindState.altitudeWindX() * scale,
            0.0f,
            ClientWindState.altitudeWindZ() * scale
        );
    }

    public static double prettyRainVelocityX(double x, double y, double z) {
        float exposure = WindShelter.weatherExposureAt(x, y, z);
        if (exposure <= 0.0f) return 0.0; // No fake cardinal-axis motion in sheltered caves.
        return cardinalSafe(ClientWindState.altitudeWindX() * exposure * prettyRainWindMultiplier, ClientWindState.directionDegrees(), true);
    }

    public static double prettyRainVelocityZ(double x, double y, double z) {
        float exposure = WindShelter.weatherExposureAt(x, y, z);
        if (exposure <= 0.0f) return 0.0;
        return cardinalSafe(ClientWindState.altitudeWindZ() * exposure * prettyRainWindMultiplier, ClientWindState.directionDegrees(), false);
    }

    private static double cardinalSafe(double velocity, float direction, boolean xAxis) {
        // Pretty Rain currently treats an exactly unchanged X or Z coordinate as an obstruction.
        // Avoid exact zero at cardinal wind angles while keeping the visual result indistinguishable.
        if (Math.abs(velocity) >= 0.0001) return velocity;
        double radians = Math.toRadians(direction);
        double signSource = xAxis ? -Math.sin(radians) : Math.cos(radians);
        return Math.copySign(0.0001, signSource == 0.0 ? 1.0 : signSource);
    }

    public static float offsetX(@Nullable ResourceLocation id, @Nullable Particle particle) {
        if (!shouldAffect(id, particle) || ClientWindState.altitudeStrength() < 0.001f
                || isSubmerged(particle)) return 0.0f;
        float localExposure = exposure(particle, id);
        return ClientWindState.altitudeWindX() * localExposure * force
            * response(id) * customResponse(id) * highWindVisualMultiplier();
    }

    public static float offsetZ(@Nullable ResourceLocation id, @Nullable Particle particle) {
        if (!shouldAffect(id, particle) || ClientWindState.altitudeStrength() < 0.001f
                || isSubmerged(particle)) return 0.0f;
        return ClientWindState.altitudeWindZ() * exposure(particle, id) * force
            * response(id) * customResponse(id) * highWindVisualMultiplier();
    }

    private static boolean isSubmerged(@Nullable Particle particle) {
        if (!(particle instanceof WindParticleAccess access)) return false;
        return ParticleWaterProtection.isSubmerged(
            access.livingwind$getPositionX(), access.livingwind$getPositionY(), access.livingwind$getPositionZ());
    }

    /**
     * A linear force made the old defaults too visually similar across moderate and storm wind.
     * This curve keeps light wind restrained but lets the upper end become progressively obvious.
     */
    private static float exposure(@Nullable Particle particle, @Nullable ResourceLocation id) {
        if (!(particle instanceof WindParticleAccess access)) return 1.0f;
        final double x = access.livingwind$getPositionX();
        final double y = access.livingwind$getPositionY();
        final double z = access.livingwind$getPositionZ();
        final int now = WindShelter.shelterTick();
        float memo = access.livingwind$cachedExposure(now, x, y, z);
        if (!Float.isNaN(memo)) return memo;
        // A cache MISS is not evidence of wind. Keep a particle's last CONFIRMED
        // sheltered state through a tiny gap as it crosses exact-block cache cells.
        // Direct measurements of full outdoor exposure always take effect immediately.
        final float measured = WindShelter.trackedParticleExposureAt(x, y, z);
        final int bx = Mth.floor(x), by = Mth.floor(y), bz = Mth.floor(z);
        if (measured >= 0.0f && measured < 0.20f) {
            access.livingwind$rememberSheltered(now, bx, by, bz);
        }
        float effective = ParticleShelterContinuity.resolve(
            measured, now, access.livingwind$getLastShelteredTick(),
            bx, by, bz,
            access.livingwind$getLastShelteredX(),
            access.livingwind$getLastShelteredY(),
            access.livingwind$getLastShelteredZ());
        access.livingwind$storeExposure(now, x, y, z, effective);
        return effective;
    }

    private static float highWindVisualMultiplier() {
        float strength = Mth.clamp(ClientWindState.altitudeStrength(), 0.0f, 1.6f);
        return (0.55f + strength * 0.95f) * WindMath.visualDriftBoost(ClientWindState.altitudeStrength());
    }

    private static float response(@Nullable ResourceLocation id) {
        if (id == null) return 0.70f;
        String p = id.getPath();
        if (p.contains("smoke") || p.contains("ash") || p.contains("spore") ||
            p.contains("flame") || p.contains("snow") || p.contains("cherry")) return 1.25f;
        if (p.equals("block") || p.contains("dust") || p.contains("falling")) return 0.95f;
        return 0.70f;
    }

    private static float customResponse(@Nullable ResourceLocation id) {
        if (id == null) return 1.0f;
        return responseMultipliers.getOrDefault(id, 1.0f);
    }

    public static boolean shouldSpawnParticularLeaves() {
        return fallingLeavesSource != ClientWindConfig.FallingLeavesSource.LIVING_WIND;
    }

    public static boolean shouldSpawnNativeLeaves() {
        // Selecting Particular alone must not erase leaves when it isn't installed.
        return fallingLeavesSource != ClientWindConfig.FallingLeavesSource.PARTICULAR || !particularInstalled;
    }

    /** Particular Reforged currently registers all falling-leaf particle IDs under particular:*_leaf. */
    public static boolean isParticularLeaf(@Nullable ResourceLocation id) {
        return id != null && "particular".equals(id.getNamespace()) && id.getPath().endsWith("_leaf");
    }

    /**
     * Expected Particular leaf count per original Particular spawn attempt. The curve is deliberately
     * below 1 in weak air and rises non-linearly toward the configured maximum in gusty/strong wind.
     * Altitude-adjusted atmospheric wind is used rather than player shelter, because leaves outdoors
     * should still shed while the player watches from under cover.
     */
    public static float particularLeafSpawnMultiplier(@Nullable ResourceLocation id) {
        if (!isParticularLeaf(id)) return 1.0f;
        return particularLeafSpawnMultiplier();
    }

    public static float particularLeafSpawnMultiplier() {
        if (!shouldSpawnParticularLeaves()) return 0.0f;
        if (!particularLeavesCompat || !ClientWindState.hasServerState()) return 1.0f;
        float full = Math.max(0.10f, particularLeafFullWind);
        float normalized = Mth.clamp(ClientWindState.altitudeStrength() / full, 0.0f, 1.0f);
        float curved = (float) Math.pow(normalized, 1.35);
        float min = Math.max(0.0f, particularLeafMinDensity);
        float max = Math.max(min, particularLeafMaxDensity);
        return Mth.lerp(curved, min, max);
    }
}
