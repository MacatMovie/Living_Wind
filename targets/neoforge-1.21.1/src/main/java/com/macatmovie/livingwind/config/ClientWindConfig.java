package com.macatmovie.livingwind.config;

import java.util.List;
import com.macatmovie.livingwind.client.LeafParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientWindConfig {
    public enum FilterMode { BLACKLIST, WHITELIST }
    /** Falling tree-leaf particle source when Particular is installed. Flowers and grass stay independent. */
    public enum FallingLeavesSource { LIVING_WIND, PARTICULAR, BOTH }
    public enum DebugHudMode { OFF, SIMPLE, ALL }
    /** Which mod should supply the general wind soundscape when AmbientSounds is installed. */
    public enum WindAudioMode { LIVING_WIND, AMBIENT_SOUNDS, BOTH }
    /** Optional Dynamic Surroundings ambient wind source, independent of Ambient Sounds. */
    public enum DynamicSurroundingsWindMode { LIVING_WIND, DYNAMIC_SURROUNDINGS, BOTH }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AFFECT_EXISTING_PARTICLES;
    public static final ModConfigSpec.DoubleValue PARTICLE_WIND_FORCE;
    public static final ModConfigSpec.EnumValue<FilterMode> PARTICLE_FILTER_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PARTICLE_FILTER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PARTICLE_RESPONSE_MULTIPLIERS;
    public static final ModConfigSpec.BooleanValue AMBIENT_PARTICLES;
    public static final ModConfigSpec.IntValue AMBIENT_SAMPLES_PER_TICK;
    public static final ModConfigSpec.IntValue AMBIENT_RADIUS;
    public static final ModConfigSpec.DoubleValue AMBIENT_MIN_WIND;
    public static final ModConfigSpec.DoubleValue AMBIENT_DENSITY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AMBIENT_WIND_RESPONSE_SCALE;
    public static final ModConfigSpec.DoubleValue AMBIENT_LIFT_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AMBIENT_SWAY_MULTIPLIER;
    public static final ModConfigSpec.IntValue AMBIENT_LIFETIME_MIN_TICKS;
    public static final ModConfigSpec.IntValue AMBIENT_LIFETIME_MAX_TICKS;
    public static final ModConfigSpec.BooleanValue LEAF_PARTICLES_ENABLED;
    public static final ModConfigSpec.DoubleValue LEAF_PARTICLE_DENSITY;
    public static final ModConfigSpec.BooleanValue LEAF_TEXTURE_COLORS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEAF_TYPE_OVERRIDES;
    public static final ModConfigSpec.DoubleValue CONIFER_SPAWN_MULTIPLIER;
    public static final ModConfigSpec.IntValue LEAF_PARTICLE_RADIUS;
    public static final ModConfigSpec.DoubleValue LEAF_PARTICLE_WIND_RESPONSE;
    public static final ModConfigSpec.BooleanValue PRETTY_RAIN_COMPAT;
    public static final ModConfigSpec.BooleanValue PARTICLE_RAIN_COMPAT;
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIMNEYS_WIND_COMPAT;
    public static final ModConfigSpec.DoubleValue PARTICLE_RAIN_WIND_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PRETTY_RAIN_WIND_MULTIPLIER;
    public static final ModConfigSpec.EnumValue<FallingLeavesSource> FALLING_LEAVES_SOURCE;
    public static final ModConfigSpec.BooleanValue PARTICULAR_FALLING_LEAVES_COMPAT;
    public static final ModConfigSpec.DoubleValue PARTICULAR_LEAF_MIN_DENSITY;
    public static final ModConfigSpec.DoubleValue PARTICULAR_LEAF_MAX_DENSITY;
    public static final ModConfigSpec.DoubleValue PARTICULAR_LEAF_FULL_WIND_STRENGTH;
    public static final ModConfigSpec.BooleanValue WIND_AUDIO_ENABLED;
    public static final ModConfigSpec.DoubleValue WIND_AUDIO_VOLUME;
    public static final ModConfigSpec.BooleanValue WIND_AUDIO_GUSTS;
    public static final ModConfigSpec.BooleanValue WIND_AUDIO_ENVIRONMENTAL;
    public static final ModConfigSpec.EnumValue<WindAudioMode> WIND_AUDIO_MODE;
    public static final ModConfigSpec.EnumValue<DynamicSurroundingsWindMode> DYNAMIC_SURROUNDINGS_WIND_MODE;
    public static final ModConfigSpec.BooleanValue REDUCE_WIND_INDOORS;
    public static final ModConfigSpec.DoubleValue SHELTER_EFFECTIVENESS;
    public static final ModConfigSpec.DoubleValue INDOOR_AUDIO_ATTENUATION;
    public static final ModConfigSpec.BooleanValue SHELTER_LOW_COST;
    public static final ModConfigSpec.BooleanValue SWINGING_LANTERNS_COMPAT;
    public static final ModConfigSpec.DoubleValue SWINGING_LANTERNS_WIND_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue FWA_WIND_COMPAT;
    public static final ModConfigSpec.BooleanValue SUPPLEMENTARIES_WIND_VANE_COMPAT;
    public static final ModConfigSpec.BooleanValue INTERACTIVE_FOLIAGE_WIND_COMPAT;
    public static final ModConfigSpec.DoubleValue FWA_WIND_IMPACT;
    public static final ModConfigSpec.DoubleValue FWA_CHAIN_INTENSITY;
    public static final ModConfigSpec.DoubleValue FWA_LANTERN_INTENSITY;
    public static final ModConfigSpec.DoubleValue FWA_BELL_INTENSITY;
    public static final ModConfigSpec.BooleanValue DEBUG_HUD; // Legacy showHud value, retained for config migration.
    public static final ModConfigSpec.EnumValue<DebugHudMode> DEBUG_HUD_MODE;
    public static final ModConfigSpec.BooleanValue SHADER_BRIDGE_ENABLED;
    public static final ModConfigSpec.BooleanValue SHADER_AUTO_PATCH;
    public static final ModConfigSpec.DoubleValue SHADER_WAVING_MULTIPLIER;

    private static final List<String> DEFAULT_PARTICLE_RESPONSES = List.of(
        "particular:oak_leaf=2.5",
        "particular:birch_leaf=2.5",
        "particular:spruce_leaf=2.5",
        "particular:jungle_leaf=2.5",
        "particular:acacia_leaf=2.5",
        "particular:dark_oak_leaf=2.5",
        "particular:azalea_leaf=2.5",
        "particular:mangrove_leaf=2.5",
        "particular:white_oak_leaf=2.5",
        "particular:white_spruce_leaf=2.5",
        "particular:maple_leaf=2.5",
        "particular:brimwood_leaf=2.5",
        "particular:ru_baobab_leaf=2.5",
        "particular:kapok_leaf=2.5",
        "particular:eucalyptus_leaf=2.5",
        "particular:redwood_leaf=2.5",
        "particular:magnolia_leaf=2.5",
        "particular:ru_palm_leaf=2.5",
        "particular:larch_leaf=2.5",
        "particular:golden_larch_leaf=2.5",
        "particular:socotra_leaf=2.5",
        "particular:bamboo_leaf=2.5",
        "particular:willow_leaf=2.5",
        "particular:ru_cypress_leaf=2.5",
        "particular:ww_baobab_leaf=2.5",
        "particular:ww_cypress_leaf=2.5",
        "particular:ww_palm_leaf=2.5",
        // SkyLands' large clouds should drift slowly rather than being blown rapidly away.
        "flying_stuff:cloud=0.25",
        "flying_stuff:cloud_smoke=0.25",
        "flying_stuff:cloud_smoke_1=0.25",
        "flying_stuff:cloud_barrier_2=0.25",
        "flying_stuff:cloud_barrier_3=0.25"
    );

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("How synchronized wind pushes existing vanilla and modded particles.")
            .translation("livingwind.config.section.particleWind")
            .push("particleWind");
        AFFECT_EXISTING_PARTICLES = b.comment("Allow Living Wind to push eligible existing particles.")
            .translation("livingwind.config.affectExistingParticles")
            .define("affectExistingParticles", true);
        PARTICLE_WIND_FORCE = b.comment(
                "Horizontal drift added to eligible particles. High wind is intentionally amplified",
                "more strongly than light wind so storms and gusts are visually distinct."
            )
            .translation("livingwind.config.particleWindForce")
            .defineInRange("particleWindForce", 0.12, 0.0, 0.25);
        PARTICLE_FILTER_MODE = b.comment("Choose whether the particle ID list is a blacklist or whitelist.")
            .translation("livingwind.config.filterMode")
            .defineEnum("filterMode", FilterMode.BLACKLIST);
        PARTICLE_FILTER = b.comment("Your custom particle IDs (blacklist additions, or whitelist entries).", "Living Wind maintains built-in blacklist defaults separately; future updates never replace your custom entries.")
            .translation("livingwind.config.filter")
            .defineListAllowEmpty(
                "filter",
                List.of(),
                () -> "minecraft:particle_id",
                o -> o instanceof String s && ResourceLocation.tryParse(s) != null
            );
        PARTICLE_RESPONSE_MULTIPLIERS = b.comment(
                "Optional per-particle wind response multipliers, stored as modid:particle=multiplier.",
                "1.0 keeps normal Living Wind response; values above 1 make a particle lighter/more wind-responsive;",
                "values between 0 and 1 reduce response. Particular leaves default to 2.5x and SkyLands clouds to 0.25x.",
                "Built-in mod defaults still apply to older configs; an explicit entry here overrides them."
            )
            .translation("livingwind.config.responseMultipliers")
            .defineListAllowEmpty(
                "responseMultipliers",
                DEFAULT_PARTICLE_RESPONSES,
                () -> "minecraft:particle=1.0",
                ClientWindConfig::isResponseEntry
            );
        b.pop();

        b.comment("Tiny world particles emitted by exposed blocks when wind is present.")
            .translation("livingwind.config.section.ambientParticles")
            .push("ambientParticles");
        AMBIENT_PARTICLES = b.comment("Enable ambient wind particles emitted by nearby blocks.")
            .translation("livingwind.config.ambientEnabled")
            .define("enabled", true);
        AMBIENT_SAMPLES_PER_TICK = b.comment(
                "Random nearby block samples per client tick. The system deliberately avoids cubic area scans."
            )
            .translation("livingwind.config.samplesPerTick")
            .defineInRange("samplesPerTick", 8, 0, 32);
        AMBIENT_RADIUS = b.comment("Horizontal radius used for randomized ambient emitter samples.")
            .translation("livingwind.config.sampleRadius")
            .defineInRange("sampleRadius", 18, 4, 48);
        AMBIENT_MIN_WIND = b.comment(
                "Minimum local wind strength before ambient wind pixels can spawn.",
                "Below this threshold no ambient wind particles are emitted."
            )
            .translation("livingwind.config.minimumWindStrength")
            .defineInRange("minimumWindStrength", 0.12, 0.0, 1.5);
        AMBIENT_DENSITY_MULTIPLIER = b.comment(
                "Multiplier for ambient-particle spawn chance without increasing terrain sample count.",
                "1.30 means roughly 30% more ambient particles for the same wind and sampling workload."
            )
            .translation("livingwind.config.ambientDensityMultiplier")
            .defineInRange("densityMultiplier", 1.30, 0.0, 4.0);
        AMBIENT_WIND_RESPONSE_SCALE = b.comment(
                "User-facing scale for how strongly Living Wind's tiny ambient debris follows the wind.",
                "The 1.0 scale is calibrated to the old 3.0 setting; the new default is 2.0 for stronger wind response.",
                "Use values below/above 2.0 to reduce/increase the new default response."
            )
            .translation("livingwind.config.ambientWindResponseMultiplier")
            .defineInRange("windResponseScale", 2.0, 0.0, 4.0);
        AMBIENT_LIFT_MULTIPLIER = b.comment(
                "Controls the subtle upward lift of tiny ambient debris, especially in stronger wind.",
                "This is deliberately gentle; higher values make motes rise more noticeably."
            )
            .translation("livingwind.config.ambientLiftMultiplier")
            .defineInRange("liftMultiplier", 1.0, 0.0, 4.0);
        AMBIENT_SWAY_MULTIPLIER = b.comment(
                "Controls smooth cross-wind meandering of ambient debris.",
                "This adds low-frequency turbulence without changing the main wind direction."
            )
            .translation("livingwind.config.ambientSwayMultiplier")
            .defineInRange("swayMultiplier", 1.0, 0.0, 4.0);
        AMBIENT_LIFETIME_MIN_TICKS = b.comment(
                "Minimum lifetime of an ambient wind pixel in ticks. 20 ticks = 1 second."
            )
            .translation("livingwind.config.ambientLifetimeMin")
            .defineInRange("minimumLifetimeTicks", 100, 20, 600);
        AMBIENT_LIFETIME_MAX_TICKS = b.comment(
                "Maximum lifetime of an ambient wind pixel in ticks. 20 ticks = 1 second."
            )
            .translation("livingwind.config.ambientLifetimeMax")
            .defineInRange("maximumLifetimeTicks", 180, 20, 600);
        b.pop();

        b.comment("Canopy-born, visibly tumbling native Minecraft leaf particles.")
            .translation("livingwind.config.section.windLeaves")
            .push("windLeaves");
        LEAF_PARTICLES_ENABLED = b.comment("Generate native, tumbling leaves/petals/grass blades from exposed foliage and plants, independently of the tiny ambient particles.")
            .translation("livingwind.config.windLeavesEnabled")
            .define("enabled", true);
        LEAF_PARTICLE_DENSITY = b.comment("Amount of windblown foliage and grass blades near trees, flowers and meadows; scales with wind strength and Minecraft particle settings.")
            .translation("livingwind.config.windLeavesDensity")
            .defineInRange("density", 1.0, 0.0, 4.0);
        LEAF_TEXTURE_COLORS = b.comment(
                "Derive flower/petal particle colors from loaded modded block textures.",
                "Colors are sampled only once per block state, cached, and refreshed on resource-pack reload.",
                "Disable to revert to biome tint, flower-name fallback, and map color only."
            )
            .translation("livingwind.config.windLeavesTextureColors")
            .define("textureDerivedColors", true);
        LEAF_TYPE_OVERRIDES = b.comment(
                "Custom per-block leaf particle types: modid:block=CONIFER or modid:block=BROADLEAF.",
                "Overrides take priority over block tags (including Vanilla Backport tags) and built-in species defaults.",
                "Leave this list empty to use the automatically detected categories."
            )
            .translation("livingwind.config.windLeavesTypes")
            .defineListAllowEmpty("leafTypeOverrides", List.of(),
                () -> "minecraft:spruce_leaves=CONIFER", LeafParticleTypes::validEntry);
        CONIFER_SPAWN_MULTIPLIER = b.comment(
                "Needle emission chance relative to normal broadleaf particles. 0.85 means 85% as many; 0 disables needles.",
                "Applies only to Living Wind's native canopy particles; particle motion and shared spawn budget remain unchanged."
            )
            .translation("livingwind.config.windLeavesConiferDensity")
            .defineInRange("coniferSpawnMultiplier", 0.85, 0.0, 1.0);
        LEAF_PARTICLE_RADIUS = b.comment("Radius of the lightweight foliage/plant-source sampling area in blocks.")
            .translation("livingwind.config.windLeavesRadius")
            .defineInRange("radius", 22, 8, 48);
        LEAF_PARTICLE_WIND_RESPONSE = b.comment("How strongly the dedicated leaf particles follow Living Wind. Independent of generic particle wind force.")
            .translation("livingwind.config.windLeavesWindResponse")
            .defineInRange("windResponse", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Optional compatibility with other visual weather/effects mods.")
            .translation("livingwind.config.section.compatibility")
            .push("compatibility");
        PRETTY_RAIN_COMPAT = b.comment(
                "Make Pretty Rain's rain and desert dust use Living Wind's synchronized direction and strength.",
                "Rolling shrubs/tumbleweeds use Living Wind direction only and keep Pretty Rain's own speed.",
                "The compatibility code is inert when Pretty Rain is not installed."
            )
            .translation("livingwind.config.prettyRainCompat")
            .define("prettyRainWind", true);
        PRETTY_RAIN_WIND_MULTIPLIER = b.comment(
                "Horizontal Pretty Rain rain/dust velocity per unit of Living Wind strength.",
                "Higher values create a steeper wind-driven rain angle during strong wind.",
                "This multiplier does not change rolling shrub/tumbleweed speed."
            )
            .translation("livingwind.config.prettyRainWindMultiplier")
            .defineInRange("prettyRainWindMultiplier", 0.55, 0.0, 2.0);
        PARTICLE_RAIN_COMPAT = b.comment(
                "Use Living Wind's direction and strength in the official Particle Rain 4.x wind system.",
                "Controls rain, snow, dust and other weather particles; Particle Rain retains its own physics.",
                "Requires Particle Rain 4.x and has no effect when it is absent. Independent of Pretty Rain."
            )
            .translation("livingwind.config.particleRainCompat")
            .define("particleRainWind", true);
        ADVANCED_CHIMNEYS_WIND_COMPAT = b.comment(
                "Replace only Advanced Chimneys' outdoor smoke wind with Living Wind direction and strength.",
                "Keeps the mod's original pipes, vents, pumps, chimney movement, collision and buoyancy.",
                "The compatibility code is inactive when Advanced Chimneys is not installed."
            )
            .translation("livingwind.config.advancedChimneysWindCompat")
            .define("advancedChimneysOutdoorSmokeWind", true);
        PARTICLE_RAIN_WIND_MULTIPLIER = b.comment(
                "Strength of Living Wind sent into the official Particle Rain 4.x wind simulation.",
                "The default (0.55) is calibrated for Particle Rain's cumulative wind physics.",
                "Adjust the tilt and drift without changing Particle Rain's particle density or gravity."
            )
            .translation("livingwind.config.particleRainWindMultiplier")
            .defineInRange("particleRainWindMultiplier", 0.55, 0.0, 2.0);
        FALLING_LEAVES_SOURCE = b.comment(
                "Choose which mod generates falling tree-leaf particles when Particular is installed.",
                "LIVING_WIND (default): only Living Wind. PARTICULAR: only Particular. BOTH: both.",
                "Living Wind flowers, petals and grass remain enabled regardless of this choice.",
                "If Particular is absent, Living Wind leaves remain active as a fallback."
            )
            .translation("livingwind.config.fallingLeavesSource")
            .defineEnum("fallingLeavesSource", FallingLeavesSource.LIVING_WIND);
        PARTICULAR_FALLING_LEAVES_COMPAT = b.comment(
                "When Particular leaves are selected, scale their spawn density with Living Wind strength.",
                "Weak air sheds fewer leaves, while strong wind and gusts shed substantially more.",
                "This is inert when Particular is not installed."
            )
            .translation("livingwind.config.particularLeavesCompat")
            .define("particularFallingLeavesWind", true);
        PARTICULAR_LEAF_MIN_DENSITY = b.comment(
                "Particular falling-leaf spawn multiplier at near-calm wind."
            )
            .translation("livingwind.config.particularLeavesMinDensity")
            .defineInRange("particularLeavesMinDensity", 0.15, 0.0, 4.0);
        PARTICULAR_LEAF_MAX_DENSITY = b.comment(
                "Particular falling-leaf spawn multiplier once wind reaches the configured full-density strength."
            )
            .translation("livingwind.config.particularLeavesMaxDensity")
            .defineInRange("particularLeavesMaxDensity", 3.0, 0.0, 8.0);
        PARTICULAR_LEAF_FULL_WIND_STRENGTH = b.comment(
                "Altitude-adjusted wind strength at which Particular falling leaves reach maximum spawn density."
            )
            .translation("livingwind.config.particularLeavesFullWind")
            .defineInRange("particularLeavesFullWindStrength", 1.20, 0.10, 3.0);
        b.pop();

        b.comment("Client-side wind ambience. Uses the final altitude/shelter-adjusted wind strength.")
            .translation("livingwind.config.section.windAudio")
            .push("windAudio");
        WIND_AUDIO_ENABLED = b.comment("Play Living Wind's wind ambience. Disable to silence all its wind sounds.")
            .translation("livingwind.config.windAudioEnabled")
            .define("enabled", true);
        WIND_AUDIO_VOLUME = b.comment(
                "Overall volume for Living Wind wind, gust and environment sounds. Minecraft Ambient volume still applies.",
                "1.00 is the calibrated normal Living Wind volume."
            )
            .translation("livingwind.config.windAudioVolume")
            .defineInRange("volume", 1.00, 0.0, 3.0);
        WIND_AUDIO_GUSTS = b.comment("Allow occasional gust accents synchronized to server gusts.")
            .translation("livingwind.config.windAudioGusts")
            .define("gustAccents", true);
        WIND_AUDIO_ENVIRONMENTAL = b.comment("Allow nearby leaves, high mountains, snow and desert sound accents.")
            .translation("livingwind.config.windAudioEnvironment")
            .define("environmentalAccents", true);
        WIND_AUDIO_MODE = b.comment("Choose which mod supplies wind audio when Ambient Sounds is installed. LIVING_WIND - Plays only Living Wind's wind sounds. AMBIENT_SOUNDS - Plays only Ambient Sounds's wind sounds. BOTH - Plays both wind sound systems.")
            .translation("livingwind.config.windAudioMode")
            .defineEnum("ambientSoundsMode", WindAudioMode.LIVING_WIND);
        DYNAMIC_SURROUNDINGS_WIND_MODE = b.comment(
                "Choose which mod supplies wind audio when Dynamic Surroundings is installed.",
                "LIVING_WIND (default): only Living Wind wind. DYNAMIC_SURROUNDINGS: only Dynamic Surroundings wind. BOTH: both wind soundtracks.",
                "Only Dynamic Surroundings biome wind loops are affected; its other sounds, waterfalls and reverb remain active.",
                "This selection is independent of the Ambient Sounds wind-audio mode."
            )
            .translation("livingwind.config.dynamicSurroundingsWindMode")
            .defineEnum("dynamicSurroundingsWindMode", DynamicSurroundingsWindMode.LIVING_WIND);
        b.pop();

        b.comment("Reduce effective visual wind in enclosed spaces.")
            .translation("livingwind.config.section.exposure")
            .push("exposure");
        REDUCE_WIND_INDOORS = b.comment("Enable location-aware wind shelter for indoor audio and particles. Uses a bounded cache; never scans rooms.")
            .translation("livingwind.config.reduceWindIndoors")
            .define("reduceWindIndoors", true);
        SHELTER_EFFECTIVENESS = b.comment("How strongly shelter blocks wind (0 = off, 1 = fully sheltered in sealed rooms).")
            .translation("livingwind.config.shelterEffectiveness")
            .defineInRange("shelterEffectiveness", 1.0, 0.0, 1.0);
        INDOOR_AUDIO_ATTENUATION = b.comment("Additional wind-audio volume retained when fully indoors (0 = silent, 1 = no extra muffling).")
            .translation("livingwind.config.indoorAudioAttenuation")
            .defineInRange("indoorAudioAttenuation", 0.10, 0.0, 1.0);
        SHELTER_LOW_COST = b.comment("Reduce shelter block probes per tick; updates around particles may take a little longer.")
            .translation("livingwind.config.shelterLowCost")
            .define("shelterLowCost", false);
        b.pop();

        b.comment("Experimental optional Complementary Reimagined + Euphoria 1.10 shader bridge.")
            .translation("livingwind.config.section.shaderBridge")
            .push("shaderBridge");
        SHADER_AUTO_PATCH = b.comment(
                "Automatically patch existing compatible Euphoria shader ZIPs and folders at startup.",
                "Keeps original shader backups in config/livingwind/shader-backups.",
                "The original Complementary shader ZIP is never patched. Restart after changing this option."
            )
            .translation("livingwind.config.shaderAutoPatch")
            .define("autoPatchEuphoriaShaders", true);
        SHADER_BRIDGE_ENABLED = b.comment(
                "Publish synchronized Living Wind strength, direction, gusts and animation time to Euphoria shaders.",
                "With compatible Euphoria shader packs, Living Wind patches the foliage code automatically."
            )
            .translation("livingwind.config.shaderBridgeEnabled")
            .define("enabled", true);
        SHADER_WAVING_MULTIPLIER = b.comment(
                "Amplitude multiplier for the optional Living Wind shader waving integration.",
                "Set to 0 to make the shader calm when Living Wind is connected."
            )
            .translation("livingwind.config.shaderWavingMultiplier")
            .defineInRange("wavingMultiplier", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Optional Fancy World Animations wind-responsive chains, lanterns and bells.")
            .translation("livingwind.config.section.fwa")
            .push("fancyWorldAnimations");
        FWA_WIND_COMPAT = b.comment("Link FWA swing animations to Living Wind strength, direction, gusts and shelter. FWA is optional.")
            .translation("livingwind.config.fwaEnabled")
            .define("enabled", true);
        FWA_WIND_IMPACT = b.comment("Overall impact of Living Wind on Fancy World Animations swinging objects. 0 = original FWA motion; 6 = visibly stronger wind-driven swings. Individual chain, lantern and bell intensities are additional multipliers.")
            .translation("livingwind.config.fwaWindImpact")
            .defineInRange("windImpact", 6.0, 0.0, 12.0);
        FWA_CHAIN_INTENSITY = b.comment("Strength of wind-driven chain movement; zero keeps chains mostly still while Living Wind is active.")
            .translation("livingwind.config.fwaChainIntensity")
            .defineInRange("chainIntensity", 1.0, 0.0, 3.0);
        FWA_LANTERN_INTENSITY = b.comment("Strength of wind-driven lantern movement.")
            .translation("livingwind.config.fwaLanternIntensity")
            .defineInRange("lanternIntensity", 1.0, 0.0, 3.0);
        FWA_BELL_INTENSITY = b.comment("Strength of wind-driven idle bell rocking (does not affect bell ringing).")
            .translation("livingwind.config.fwaBellIntensity")
            .defineInRange("bellIntensity", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Optional Swinging Lanterns 2.0.4 integration; its physics, collisions and Create motion remain native.")
            .translation("livingwind.config.section.swingingLanterns")
            .push("swingingLanterns");
        SWINGING_LANTERNS_COMPAT = b.comment("Use Living Wind direction and strength to drive Swinging Lanterns' existing wind input. Requires Swinging Lanterns; no extra forces are applied to its physics solver.")
            .translation("livingwind.config.swingingLanternsEnabled")
            .define("enabled", true);
        SWINGING_LANTERNS_WIND_MULTIPLIER = b.comment("Multiplier for Living Wind's sway and downwind lean of Swinging Lanterns. Light winds give gentle swings; stronger winds tilt lanterns downwind. Set to 0 for calm wind, or turn integration off for stock behavior.")
            .translation("livingwind.config.swingingLanternsMultiplier")
            .defineInRange("windMultiplier", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Optional Interactive Foliage 2.2.0 GPU grass-sway integration.")
            .translation("livingwind.config.section.interactiveFoliage")
            .push("interactiveFoliage");
        INTERACTIVE_FOLIAGE_WIND_COMPAT = b.comment("Use Living Wind direction and strength for Interactive Foliage's grass waving. Enable Weather Wind in Interactive Foliage. Its entity interactions are unchanged.")
            .translation("livingwind.config.interactiveFoliageWind")
            .define("enabled", true);
        b.pop();

        b.comment("Optional Supplementaries wind vane animation compatibility.")
            .translation("livingwind.config.section.supplementaries")
            .push("supplementaries");
        SUPPLEMENTARIES_WIND_VANE_COMPAT = b.comment("Steer Supplementaries wind vanes using Living Wind's synchronized wind direction and local shelter. The original Supplementaries redstone behavior is unchanged.")
            .translation("livingwind.config.supplementariesVane")
            .define("windVane", true);
        b.pop();

        b.comment("Developer/testing readouts.")
            .translation("livingwind.config.section.debug")
            .push("debug");
        DEBUG_HUD = b.comment("Legacy debug HUD toggle for existing configuration files. Use hudMode instead.")
            .translation("livingwind.config.showHud")
            .define("showHud", false);
        DEBUG_HUD_MODE = b.comment("OFF: no overlay; SIMPLE: wind strength, direction and gust status; ALL: full diagnostic overlay.")
            .translation("livingwind.config.hudMode")
            .defineEnum("hudMode", DebugHudMode.OFF);
        b.pop();
        SPEC = b.build();
    }

    private static boolean isResponseEntry(Object value) {
        if (!(value instanceof String raw)) return false;
        int eq = raw.lastIndexOf('=');
        if (eq <= 0 || eq >= raw.length() - 1) return false;
        ResourceLocation id = ResourceLocation.tryParse(raw.substring(0, eq).trim());
        if (id == null) return false;
        try {
            double multiplier = Double.parseDouble(raw.substring(eq + 1).trim());
            return Double.isFinite(multiplier) && multiplier >= 0.0 && multiplier <= 10.0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Preserve old showHud=true as ALL until the user picks a new mode. */
    public static DebugHudMode debugHudMode() {
        DebugHudMode selected = DEBUG_HUD_MODE.get();
        return selected == DebugHudMode.OFF && DEBUG_HUD.get() ? DebugHudMode.ALL : selected;
    }

    /** Changing modes retires the legacy boolean so OFF always means OFF. */
    public static void setDebugHudMode(DebugHudMode mode) {
        DEBUG_HUD_MODE.set(mode);
        if (DEBUG_HUD.get()) DEBUG_HUD.set(false);
    }

    private ClientWindConfig() {}
}
