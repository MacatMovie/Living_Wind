package com.macatmovie.livingwind.config;

import java.util.List;
import com.macatmovie.livingwind.wind.WindDimensionSettings;
import net.minecraft.resources.ResourceLocation;

import com.macatmovie.livingwind.fabric.config.FabricConfigSpec;

public final class WindConfig {
    public static final FabricConfigSpec SPEC;
    public static final FabricConfigSpec.DoubleValue MIN_BASE_STRENGTH;
    public static final FabricConfigSpec.DoubleValue MAX_BASE_STRENGTH;
    public static final FabricConfigSpec.DoubleValue MAX_EFFECTIVE_STRENGTH;
    public static final FabricConfigSpec.DoubleValue GLOBAL_STRENGTH_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue RAIN_STRENGTH_BONUS;
    public static final FabricConfigSpec.DoubleValue THUNDER_STRENGTH_BONUS;
    public static final FabricConfigSpec.BooleanValue WEATHER_STRENGTH_RANGES_ENABLED;
    public static final FabricConfigSpec.DoubleValue CLEAR_WEATHER_MIN_STRENGTH;
    public static final FabricConfigSpec.DoubleValue CLEAR_WEATHER_MAX_STRENGTH;
    public static final FabricConfigSpec.DoubleValue RAIN_WEATHER_MIN_STRENGTH;
    public static final FabricConfigSpec.DoubleValue RAIN_WEATHER_MAX_STRENGTH;
    public static final FabricConfigSpec.DoubleValue THUNDER_WEATHER_MIN_STRENGTH;
    public static final FabricConfigSpec.DoubleValue THUNDER_WEATHER_MAX_STRENGTH;
    public static final FabricConfigSpec.DoubleValue WEATHER_STRENGTH_FLUCTUATION;
    public static final FabricConfigSpec.DoubleValue CALM_DAY_CHANCE;
    public static final FabricConfigSpec.DoubleValue BREEZY_DAY_CHANCE;
    public static final FabricConfigSpec.DoubleValue WINDY_PERIOD_CHANCE;
    public static final FabricConfigSpec.BooleanValue GUST_CLUSTERS_ENABLED;
    public static final FabricConfigSpec.DoubleValue GUST_CLUSTER_CHANCE_MULTIPLIER;
    public static final FabricConfigSpec.IntValue WINDY_PERIOD_MIN_TICKS;
    public static final FabricConfigSpec.IntValue WINDY_PERIOD_MAX_TICKS;
    public static final FabricConfigSpec.DoubleValue PRE_STORM_FRONT_CHANCE;
    public static final FabricConfigSpec.IntValue PRE_STORM_LEAD_TICKS;
    public static final FabricConfigSpec.IntValue SYNC_INTERVAL_TICKS;
    public static final FabricConfigSpec.BooleanValue CREATE_WINDMILLS_ENABLED;
    public static final FabricConfigSpec.DoubleValue CREATE_WINDMILL_MIN_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue CREATE_WINDMILL_MAX_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue CREATE_WINDMILL_RPM_STEP;
    public static final FabricConfigSpec.DoubleValue CREATE_WINDMILL_MIN_RPM_CHANGE;
    public static final FabricConfigSpec.DoubleValue CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE;
    public static final FabricConfigSpec.IntValue CREATE_WINDMILL_UPDATE_TICKS;
    public static final FabricConfigSpec.BooleanValue SMALL_SHIPS_WIND_ENABLED;
    public static final FabricConfigSpec.BooleanValue TREASURE_BALLOONS_WIND_ENABLED;
    public static final FabricConfigSpec.DoubleValue TREASURE_BALLOONS_WIND_MULTIPLIER;

    public static final FabricConfigSpec.BooleanValue AERONAUTICS_WIND_ENABLED;
    public static final FabricConfigSpec.DoubleValue AERONAUTICS_WIND_MULTIPLIER;
    public static final FabricConfigSpec.BooleanValue AERONAUTICS_KINETIC_BEARING_ENABLED;
    public static final FabricConfigSpec.BooleanValue AERONAUTICS_KINETIC_BEARING_DEBUG;
    public static final FabricConfigSpec.DoubleValue AERONAUTICS_KINETIC_BEARING_MULTIPLIER;
    public static final FabricConfigSpec.BooleanValue HIGH_SEAS_WIND_ENABLED;

    public static final FabricConfigSpec.BooleanValue GLIDER_WIND_ENABLED;
    public static final FabricConfigSpec.DoubleValue GLIDER_WIND_INFLUENCE;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_ENABLED;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_CHECK_SHELTER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_CLEAR_WEATHER_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_RAIN_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_THUNDER_MULTIPLIER;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_PLAYERS;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_MOBS;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_PROJECTILES;
    public static final FabricConfigSpec.ConfigValue<List<? extends String>> ENTITY_WIND_PROJECTILE_BLACKLIST;
    public static final FabricConfigSpec.ConfigValue<List<? extends String>> ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_BOATS;
    public static final FabricConfigSpec.BooleanValue ENTITY_WIND_DROPPED_ITEMS;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_PLAYER_GROUNDED_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_PLAYER_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_MOB_GROUNDED_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_MOB_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_PROJECTILE_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_BOAT_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ENTITY_WIND_DROPPED_ITEM_MULTIPLIER;

    public static final FabricConfigSpec.BooleanValue SEASONAL_WIND_ENABLED;
    public static final FabricConfigSpec.DoubleValue SEASONAL_WIND_INTENSITY;

    public enum NetherWindMode { DISABLED, REDUCED_GUSTY, NORMAL }
    public enum EndWindMode { DISABLED, VERY_WEAK, NORMAL }
    public static final FabricConfigSpec.EnumValue<NetherWindMode> NETHER_WIND_MODE;
    public static final FabricConfigSpec.EnumValue<EndWindMode> END_WIND_MODE;
    public static final FabricConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST;
    public static final FabricConfigSpec.ConfigValue<List<? extends String>> DIMENSION_MODES;
    public static final FabricConfigSpec.EnumValue<WindDimensionSettings.Mode> DEFAULT_DIMENSION_MODE;
    public static final FabricConfigSpec.BooleanValue ALTITUDE_SCALING_ENABLED;
    public static final FabricConfigSpec.IntValue ALTITUDE_MIN_Y;
    public static final FabricConfigSpec.IntValue ALTITUDE_MAX_Y;
    public static final FabricConfigSpec.DoubleValue ALTITUDE_MIN_MULTIPLIER;
    public static final FabricConfigSpec.DoubleValue ALTITUDE_MAX_MULTIPLIER;

    static {
        FabricConfigSpec.Builder b = new FabricConfigSpec.Builder();
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
        WEATHER_STRENGTH_RANGES_ENABLED = b.comment(
                "Use separate background wind strength ranges for clear weather, rain and thunderstorms.",
                "OFF by default - the original weather bonuses and wind simulation remain unchanged.",
                "Gusts, pre-storm fronts, manual windy events and the global strength multiplier still apply separately."
            )
            .translation("livingwind.config.weatherStrengthRangesEnabled")
            .define("useStrengthRanges", false);
        CLEAR_WEATHER_MIN_STRENGTH = b.comment("Minimum ordinary wind strength in clear weather, before global scaling and gusts.")
            .translation("livingwind.config.clearWeatherMinStrength")
            .defineInRange("clearMinimumStrength", 0.05, 0.0, 2.5);
        CLEAR_WEATHER_MAX_STRENGTH = b.comment("Maximum ordinary wind strength in clear weather. Set equal to the minimum for steady background wind.")
            .translation("livingwind.config.clearWeatherMaxStrength")
            .defineInRange("clearMaximumStrength", 0.40, 0.0, 2.5);
        RAIN_WEATHER_MIN_STRENGTH = b.comment("Minimum ordinary wind strength during rain, before global scaling and gusts.")
            .translation("livingwind.config.rainWeatherMinStrength")
            .defineInRange("rainMinimumStrength", 0.30, 0.0, 2.5);
        RAIN_WEATHER_MAX_STRENGTH = b.comment("Maximum ordinary wind strength during rain. Set equal to the minimum for steady background wind.")
            .translation("livingwind.config.rainWeatherMaxStrength")
            .defineInRange("rainMaximumStrength", 0.85, 0.0, 2.5);
        THUNDER_WEATHER_MIN_STRENGTH = b.comment("Minimum ordinary wind strength during thunderstorms, before global scaling and gusts.")
            .translation("livingwind.config.thunderWeatherMinStrength")
            .defineInRange("thunderMinimumStrength", 0.65, 0.0, 2.5);
        THUNDER_WEATHER_MAX_STRENGTH = b.comment("Maximum ordinary wind strength during thunderstorms. Set equal to the minimum for steady background wind.")
            .translation("livingwind.config.thunderWeatherMaxStrength")
            .defineInRange("thunderMaximumStrength", 1.35, 0.0, 2.5);
        WEATHER_STRENGTH_FLUCTUATION = b.comment(
                "Ordinary wind strength fluctuation (0 = steady, 1 = full natural variation).",
                "When custom weather ranges are enabled, this controls the movement inside each range.",
                "Does not suppress gusts, pre-storm fronts, commands, wind direction changes or weather transitions."
            )
            .translation("livingwind.config.weatherStrengthFluctuation")
            .defineInRange("strengthFluctuation", 1.0, 0.0, 1.0);
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
        DIMENSION_BLACKLIST = b.comment("Dimension IDs with Living Wind completely disabled (e.g. modid:dimension). Overrides Nether/End settings and forced wind commands.")
            .translation("livingwind.config.dimensionBlacklist")
            .defineListAllowEmpty("dimensionBlacklist", List.<String>of(), () -> "modid:dimension",
                entry -> entry instanceof String id && ResourceLocation.tryParse(id) != null);
        DIMENSION_MODES = b.comment("Dimension modes as modid:dimension=MODE. Valid modes: NORMAL, REDUCED_GUSTY, VERY_WEAK, DISABLED. Entries override the legacy blacklist and vanilla mode values.")
            .translation("livingwind.config.dimensionModes")
            .defineListAllowEmpty("dimensionModes", List.<String>of(), () -> "modid:dimension=NORMAL",
                entry -> WindDimensionSettings.validEntry(entry));
        DEFAULT_DIMENSION_MODE = b.comment("Wind behavior of dimensions not explicitly listed (vanilla Nether and End retain their previous modes until overridden).")
            .translation("livingwind.config.defaultDimensionMode")
            .defineEnum("defaultDimensionMode", WindDimensionSettings.Mode.NORMAL);
        b.pop();

        b.comment("Optional Serene Seasons / Ecliptic Seasons seasonal atmosphere. Server controls affect the synchronized wind for every player.")
            .translation("livingwind.config.section.seasonalCompatibility")
            .push("seasonalCompatibility");
        SEASONAL_WIND_ENABLED = b.comment("Adjust background wind and natural gust frequency/intensity across the four seasons. Respects manual wind commands and dimensions without seasons.")
            .translation("livingwind.config.seasonalWindEnabled")
            .define("seasonalWindEnabled", true);
        SEASONAL_WIND_INTENSITY = b.comment("Scale seasonal wind differences: 0 = normal Living Wind all year, 1 = balanced seasons, 2 = exaggerated seasonal changes.")
            .translation("livingwind.config.seasonalWindIntensity")
            .defineInRange("seasonalWindIntensity", 1.0, 0.0, 2.0);
        b.pop();

        b.comment("Optional directional wind for deployed gliders from Gliders, Reliable Gliders, and Paraglider. Independent of Entity Wind; only affects actively gliding local players.")
            .translation("livingwind.config.section.gliderWind")
            .push("gliderWind");
        GLIDER_WIND_ENABLED = b.comment("Let wind push an actively gliding player. Handles headwinds, tailwinds and sideways drift; prevents stacking with ordinary player Entity Wind.")
            .define("enabled", true);
        GLIDER_WIND_INFLUENCE = b.comment("Strength of the glider-only wind effect. 0 disables movement, 1 is balanced; higher values are stronger. The effect is smooth and capped.")
            .defineInRange("influence", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Optional physical wind on entities. Disabled by default; changes gameplay when enabled.")
            .translation("livingwind.config.section.entityWind")
            .push("entityWind");
        ENTITY_WIND_ENABLED = b.comment("Master switch: allow Living Wind to influence entity movement. OFF by default.")
            .translation("livingwind.config.entityWindEnabled")
            .define("enabled", false);
        ENTITY_WIND_CHECK_SHELTER = b.comment("Check outdoor exposure for wind-affected entities. Disable to skip per-entity sky checks for better performance, but entities can then be pushed indoors or underground. Does not change particle or sound shelter behavior.")
            .translation("livingwind.config.entityWindCheckShelter")
            .define("checkShelter", true);
        ENTITY_WIND_CLEAR_WEATHER_MULTIPLIER = b.comment(
                "Extra entity push multiplier during clear weather, applied on top of each entity type's own wind impact settings.",
                "0 = no entity push in clear weather, 1.0 = unchanged. Rain and thunder transitions blend smoothly. Only used if Entity Wind is enabled."
            )
            .translation("livingwind.config.entityWindClearWeatherMultiplier")
            .defineInRange("clearWeatherMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_RAIN_MULTIPLIER = b.comment(
                "Extra entity push multiplier during rain (all affected entity types). 1.0 keeps the original strength.",
                "Applied only to entity wind, not Living Wind's shared wind strength or particle physics."
            )
            .translation("livingwind.config.entityWindRainMultiplier")
            .defineInRange("rainMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_THUNDER_MULTIPLIER = b.comment(
                "Extra entity push multiplier during thunderstorms (all affected entity types). 1.0 keeps the original strength.",
                "Works independently of the Weather page's wind strength ranges and blends during weather transitions."
            )
            .translation("livingwind.config.entityWindThunderMultiplier")
            .defineInRange("thunderMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_PLAYERS = b.comment("Push survival/adventure players, with stronger effects while airborne. Creative/spectator and mounted players are excluded.")
            .translation("livingwind.config.entityWindPlayers")
            .define("players", true);
        ENTITY_WIND_PLAYER_GROUNDED_MULTIPLIER = b.comment("Wind force on grounded players, relative to the original 0.12 grounded factor. 2.5 is 2.5x stronger than before.")
            .translation("livingwind.config.entityWindPlayerGroundedMultiplier")
            .defineInRange("playerGroundedMultiplier", 2.5, 0.0, 5.0);
        ENTITY_WIND_PLAYER_MULTIPLIER = b.comment("Wind force on jumping/falling players. Existing playerMultiplier values are preserved. 0 disables airborne player push.")
            .translation("livingwind.config.entityWindPlayerMultiplier")
            .defineInRange("playerMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_MOBS = b.comment("Push mobs, especially while falling or jumping. Ground movement receives only a small push.")
            .translation("livingwind.config.entityWindMobs")
            .define("mobs", true);
        ENTITY_WIND_MOB_GROUNDED_MULTIPLIER = b.comment("Wind force on grounded mobs, relative to the original 0.12 grounded factor. 2.5 is 2.5x stronger than before.")
            .translation("livingwind.config.entityWindMobGroundedMultiplier")
            .defineInRange("mobGroundedMultiplier", 2.5, 0.0, 5.0);
        ENTITY_WIND_MOB_MULTIPLIER = b.comment("Wind force on airborne mobs. Existing mobMultiplier values are preserved. 0 disables airborne mob push.")
            .translation("livingwind.config.entityWindMobMultiplier")
            .defineInRange("mobMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_PROJECTILES = b.comment("Deflect moving projectiles while airborne, such as arrows, snowballs and thrown potions.")
            .translation("livingwind.config.entityWindProjectiles")
            .define("projectiles", true);
        ENTITY_WIND_PROJECTILE_MULTIPLIER = b.comment("Multiplier for wind deflection of projectiles. 0 disables the force for projectiles.")
            .translation("livingwind.config.entityWindProjectileMultiplier")
            .defineInRange("projectileMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_PROJECTILE_BLACKLIST = b.comment("Extra projectile entity IDs exempt from wind movement. Built-in protections are applied separately. Only checked when entity and projectile wind are enabled.")
            .translation("livingwind.config.projectileBlacklist")
            .defineListAllowEmpty("projectileBlacklist", List.<String>of(), () -> "modid:projectile", WindConfig::validProjectileId);
        ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS = b.comment("Built-in projectile exemptions to disable after unlocking defaults in the editor. These are no longer protected from wind.")
            .translation("livingwind.config.projectileBlacklistExceptions")
            .defineListAllowEmpty("projectileBlacklistExceptions", List.<String>of(), () -> "modid:projectile", WindConfig::validProjectileId);
        ENTITY_WIND_BOATS = b.comment("Let wind gently drift vanilla boats and chest boats. Steering remains under the rider's control. OFF by default.")
            .translation("livingwind.config.entityWindBoats")
            .define("boats", false);
        ENTITY_WIND_BOAT_MULTIPLIER = b.comment("Multiplier for wind-driven boat drift. 0 disables boat force. Only applies in exposed outdoor areas.")
            .translation("livingwind.config.entityWindBoatMultiplier")
            .defineInRange("boatMultiplier", 1.0, 0.0, 5.0);
        ENTITY_WIND_DROPPED_ITEMS = b.comment("Let exposed dropped item entities drift with wind, including items floating at the water surface. OFF by default.")
            .translation("livingwind.config.entityWindDroppedItems")
            .define("droppedItems", false);
        ENTITY_WIND_DROPPED_ITEM_MULTIPLIER = b.comment("Multiplier for dropped item wind drift. 0 disables the force; items on the ground experience less push.")
            .translation("livingwind.config.entityWindDroppedItemMultiplier")
            .defineInRange("droppedItemMultiplier", 1.0, 0.0, 5.0);
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

        b.comment("Optional Sable / Create Aeronautics vehicle wind physics. Server-authoritative; has no effect without Sable installed.")
            .translation("livingwind.config.section.aeronautics")
            .push("aeronautics");
        AERONAUTICS_WIND_ENABLED = b.comment("Let wind push assembled Sable contraptions with Aeronautics envelopes, Create sails, or tagged balloon blocks (including Coasters: Simulated). Disabled automatically when Sable is absent or AeroWeather supplies physics.")
            .translation("livingwind.config.aeronauticsWindEnabled")
            .define("enabled", true);
        AERONAUTICS_WIND_MULTIPLIER = b.comment("Wind force multiplier on eligible airships/sail-bearing contraptions. 1.0x is the balanced, playtested baseline (formerly 0.30x); 0 disables the force.")
            .translation("livingwind.config.aeronauticsWindMultiplier")
            .defineInRange("forceMultiplier", 1.0, 0.0, 5.0);
        AERONAUTICS_KINETIC_BEARING_ENABLED = b.comment("Drive Transmission & Linkage Kinetic Conversion Bearings with Living Wind. Only assembled Sable rotors with Create windmill sails generate torque. NeoForge 1.21.1 only.")
            .translation("livingwind.config.kineticConversionBearingEnabled")
            .define("kineticConversionBearingEnabled", true);
        AERONAUTICS_KINETIC_BEARING_MULTIPLIER = b.comment("Transmission & Linkage windmill power multiplier. 1.0 uses the stronger v1.3.7 wind-drive balance; higher values provide more power under load. 0 disables wind drive (drag remains).")
            .translation("livingwind.config.kineticConversionBearingTorqueMultiplier")
            .defineInRange("kineticConversionBearingTorqueMultiplier", 1.0, 0.0, 5.0);
        AERONAUTICS_KINETIC_BEARING_DEBUG = b.comment("Diagnostic: log rotor sail layout every 10 seconds, plus wind strength, RPM and limiting forces every 5 seconds. Enable only for testing.")
            .translation("livingwind.config.kineticConversionBearingDebug")
            .define("kineticConversionBearingDebug", false);
        b.pop();

        b.comment("Optional Create: High Seas wind integration. Replaces its wind queries with Living Wind, leaving sail physics to High Seas. Requires Create: High Seas.")
            .translation("livingwind.config.section.highSeas")
            .push("highSeas");
        HIGH_SEAS_WIND_ENABLED = b.comment(
                "When High Seas is installed, feed its sails, wind vane, and wind visuals from Living Wind's synchronized weather instead of High Seas' procedural wind.",
                "Turn off to restore High Seas' original wind simulation and behavior."
            )
            .translation("livingwind.config.highSeasWindEnabled")
            .define("enabled", true);
        b.pop();

        b.comment("Optional Small Ships 2.0 wind integration. The ships retain their sailing physics and use Living Wind's direction/strength instead of independently generated wind.")
            .translation("livingwind.config.section.smallShips")
            .push("smallShips");
        SMALL_SHIPS_WIND_ENABLED = b.comment("Use Living Wind as Small Ships' wind source when Small Ships is installed. Disable to restore its independent wind simulation.")
            .translation("livingwind.config.smallShipsWindEnabled")
            .define("enabled", true);
        b.pop();

        b.comment("Optional Realm RPG: Treasure Balloons flight integration (both loaders). Has no effect without the other mod.")
            .translation("livingwind.config.section.treasureBalloons")
            .push("treasureBalloons");
        TREASURE_BALLOONS_WIND_ENABLED = b.comment("Let Living Wind push intact Treasure Balloons in its direction, speeding them up downwind and slowing them upwind. Disable to restore the original cardinal-direction flight.")
            .translation("livingwind.config.treasureBalloonsWindEnabled")
            .define("enabled", true);
        TREASURE_BALLOONS_WIND_MULTIPLIER = b.comment("Strength of wind-driven Treasure Balloon drift. 1.0 is balanced; 0 disables drift. Does not affect popped balloons.")
            .translation("livingwind.config.treasureBalloonsWindMultiplier")
            .defineInRange("windMultiplier", 1.0, 0.0, 5.0);
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

    private static boolean validProjectileId(Object value) {
        if (!(value instanceof String id) || id.isBlank() || !id.contains(":")) return false;
        return ResourceLocation.tryParse(id) != null;
    }

    private WindConfig() {}
}
