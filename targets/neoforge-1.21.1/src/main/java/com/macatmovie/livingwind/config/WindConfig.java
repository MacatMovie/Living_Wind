package com.macatmovie.livingwind.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WindConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MIN_BASE_STRENGTH;
    public static final ModConfigSpec.DoubleValue MAX_BASE_STRENGTH;
    public static final ModConfigSpec.DoubleValue MAX_EFFECTIVE_STRENGTH;
    public static final ModConfigSpec.DoubleValue GLOBAL_STRENGTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RAIN_STRENGTH_BONUS;
    public static final ModConfigSpec.DoubleValue THUNDER_STRENGTH_BONUS;
    public static final ModConfigSpec.DoubleValue CALM_DAY_CHANCE;
    public static final ModConfigSpec.DoubleValue BREEZY_DAY_CHANCE;
    public static final ModConfigSpec.DoubleValue WINDY_PERIOD_CHANCE;
    public static final ModConfigSpec.BooleanValue GUST_CLUSTERS_ENABLED;
    public static final ModConfigSpec.DoubleValue GUST_CLUSTER_CHANCE_MULTIPLIER;
    public static final ModConfigSpec.IntValue WINDY_PERIOD_MIN_TICKS;
    public static final ModConfigSpec.IntValue WINDY_PERIOD_MAX_TICKS;
    public static final ModConfigSpec.DoubleValue PRE_STORM_FRONT_CHANCE;
    public static final ModConfigSpec.IntValue PRE_STORM_LEAD_TICKS;
    public static final ModConfigSpec.IntValue SYNC_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue CREATE_WINDMILLS_ENABLED;
    public static final ModConfigSpec.DoubleValue CREATE_WINDMILL_MIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CREATE_WINDMILL_MAX_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CREATE_WINDMILL_RPM_STEP;
    public static final ModConfigSpec.DoubleValue CREATE_WINDMILL_MIN_RPM_CHANGE;
    public static final ModConfigSpec.DoubleValue CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE;
    public static final ModConfigSpec.IntValue CREATE_WINDMILL_UPDATE_TICKS;

    public enum NetherWindMode { DISABLED, REDUCED_GUSTY, NORMAL }
    public enum EndWindMode { DISABLED, VERY_WEAK, NORMAL }
    public static final ModConfigSpec.EnumValue<NetherWindMode> NETHER_WIND_MODE;
    public static final ModConfigSpec.EnumValue<EndWindMode> END_WIND_MODE;
    public static final ModConfigSpec.BooleanValue ALTITUDE_SCALING_ENABLED;
    public static final ModConfigSpec.IntValue ALTITUDE_MIN_Y;
    public static final ModConfigSpec.IntValue ALTITUDE_MAX_Y;
    public static final ModConfigSpec.DoubleValue ALTITUDE_MIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ALTITUDE_MAX_MULTIPLIER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Core synchronized wind simulation values.")
            .translation("livingwind.config.section.wind")
            .push("wind");
        MIN_BASE_STRENGTH = b.comment("Lowest ordinary clear-weather base wind target.")
            .translation("livingwind.config.minimumBaseStrength")
            .defineInRange("minimumBaseStrength", 0.04, 0.0, 1.5);
        MAX_BASE_STRENGTH = b.comment("Highest ordinary clear-weather base wind target before events/weather.")
            .translation("livingwind.config.maximumBaseStrength")
            .defineInRange("maximumBaseStrength", 0.48, 0.0, 1.5);
        MAX_EFFECTIVE_STRENGTH = b.comment("Simulation cap after weather, events and gusts are combined, before the global multiplier.")
            .translation("livingwind.config.maximumEffectiveStrength")
            .defineInRange("maximumEffectiveStrength", 1.60, 0.25, 2.5);
        GLOBAL_STRENGTH_MULTIPLIER = b.comment(
                "Final multiplier applied to the whole synchronized wind result.",
                "1.30 makes every wind state, including extreme storms and gusts, 30% stronger."
            )
            .translation("livingwind.config.globalStrengthMultiplier")
            .defineInRange("globalStrengthMultiplier", 1.30, 0.25, 2.5);
        b.pop();

        b.comment("How vanilla rain and thunder influence Living Wind.")
            .translation("livingwind.config.section.weather")
            .push("weather");
        RAIN_STRENGTH_BONUS = b.comment("Bias added while raining. Turbulence and gusts remain irregular.")
            .translation("livingwind.config.rainStrengthBonus")
            .defineInRange("rainStrengthBonus", 0.24, 0.0, 1.5);
        THUNDER_STRENGTH_BONUS = b.comment("Additional bias while thundering.")
            .translation("livingwind.config.thunderStrengthBonus")
            .defineInRange("thunderStrengthBonus", 0.44, 0.0, 1.5);
        PRE_STORM_FRONT_CHANCE = b.comment("Chance an approaching scheduled rain transition creates a pre-storm wind front.")
            .translation("livingwind.config.preStormFrontChance")
            .defineInRange("preStormFrontChance", 0.58, 0.0, 1.0);
        PRE_STORM_LEAD_TICKS = b.comment("How early a pre-storm front may begin. 1000 ticks is one Minecraft hour.")
            .translation("livingwind.config.preStormLeadTicks")
            .defineInRange("preStormLeadTicks", 2400, 200, 12000);
        b.pop();

        b.comment("Occasional naturally windy periods independent of storms.")
            .translation("livingwind.config.section.events")
            .push("events");
        CALM_DAY_CHANCE = b.comment(
                "Chance each day of a genuinely calm atmospheric profile.",
                "Calm days do not also roll a naturally windy period. Storms and commands can still bring wind."
            )
            .translation("livingwind.config.calmDayChance")
            .defineInRange("calmDayChance", 0.09, 0.0, 1.0);
        BREEZY_DAY_CHANCE = b.comment(
                "Chance each day of a sustained breezy atmospheric profile (after the calm-day roll).",
                "Calm and breezy shares are mutually exclusive; their total should not exceed 1.0."
            )
            .translation("livingwind.config.breezyDayChance")
            .defineInRange("breezyDayChance", 0.26, 0.0, 1.0);
        WINDY_PERIOD_CHANCE = b.comment("Chance per Minecraft day to schedule a naturally windy period.")
            .translation("livingwind.config.windyPeriodChance")
            .defineInRange("windyPeriodChance", 0.55, 0.0, 1.0);
        WINDY_PERIOD_MIN_TICKS = b.comment("Minimum duration of a naturally windy period in ticks.")
            .translation("livingwind.config.windyPeriodMinTicks")
            .defineInRange("windyPeriodMinTicks", 3000, 200, 24000);
        WINDY_PERIOD_MAX_TICKS = b.comment("Maximum duration of a naturally windy period in ticks.")
            .translation("livingwind.config.windyPeriodMaxTicks")
            .defineInRange("windyPeriodMaxTicks", 7200, 200, 24000);
        GUST_CLUSTERS_ENABLED = b.comment("Allow occasional clusters of 2-5 back-to-back natural gusts; does not change normal gust scheduling.")
            .translation("livingwind.config.gustClustersEnabled")
            .define("gustClustersEnabled", true);
        GUST_CLUSTER_CHANCE_MULTIPLIER = b.comment("Multiplier for the chance that a natural gust starts a cluster. 0 disables new clusters, 1 is the default.")
            .translation("livingwind.config.gustClusterChanceMultiplier")
            .defineInRange("gustClusterChanceMultiplier", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Optional Overworld altitude scaling. Higher exposed terrain can experience stronger wind while the shared atmospheric direction/state remains the same.")
            .translation("livingwind.config.section.altitude")
            .push("altitude");
        ALTITUDE_SCALING_ENABLED = b.comment("Enable stronger local wind at higher Y levels in the Overworld.")
            .translation("livingwind.config.altitudeScalingEnabled")
            .define("enabled", true);
        ALTITUDE_MIN_Y = b.comment("Y level where the minimum altitude multiplier applies. Below this height the minimum multiplier is retained.")
            .translation("livingwind.config.altitudeMinY")
            .defineInRange("minimumY", 64, -64, 320);
        ALTITUDE_MAX_Y = b.comment("Y level where the maximum altitude multiplier is reached. Above it the maximum multiplier is retained.")
            .translation("livingwind.config.altitudeMaxY")
            .defineInRange("maximumY", 192, -64, 512);
        ALTITUDE_MIN_MULTIPLIER = b.comment("Wind multiplier at/below minimumY.")
            .translation("livingwind.config.altitudeMinMultiplier")
            .defineInRange("minimumMultiplier", 1.0, 0.0, 3.0);
        ALTITUDE_MAX_MULTIPLIER = b.comment("Wind multiplier at/above maximumY.")
            .translation("livingwind.config.altitudeMaxMultiplier")
            .defineInRange("maximumMultiplier", 1.35, 0.0, 3.0);
        b.pop();

        b.comment("Dimension-specific simulation. Weather bonuses are never applied in the Nether or End.")
            .translation("livingwind.config.section.dimensions")
            .push("dimensions");
        NETHER_WIND_MODE = b.comment("DISABLED = no wind; REDUCED_GUSTY = weaker background, more pronounced gusts; NORMAL = unmodified simulation.")
            .translation("livingwind.config.netherWindMode")
            .defineEnum("netherWindMode", NetherWindMode.REDUCED_GUSTY);
        END_WIND_MODE = b.comment("DISABLED = no wind; VERY_WEAK = faint air with very gradual directional shifts; NORMAL = unmodified simulation.")
            .translation("livingwind.config.endWindMode")
            .defineEnum("endWindMode", EndWindMode.DISABLED);
        b.pop();

        b.comment("Optional Create windmill bearing compatibility. Uses real kinetic RPM and stress capacity, not visual-only animation.")
            .translation("livingwind.config.section.createWindmills")
            .push("createWindmills");
        CREATE_WINDMILLS_ENABLED = b.comment("Scale Create windmill bearing RPM with Living Wind. Both mods must be installed on the server; Create is optional.")
            .translation("livingwind.config.createWindmillsEnabled")
            .define("enabled", true);
        CREATE_WINDMILL_MIN_MULTIPLIER = b.comment("Minimum speed multiplier for very weak winds. At exactly zero wind, assembled bearings instead run at an absolute 0.1 RPM.")
            .translation("livingwind.config.createWindmillMinMultiplier")
            .defineInRange("minimumMultiplier", 0.05, 0.01, 2.0);
        CREATE_WINDMILL_MAX_MULTIPLIER = b.comment("Generated RPM multiplier at Living Wind strength 2.0 or higher. Affects kinetic stress capacity.")
            .translation("livingwind.config.createWindmillMaxMultiplier")
            .defineInRange("maximumMultiplier", 2.50, 0.50, 4.0);
        CREATE_WINDMILL_RPM_STEP = b.comment("Target RPM quantization step, preventing minor wind fluctuations from rebuilding Create kinetic networks.")
            .translation("livingwind.config.createWindmillRpmStep")
            .defineInRange("rpmStep", 1.0, 0.25, 4.0);
        CREATE_WINDMILL_MIN_RPM_CHANGE = b.comment("Minimum RPM difference required to refresh a kinetic network.")
            .translation("livingwind.config.createWindmillMinRpmChange")
            .defineInRange("minimumRpmChange", 1.0, 0.25, 8.0);
        CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE = b.comment("Maximum RPM increase/decrease in one refresh. Prevents instantaneous huge speed jumps.")
            .translation("livingwind.config.createWindmillMaxRpmChange")
            .defineInRange("maximumChangePerUpdate", 6.0, 0.5, 32.0);
        CREATE_WINDMILL_UPDATE_TICKS = b.comment("Minimum tick interval between kinetic network speed checks per bearing. 40 ticks = 2 seconds.")
            .translation("livingwind.config.createWindmillUpdateTicks")
            .defineInRange("updateTicks", 40, 20, 200);
        b.pop();

        b.comment("Low-frequency server-to-client wind synchronization.")
            .translation("livingwind.config.section.network")
            .push("network");
        SYNC_INTERVAL_TICKS = b.comment("Small wind snapshot interval. 40 ticks is every 2 seconds.")
            .translation("livingwind.config.syncIntervalTicks")
            .defineInRange("syncIntervalTicks", 40, 10, 200);
        b.pop();
        SPEC = b.build();
    }

    private WindConfig() {}
}
