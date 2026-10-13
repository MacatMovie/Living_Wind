package com.macatmovie.livingwind.server;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.GustPulse;
import com.macatmovie.livingwind.wind.WindMath;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ServerLevelData;
import java.util.ArrayList;
import java.util.List;

public final class WindSavedData extends SavedData {
    private static final String DATA_NAME = "livingwind_wind";

    private float baseStrength = 0.12f;
    private float targetBaseStrength = 0.18f;
    private float baseDirection = 0.0f;
    private float targetDirection = 0.0f;
    private float turbulence = 0.28f;
    private float targetTurbulence = 0.28f;
    private float directionInstability = 0.25f;
    private float targetDirectionInstability = 0.25f;

    private float phaseA = 0.0f;
    private float phaseB = 1.0f;
    private long nextTargetTick = 0L;
    private long nextGustCheckTick = 0L;
    private long gustStartTick = -1L;
    private long gustEndTick = -1L;
    private float gustMagnitude = 0.0f;
    private float gustDirectionOffset = 0.0f;
    // 0 = original smooth sine, 1 = sudden arrival / long fade,
    // 2 = slow build / sharp fade, 3 = sharp arrival / sharp fade.
    private int gustShape = 0;
    private float gustAttackFraction = 0.25f;
    private float gustReleaseFraction = 0.35f;

    // Natural gust clusters: a normal gust can spawn 1-4 closely spaced followers.
    // Prior pulses remain active independently, so new gusts never cut off an earlier tail.
    private final List<GustPulse> overlappingGusts = new ArrayList<>();
    private int clusterGustsRemaining = 0;
    private long nextClusterGustTick = -1L;
    private long clusterEndTick = -1L;
    private long clusterCooldownUntilTick = 0L;

    private long profileDay = Long.MIN_VALUE;
    private float dailyBaseOffset = 0.0f;
    private float nightMultiplier = 0.68f;
    private boolean dayHasWindyPeriod = false;
    private int windyStartDayTick = 0;
    private int windyEndDayTick = 0;
    private float windyBoost = 0.0f;

    // Temporary command-driven test event. It overlays the natural daily schedule rather than
    // replacing the simulation, and expires automatically.
    private long manualWindyEndTick = -1L;
    private float manualWindyBoost = 0.0f;

    // Highest-priority explicit wind-strength override. Lasts 600 game ticks (30 seconds),
    // and is applied AFTER weather, windy periods, gusts, and global scaling.
    // Normal weather simulation continues underneath, ready to resume on expiry.
    private long forcedStrengthEndTick = -1L;
    private float forcedStrength = 0.0f;

    private boolean stormFrontChecked = false;
    private boolean stormFrontPlanned = false;
    private float stormFrontBoost = 0.0f;
    private float upcomingStormBoost = 0.0f;
    private float activeStormBoost = 0.0f;
    private boolean wasRaining = false;

    private float effectiveStrength = 0.0f;
    private float effectiveDirection = 0.0f;
    private float effectiveGust = 0.0f;
    private boolean effectiveWindyPeriod = false;
    private boolean effectiveStormFront = false;

    public static WindSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(WindSavedData::new, WindSavedData::load), DATA_NAME
        );
    }

    public static WindSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WindSavedData d = new WindSavedData();
        d.baseStrength = tag.getFloat("BaseStrength");
        d.targetBaseStrength = tag.getFloat("TargetBaseStrength");
        d.baseDirection = tag.getFloat("BaseDirection");
        d.targetDirection = tag.getFloat("TargetDirection");
        d.turbulence = tag.getFloat("Turbulence");
        d.targetTurbulence = tag.getFloat("TargetTurbulence");
        d.directionInstability = tag.getFloat("DirectionInstability");
        d.targetDirectionInstability = tag.getFloat("TargetDirectionInstability");
        d.phaseA = tag.getFloat("PhaseA");
        d.phaseB = tag.getFloat("PhaseB");
        d.nextTargetTick = tag.getLong("NextTargetTick");
        d.nextGustCheckTick = tag.getLong("NextGustCheckTick");
        d.gustStartTick = tag.getLong("GustStartTick");
        d.gustEndTick = tag.getLong("GustEndTick");
        d.gustMagnitude = tag.getFloat("GustMagnitude");
        d.gustDirectionOffset = tag.getFloat("GustDirectionOffset");
        // Existing worlds with an in-progress gust default to the original sine envelope.
        d.gustShape = Mth.clamp(tag.getInt("GustShape"), 0, 3);
        d.gustAttackFraction = tag.contains("GustAttackFraction")
            ? Mth.clamp(tag.getFloat("GustAttackFraction"), 0.01f, 0.85f) : 0.25f;
        d.gustReleaseFraction = tag.contains("GustReleaseFraction")
            ? Mth.clamp(tag.getFloat("GustReleaseFraction"), 0.01f, 0.85f) : 0.35f;
        d.clusterGustsRemaining = Mth.clamp(tag.getInt("ClusterGustsRemaining"), 0, 4);
        d.nextClusterGustTick = tag.contains("NextClusterGustTick") ? tag.getLong("NextClusterGustTick") : -1L;
        d.clusterEndTick = tag.contains("ClusterEndTick") ? tag.getLong("ClusterEndTick") : -1L;
        d.clusterCooldownUntilTick = tag.getLong("ClusterCooldownUntilTick");
        ListTag tails = tag.getList("OverlappingGusts", 10);
        for (int i = 0; i < Math.min(tails.size(), 5); i++) {
            CompoundTag p = tails.getCompound(i);
            long start = p.getLong("Start");
            long end = p.getLong("End");
            if (end > start) d.overlappingGusts.add(new GustPulse(start, end,
                Mth.clamp(p.getFloat("Magnitude"), 0.0f, 3.0f), p.getFloat("Direction"),
                Mth.clamp(p.getInt("Shape"), 0, 3), p.getFloat("Attack"), p.getFloat("Release")));
        }
        d.profileDay = tag.getLong("ProfileDay");
        d.dailyBaseOffset = tag.getFloat("DailyBaseOffset");
        d.nightMultiplier = tag.getFloat("NightMultiplier");
        d.dayHasWindyPeriod = tag.getBoolean("HasWindyPeriod");
        d.windyStartDayTick = tag.getInt("WindyStart");
        d.windyEndDayTick = tag.getInt("WindyEnd");
        d.windyBoost = tag.getFloat("WindyBoost");
        d.manualWindyEndTick = tag.contains("ManualWindyEndTick") ? tag.getLong("ManualWindyEndTick") : -1L;
        d.manualWindyBoost = tag.getFloat("ManualWindyBoost");
        d.forcedStrengthEndTick = tag.contains("ForcedStrengthEndTick") ? tag.getLong("ForcedStrengthEndTick") : -1L;
        d.forcedStrength = Mth.clamp(tag.getFloat("ForcedStrength"), 0.0f, 2.5f);
        d.stormFrontChecked = tag.getBoolean("StormFrontChecked");
        d.stormFrontPlanned = tag.getBoolean("StormFrontPlanned");
        d.stormFrontBoost = tag.getFloat("StormFrontBoost");
        d.upcomingStormBoost = tag.getFloat("UpcomingStormBoost");
        d.activeStormBoost = tag.getFloat("ActiveStormBoost");
        d.wasRaining = tag.getBoolean("WasRaining");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("BaseStrength", baseStrength);
        tag.putFloat("TargetBaseStrength", targetBaseStrength);
        tag.putFloat("BaseDirection", baseDirection);
        tag.putFloat("TargetDirection", targetDirection);
        tag.putFloat("Turbulence", turbulence);
        tag.putFloat("TargetTurbulence", targetTurbulence);
        tag.putFloat("DirectionInstability", directionInstability);
        tag.putFloat("TargetDirectionInstability", targetDirectionInstability);
        tag.putFloat("PhaseA", phaseA);
        tag.putFloat("PhaseB", phaseB);
        tag.putLong("NextTargetTick", nextTargetTick);
        tag.putLong("NextGustCheckTick", nextGustCheckTick);
        tag.putLong("GustStartTick", gustStartTick);
        tag.putLong("GustEndTick", gustEndTick);
        tag.putFloat("GustMagnitude", gustMagnitude);
        tag.putFloat("GustDirectionOffset", gustDirectionOffset);
        tag.putInt("GustShape", gustShape);
        tag.putFloat("GustAttackFraction", gustAttackFraction);
        tag.putFloat("GustReleaseFraction", gustReleaseFraction);
        tag.putInt("ClusterGustsRemaining", clusterGustsRemaining);
        tag.putLong("NextClusterGustTick", nextClusterGustTick);
        tag.putLong("ClusterEndTick", clusterEndTick);
        tag.putLong("ClusterCooldownUntilTick", clusterCooldownUntilTick);
        ListTag tails = new ListTag();
        for (GustPulse pulse : overlappingGusts) {
            CompoundTag p = new CompoundTag();
            p.putLong("Start", pulse.startTick());
            p.putLong("End", pulse.endTick());
            p.putFloat("Magnitude", pulse.magnitude());
            p.putFloat("Direction", pulse.directionOffset());
            p.putInt("Shape", pulse.shape());
            p.putFloat("Attack", pulse.attackFraction());
            p.putFloat("Release", pulse.releaseFraction());
            tails.add(p);
        }
        tag.put("OverlappingGusts", tails);
        tag.putLong("ProfileDay", profileDay);
        tag.putFloat("DailyBaseOffset", dailyBaseOffset);
        tag.putFloat("NightMultiplier", nightMultiplier);
        tag.putBoolean("HasWindyPeriod", dayHasWindyPeriod);
        tag.putInt("WindyStart", windyStartDayTick);
        tag.putInt("WindyEnd", windyEndDayTick);
        tag.putFloat("WindyBoost", windyBoost);
        tag.putLong("ManualWindyEndTick", manualWindyEndTick);
        tag.putFloat("ManualWindyBoost", manualWindyBoost);
        tag.putLong("ForcedStrengthEndTick", forcedStrengthEndTick);
        tag.putFloat("ForcedStrength", forcedStrength);
        tag.putBoolean("StormFrontChecked", stormFrontChecked);
        tag.putBoolean("StormFrontPlanned", stormFrontPlanned);
        tag.putFloat("StormFrontBoost", stormFrontBoost);
        tag.putFloat("UpcomingStormBoost", upcomingStormBoost);
        tag.putFloat("ActiveStormBoost", activeStormBoost);
        tag.putBoolean("WasRaining", wasRaining);
        return tag;
    }

    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        // Check expiry before any dimension/weather early return. In particular, a forced
        // command must work even when natural wind is disabled for a dimension.
        if (forcedStrengthEndTick >= 0L && now >= forcedStrengthEndTick) {
            forcedStrengthEndTick = -1L;
            setDirty();
        }
        boolean forceStrength = forcedStrengthEndTick > now;
        RandomSource random = level.getRandom();
        boolean inNether = level.dimension() == net.minecraft.world.level.Level.NETHER;
        boolean inEnd = level.dimension() == net.minecraft.world.level.Level.END;
        boolean windDisabled = inNether && WindConfig.NETHER_WIND_MODE.get() == WindConfig.NetherWindMode.DISABLED
            || inEnd && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.DISABLED;
        if (windDisabled) {
            effectiveStrength = forceStrength ? forcedStrength : 0.0f;
            effectiveGust = 0.0f;
            effectiveWindyPeriod = false;
            effectiveStormFront = false;
            return;
        }
        boolean reducedNether = inNether && WindConfig.NETHER_WIND_MODE.get() == WindConfig.NetherWindMode.REDUCED_GUSTY;
        boolean weakEnd = inEnd && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.VERY_WEAK;


        if (profileDay == Long.MIN_VALUE) {
            phaseA = random.nextFloat() * (float) (Math.PI * 2.0);
            phaseB = random.nextFloat() * (float) (Math.PI * 2.0);
            baseDirection = targetDirection = random.nextFloat() * 360.0f;
        }

        updateDailyProfile(level, random);
        updateStormProfile(level, random);
        updateStormFront(level, random);
        if (manualWindyEndTick > 0L && manualWindyEndTick <= now) {
            manualWindyEndTick = -1L;
            manualWindyBoost = 0.0f;
            setDirty();
        }
        boolean manualWindyActive = manualWindyEndTick > now;
        effectiveWindyPeriod = manualWindyActive || isWindyPeriod(level.getDayTime());
        effectiveStormFront = stormFrontPlanned && isPreStormWindow(level);

        if (now >= nextTargetTick) chooseNewTargets(level, random, now);
        // Forget expired tails only after their envelopes have reached zero.
        if (!overlappingGusts.isEmpty() && overlappingGusts.removeIf(p -> now >= p.endTick())) setDirty();
        maybeStartGust(level, random, now);

        baseStrength = WindMath.approach(baseStrength, targetBaseStrength, 0.0008f + turbulence * 0.0012f);
        turbulence = WindMath.approach(turbulence, targetTurbulence, 0.0009f);
        directionInstability = WindMath.approach(directionInstability, targetDirectionInstability, 0.0007f);
        baseDirection = WindMath.approachDegrees(baseDirection, targetDirection,
            (0.08f + directionInstability * 0.18f) * (weakEnd ? 0.07f : 1.0f));

        boolean naturalSky = supportsWeather(level);
        float nightFactor = naturalSky ? nightFactor(level.getDayTime()) : 0.0f;
        float timeMultiplier = Mth.lerp(nightFactor, 1.0f, nightMultiplier);

        float weatherBias = 0.0f;
        float variationScale = 1.0f;
        if (naturalSky && level.isRaining()) {
            weatherBias += WindConfig.RAIN_STRENGTH_BONUS.get().floatValue();
            variationScale += 0.10f;
        }
        if (naturalSky && level.isThundering()) {
            weatherBias += WindConfig.THUNDER_STRENGTH_BONUS.get().floatValue();
            variationScale += 0.18f;
        }

        // Night calm is deliberately only a bias on the ordinary background wind. Weather and
        // wind events can override it, which is how genuinely windy nights still happen.
        float backgroundWind = Math.max(0.0f, baseStrength + dailyBaseOffset) * timeMultiplier;
        float naturalWindyBoost = isWindyPeriod(level.getDayTime()) ? windyBoost : 0.0f;
        float eventBias = Math.max(naturalWindyBoost, manualWindyActive ? manualWindyBoost : 0.0f);
        if (effectiveStormFront) eventBias += stormFrontBoost;

        float livingVariation =
            (float) Math.sin(now * 0.037 + phaseA) * (0.018f + turbulence * 0.030f) +
            (float) Math.sin(now * 0.011 + phaseB) * (0.025f + turbulence * 0.045f);

        effectiveGust = weakEnd ? 0.0f : currentGust(now);
        float raw = backgroundWind + weatherBias + activeStormBoost + eventBias;
        raw += livingVariation * variationScale;
        if (reducedNether) raw *= 0.53f;
        if (weakEnd) raw *= 0.08f;
        raw += effectiveGust * (reducedNether ? 1.55f : 1.0f);
        float simulatedStrength = Mth.clamp(raw, 0.0f, WindConfig.MAX_EFFECTIVE_STRENGTH.get().floatValue());
        // A final global scale keeps every consumer on the same stronger/weaker wind model: HUD,
        // particle drift, ambient motes and future gameplay/API integrations all receive this value.
        effectiveStrength = simulatedStrength * WindConfig.GLOBAL_STRENGTH_MULTIPLIER.get().floatValue();

        float directionWander = (
            (float) Math.sin(now * 0.014 + phaseB) * (8.0f + directionInstability * 28.0f) +
            (float) Math.sin(now * 0.0047 + phaseA) * (4.0f + directionInstability * 18.0f)
        ) * (weakEnd ? 0.025f : 1.0f);
        float gustDirection = effectiveGust > 0.001f ? currentGustDirection(now) : 0.0f;
        effectiveDirection = WindMath.normalizeDegrees(baseDirection + directionWander + gustDirection);

        // Command strength takes final precedence, including over gust clusters, windy
        // periods, rain/thunder, storm fronts, nighttime and GLOBAL_STRENGTH_MULTIPLIER.
        // Do not report a gust contribution that would imply extra force while locked.
        if (forceStrength) {
            effectiveStrength = forcedStrength;
            effectiveGust = 0.0f;
        }
    }

    /**
     * Force the FINAL effective wind strength for exactly 600 game ticks (30 seconds),
     * irrespective of natural gusts, windy periods, weather and config multipliers.
     * Repeating the command replaces the strength and restarts the countdown.
     */
    public void commandPushStrength(float value, long now) {
        float clamped = Mth.clamp(value, 0.0f, 2.5f);
        forcedStrength = clamped;
        forcedStrengthEndTick = now + 600L;
        // Retain the requested value in the background too, preventing an abrupt snap
        // back to the pre-command base wind when the override expires.
        baseStrength = clamped;
        targetBaseStrength = clamped;
        nextTargetTick = now + 600L;
        setDirty();
    }

    /** Push direction immediately, then hand control back to natural retargeting after ~30 seconds. */
    public void commandPushDirection(float degrees, long now) {
        float normalized = WindMath.normalizeDegrees(degrees);
        baseDirection = normalized;
        targetDirection = normalized;
        nextTargetTick = Math.max(nextTargetTick, now + 600L);
        setDirty();
    }

    /** Start a finite test gust. The normal gust scheduler resumes after this gust has ended. */
    public void commandStartGust(float magnitude, int durationTicks, long now) {
        cancelCluster();
        overlappingGusts.clear();
        gustStartTick = now;
        gustEndTick = now + Math.max(20, durationTicks);
        gustMagnitude = Mth.clamp(magnitude, 0.0f, 2.0f);
        gustDirectionOffset = 0.0f;
        // Explicit test commands retain the predictable original rise/fall profile.
        gustShape = 0;
        gustAttackFraction = 0.25f;
        gustReleaseFraction = 0.35f;
        nextGustCheckTick = gustEndTick + 100L;
        setDirty();
    }

    /** Overlay a temporary windy period without disabling the natural daily wind model. */
    public void commandStartWindyPeriod(float boost, int durationTicks, long now) {
        manualWindyBoost = Mth.clamp(boost, 0.0f, 2.0f);
        manualWindyEndTick = now + Math.max(20, durationTicks);
        setDirty();
    }

    /** Stop both the command-driven windy period and any currently scheduled natural one for today. */
    public void commandStopWindyPeriod() {
        manualWindyEndTick = -1L;
        manualWindyBoost = 0.0f;
        dayHasWindyPeriod = false;
        windyBoost = 0.0f;
        effectiveWindyPeriod = false;
        setDirty();
    }

    /** Clear test gust/event state and ask the natural system for a fresh target on the next tick. */
    public void commandReturnToNatural(long now) {
        forcedStrengthEndTick = -1L;
        manualWindyEndTick = -1L;
        manualWindyBoost = 0.0f;
        cancelCluster();
        overlappingGusts.clear();
        gustStartTick = gustEndTick = -1L;
        gustMagnitude = 0.0f;
        gustDirectionOffset = 0.0f;
        gustShape = 0;
        gustAttackFraction = 0.25f;
        gustReleaseFraction = 0.35f;
        profileDay = Long.MIN_VALUE;
        nextTargetTick = now;
        nextGustCheckTick = now + 100L;
        setDirty();
    }

    public float commandBaseStrength() { return baseStrength; }
    public float commandTargetBaseStrength() { return targetBaseStrength; }
    public float commandBaseDirection() { return baseDirection; }
    public long commandManualWindyTicksRemaining(long now) { return Math.max(0L, manualWindyEndTick - now); }
    public long commandForcedStrengthTicksRemaining(long now) { return Math.max(0L, forcedStrengthEndTick - now); }
    public float commandForcedStrength() { return forcedStrength; }

    private void chooseNewTargets(ServerLevel level, RandomSource random, long now) {
        float min = WindConfig.MIN_BASE_STRENGTH.get().floatValue();
        float max = Math.max(min, WindConfig.MAX_BASE_STRENGTH.get().floatValue());
        float centeredRandom = (random.nextFloat() + random.nextFloat()) * 0.5f;
        targetBaseStrength = Mth.lerp(centeredRandom, min, max);

        float desiredTurbulence = 0.16f + random.nextFloat() * 0.34f;
        if (supportsWeather(level) && level.isRaining()) desiredTurbulence += 0.12f;
        if (supportsWeather(level) && level.isThundering()) desiredTurbulence += 0.18f;
        if (effectiveWindyPeriod) desiredTurbulence += 0.10f;
        if (effectiveStormFront) desiredTurbulence += 0.12f;
        targetTurbulence = Mth.clamp(desiredTurbulence, 0.05f, 0.95f);

        targetDirectionInstability = Mth.clamp(
            0.12f + random.nextFloat() * 0.42f + targetTurbulence * 0.16f, 0.05f, 0.90f
        );

        if (level.dimension() == net.minecraft.world.level.Level.END
            && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.VERY_WEAK) {
            targetTurbulence = Math.min(targetTurbulence, 0.10f);
            targetDirectionInstability = Math.min(targetDirectionInstability, 0.08f);
        }
        float maxTurn = 18.0f + targetDirectionInstability * 85.0f;
        float delta = (random.nextFloat() - random.nextFloat()) * maxTurn;
        if (level.dimension() == net.minecraft.world.level.Level.END
            && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.VERY_WEAK) delta *= 0.10f;
        targetDirection = (!(level.dimension() == net.minecraft.world.level.Level.END
            && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.VERY_WEAK)
            && random.nextFloat() < 0.025f)
            ? random.nextFloat() * 360.0f
            : WindMath.normalizeDegrees(targetDirection + delta);

        int delay = 260 + random.nextInt(501);
        delay = Math.max(120, (int) (delay * (1.10f - targetTurbulence * 0.45f)));
        nextTargetTick = now + delay;
        setDirty();
    }

    private void maybeStartGust(ServerLevel level, RandomSource random, long now) {
        if (level.dimension() == net.minecraft.world.level.Level.END
            && WindConfig.END_WIND_MODE.get() == WindConfig.EndWindMode.VERY_WEAK) return;

        // A cluster schedules its followers independently of the usual natural gust check.
        // They may start before the previous gust fades away, but the normal scheduler is
        // otherwise left untouched.
        if (clusterGustsRemaining > 0) {
            if (!WindConfig.GUST_CLUSTERS_ENABLED.get()) {
                cancelCluster();
                setDirty();
                return;
            }
            if (now >= nextClusterGustTick) {
                startNaturalGust(level, random, now);
                clusterGustsRemaining--;
                if (clusterGustsRemaining > 0) {
                    scheduleClusterFollower(random, now);
                } else {
                    clusterEndTick = -1L;
                    nextClusterGustTick = -1L;
                    // 2-4 minutes *after the last cluster gust*, not the first.
                    clusterCooldownUntilTick = now + 2400L + random.nextInt(2401);
                    nextGustCheckTick = now + 100 + random.nextInt(181);
                }
                setDirty();
            }
            return;
        }
        if (gustEndTick > now || now < nextGustCheckTick) return;

        float chance = 0.07f + turbulence * 0.16f;
        if (supportsWeather(level) && level.isRaining()) chance += 0.09f;
        if (supportsWeather(level) && level.isThundering()) chance += 0.16f;
        if (effectiveWindyPeriod) chance += 0.12f;
        if (effectiveStormFront) chance += 0.12f;
        if (level.dimension() == net.minecraft.world.level.Level.NETHER
            && WindConfig.NETHER_WIND_MODE.get() == WindConfig.NetherWindMode.REDUCED_GUSTY) chance += 0.13f;

        if (random.nextFloat() < chance) {
            startNaturalGust(level, random, now);
            if (WindConfig.GUST_CLUSTERS_ENABLED.get() && now >= clusterCooldownUntilTick) {
                float clusterChance;
                if (supportsWeather(level) && level.isThundering()) clusterChance = 0.45f;
                else if (supportsWeather(level) && level.isRaining()) clusterChance = 0.35f;
                else if (effectiveWindyPeriod) clusterChance = 0.30f;
                else clusterChance = baseStrength + dailyBaseOffset < 0.16f ? 0.10f : 0.20f;
                if (effectiveStormFront) clusterChance += 0.08f;
                clusterChance = Math.min(0.95f, clusterChance * WindConfig.GUST_CLUSTER_CHANCE_MULTIPLIER.get().floatValue());
                if (random.nextFloat() < clusterChance) {
                    clusterGustsRemaining = 1 + random.nextInt(4); // total 2-5 gusts
                    clusterEndTick = now + 400L + random.nextInt(501); // 20-45 seconds
                    scheduleClusterFollower(random, now);
                }
            }
        }
        nextGustCheckTick = now + 100 + random.nextInt(181);
        setDirty();
    }

    private void scheduleClusterFollower(RandomSource random, long now) {
        // Reserve room for the remaining followers, so all of them start within the
        // 20-45-second event window. Individual gaps are randomized from 3 to 8 seconds.
        long available = Math.max(60L, clusterEndTick - now);
        int maximumGap = (int) Math.min(160L,
            Math.max(60L, available - (clusterGustsRemaining - 1L) * 60L));
        nextClusterGustTick = now + 60 + random.nextInt(maximumGap - 60 + 1);
    }

    private void cancelCluster() {
        clusterGustsRemaining = 0;
        nextClusterGustTick = -1L;
        clusterEndTick = -1L;
    }

    private void startNaturalGust(ServerLevel level, RandomSource random, long now) {
        // Move the still-active gust into a bounded tail list before replacing the head.
        // At most four natural pulses can overlap with the 3-second minimum gap.
        if (gustEndTick > now) {
            overlappingGusts.add(new GustPulse(gustStartTick, gustEndTick, gustMagnitude,
                gustDirectionOffset, gustShape, gustAttackFraction, gustReleaseFraction));
        }
        int duration = 90 + random.nextInt(131);
        float gustRoll = random.nextFloat();
        float magnitude = 0.10f + 0.40f * (float) Math.pow(gustRoll, 1.35f - turbulence * 0.50f);
        if (supportsWeather(level) && level.isThundering()) magnitude += random.nextFloat() * 0.18f;
        if (random.nextFloat() < 0.035f) magnitude *= 1.60f;
        gustStartTick = now;
        gustEndTick = now + duration;
        gustMagnitude = magnitude;
        gustDirectionOffset = (random.nextFloat() - random.nextFloat()) * (12.0f + directionInstability * 32.0f);

        float shapeRoll = random.nextFloat();
        if (shapeRoll < 0.40f) {
            gustShape = 0;
        } else if (shapeRoll < 0.80f) {
            gustShape = 1;
            gustAttackFraction = 0.02f + random.nextFloat() * 0.055f;
            gustReleaseFraction = 0.30f + random.nextFloat() * 0.30f;
        } else if (shapeRoll < 0.95f) {
            gustShape = 2;
            gustAttackFraction = 0.30f + random.nextFloat() * 0.30f;
            gustReleaseFraction = 0.05f + random.nextFloat() * 0.10f;
        } else {
            gustShape = 3;
            gustAttackFraction = 0.02f + random.nextFloat() * 0.06f;
            gustReleaseFraction = 0.04f + random.nextFloat() * 0.10f;
        }
    }

    private float gustEnvelope(long now) {
        return GustPulse.envelope(now, gustStartTick, gustEndTick,
            gustShape, gustAttackFraction, gustReleaseFraction);
    }

    private float currentGust(long now) {
        float head = gustMagnitude * gustEnvelope(now);
        float strongest = head;
        float total = head;
        for (GustPulse pulse : overlappingGusts) {
            float contribution = pulse.contribution(now);
            strongest = Math.max(strongest, contribution);
            total += contribution;
        }
        return GustPulse.blend(strongest, total);
    }

    private float currentGustDirection(long now) {
        float headEnvelope = gustEnvelope(now);
        float headContribution = gustMagnitude * headEnvelope;
        float weightedDirection = headContribution * gustDirectionOffset * headEnvelope;
        float total = headContribution;
        for (GustPulse pulse : overlappingGusts) {
            float envelope = pulse.envelope(now);
            float contribution = pulse.magnitude() * envelope;
            weightedDirection += contribution * pulse.directionOffset() * envelope;
            total += contribution;
        }
        return total > 0.00001f ? weightedDirection / total : 0.0f;
    }

    private void updateDailyProfile(ServerLevel level, RandomSource random) {
        long day = Math.floorDiv(level.getDayTime(), 24000L);
        if (day == profileDay) return;
        profileDay = day;

        // A shared daily atmosphere makes sunny days feel distinct rather than constantly
        // returning to the same gentle middle range. Calm and breezy profiles are exclusive.
        // They affect ordinary background wind only; thunderstorms, commands and gusts still work.
        dailyBaseOffset = (random.nextFloat() - random.nextFloat()) * 0.045f;
        float profileRoll = random.nextFloat();
        float calmChance = WindConfig.CALM_DAY_CHANCE.get().floatValue();
        float breezyChance = WindConfig.BREEZY_DAY_CHANCE.get().floatValue();
        boolean calmDay = profileRoll < calmChance;
        if (calmDay) {
            dailyBaseOffset -= 0.17f + random.nextFloat() * 0.16f;
        } else if (profileRoll < Math.min(1.0f, calmChance + breezyChance)) {
            dailyBaseOffset += 0.30f + random.nextFloat() * 0.10f;
        } else if (random.nextFloat() < 0.12f) {
            // Keep the occasional mildly windier day from the original simulation.
            dailyBaseOffset += 0.08f + random.nextFloat() * 0.13f;
        }

        float nightRoll = random.nextFloat();
        if (nightRoll < 0.72f) {
            // Most nights are calmer.
            nightMultiplier = 0.50f + random.nextFloat() * 0.32f;
        } else if (nightRoll < 0.94f) {
            // Some nights remain normal or slightly windy.
            nightMultiplier = 0.86f + random.nextFloat() * 0.30f;
        } else {
            // Rare exception: the night itself is unusually windy, even without a storm.
            nightMultiplier = 1.20f + random.nextFloat() * 0.45f;
        }

        // Scheduled high-wind spells cannot overwrite a naturally calm day's identity.
        // A user-triggered windy spell and incoming weather can still override it.
        dayHasWindyPeriod = !calmDay && random.nextFloat() < WindConfig.WINDY_PERIOD_CHANCE.get();
        if (dayHasWindyPeriod) {
            int minDuration = WindConfig.WINDY_PERIOD_MIN_TICKS.get();
            int maxDuration = Math.max(minDuration, WindConfig.WINDY_PERIOD_MAX_TICKS.get());
            int duration = minDuration + random.nextInt(maxDuration - minDuration + 1);
            // Fit the full scheduled duration into the current profile day. Previously a
            // period crossing midnight was cut short when the next day's profile replaced it.
            duration = Math.min(duration, 24000);
            windyStartDayTick = random.nextInt(24000 - duration + 1);
            windyEndDayTick = windyStartDayTick + duration;
            windyBoost = 0.28f + random.nextFloat() * 0.20f;
            if (random.nextFloat() < 0.09f) windyBoost += 0.15f + random.nextFloat() * 0.16f;
        } else {
            windyStartDayTick = windyEndDayTick = 0;
            windyBoost = 0.0f;
        }
        setDirty();
    }

    private boolean isWindyPeriod(long dayTime) {
        if (!dayHasWindyPeriod) return false;
        int t = (int) Math.floorMod(dayTime, 24000L);
        // v0.2.5 schedules complete events within their originating Minecraft day.
        // Accept the old wrapped representation when loading pre-update world data.
        if (windyStartDayTick <= windyEndDayTick) return t >= windyStartDayTick && t < windyEndDayTick;
        return t >= windyStartDayTick || t < windyEndDayTick;
    }

    private void updateStormProfile(ServerLevel level, RandomSource random) {
        if (!supportsWeather(level)) {
            if (activeStormBoost != 0.0f || stormFrontChecked || stormFrontPlanned || upcomingStormBoost != 0.0f) {
                activeStormBoost = 0.0f;
                clearUpcomingStormPlan();
            }
            wasRaining = false;
            return;
        }

        boolean raining = level.isRaining();
        if (raining && !wasRaining) {
            // If a front was predicted, its windiness continues into the actual rain. Worlds loaded
            // in the middle of rain still get a modest randomized storm profile instead of zero.
            if (upcomingStormBoost > 0.0f) {
                activeStormBoost = upcomingStormBoost;
            } else {
                activeStormBoost = random.nextFloat() < 0.35f ? 0.04f + random.nextFloat() * 0.10f : 0.0f;
            }
            stormFrontChecked = false;
            stormFrontPlanned = false;
            stormFrontBoost = 0.0f;
            upcomingStormBoost = 0.0f;
            setDirty();
        } else if (!raining && wasRaining) {
            activeStormBoost = 0.0f;
            stormFrontChecked = false;
            stormFrontPlanned = false;
            stormFrontBoost = 0.0f;
            upcomingStormBoost = 0.0f;
            setDirty();
        }
        wasRaining = raining;
    }

    private void updateStormFront(ServerLevel level, RandomSource random) {
        if (!supportsWeather(level) || !level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
            clearUpcomingStormPlan();
            return;
        }

        ServerLevelData weather = (ServerLevelData) level.getLevelData();
        int rainTime = weather.getRainTime();
        int lead = WindConfig.PRE_STORM_LEAD_TICKS.get();

        if (level.isRaining()) return;

        // Vanilla's clear-weather timer can intentionally suppress the normal rain countdown.
        // Do not advertise a front while that override is active.
        if (weather.getClearWeatherTime() > 0) {
            clearUpcomingStormPlan();
            return;
        }

        // Well outside the forecast window, allow the next approaching rain transition to roll a
        // fresh profile. rainTime counts down to the next vanilla rain-state toggle while clear.
        if (rainTime <= 0 || rainTime > lead + 600) {
            if (rainTime > lead + 600) clearUpcomingStormPlan();
            return;
        }

        if (!stormFrontChecked && rainTime <= lead) {
            stormFrontChecked = true;
            stormFrontPlanned = random.nextFloat() < WindConfig.PRE_STORM_FRONT_CHANCE.get();
            stormFrontBoost = 0.0f;
            upcomingStormBoost = 0.0f;

            if (stormFrontPlanned) {
                // The same roll drives both the pre-rain buildup and the extra wind during the rain,
                // making a windy front a genuine warning of a windier-than-normal storm.
                upcomingStormBoost = 0.08f + random.nextFloat() * 0.22f;
                if (random.nextFloat() < 0.12f) upcomingStormBoost += 0.12f + random.nextFloat() * 0.20f;

                int thunderTime = weather.getThunderTime();
                if (thunderTime > 0 && thunderTime <= lead) {
                    upcomingStormBoost += 0.06f + random.nextFloat() * 0.10f;
                }

                stormFrontBoost = upcomingStormBoost * (0.55f + random.nextFloat() * 0.30f);
            }
            setDirty();
        }
    }

    private void clearUpcomingStormPlan() {
        if (!stormFrontChecked && !stormFrontPlanned && stormFrontBoost == 0.0f && upcomingStormBoost == 0.0f) return;
        stormFrontChecked = false;
        stormFrontPlanned = false;
        stormFrontBoost = 0.0f;
        upcomingStormBoost = 0.0f;
        setDirty();
    }

    private boolean isPreStormWindow(ServerLevel level) {
        if (!supportsWeather(level) || !level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) return false;
        if (level.isRaining()) return false;
        ServerLevelData weather = (ServerLevelData) level.getLevelData();
        int rainTime = weather.getRainTime();
        return weather.getClearWeatherTime() <= 0 && rainTime > 0 && rainTime <= WindConfig.PRE_STORM_LEAD_TICKS.get();
    }

    private static boolean supportsWeather(ServerLevel level) {
        // The End has skylight but no weather. Keep the Nether and End independent of
        // any rain/thunder timers inherited from the dimension's level data.
        return level.dimension() != net.minecraft.world.level.Level.NETHER
            && level.dimension() != net.minecraft.world.level.Level.END
            && level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }

    private static float nightFactor(long dayTime) {
        int t = (int) Math.floorMod(dayTime, 24000L);
        if (t < 12000) return 0.0f;
        if (t < 14000) return (t - 12000) / 2000.0f;
        if (t < 22000) return 1.0f;
        return 1.0f - (t - 22000) / 2000.0f;
    }

    public WindSnapshot snapshot(long gameTime) {
        return new WindSnapshot(
            effectiveStrength,
            effectiveDirection,
            Mth.clamp(turbulence, 0.0f, 1.0f),
            Mth.clamp(directionInstability, 0.0f, 1.0f),
            effectiveGust,
            effectiveWindyPeriod,
            effectiveStormFront,
            gameTime
        );
    }
}
