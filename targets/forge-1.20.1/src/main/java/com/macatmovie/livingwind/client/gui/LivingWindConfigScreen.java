package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.config.WindConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 in-game configuration screen with no external config-screen dependency. */
public final class LivingWindConfigScreen extends Screen {
    private static final int TOP = 32, BOTTOM = 36, ROW_H = 26;
    private final Screen parent;
    private final String page;
    private final List<Row> rows = new ArrayList<>();
    private double scroll;
    private int contentHeight;
    private int labelLeft;

    public LivingWindConfigScreen(Screen parent) {
        this(parent, "main");
    }

    private LivingWindConfigScreen(Screen parent, String page) {
        super(Component.literal(switch (page) {
            case "main" -> "Living Wind Configuration";
            case "common" -> "Living Wind Common Configuration";
            case "client" -> "Living Wind Client Configuration";
            case "commonCompatibility", "clientCompatibility" -> "Living Wind Compatibility";
            default -> page;
        }));
        this.parent = parent;
        this.page = page;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets(); rows.clear();
        header("Wind Simulation");
        number("Minimum Base Strength", WindConfig.MIN_BASE_STRENGTH.get(), v -> WindConfig.MIN_BASE_STRENGTH.set(v), 0, 1.5);
        number("Maximum Base Strength", WindConfig.MAX_BASE_STRENGTH.get(), v -> WindConfig.MAX_BASE_STRENGTH.set(v), 0, 1.5);
        number("Maximum Simulated Strength", WindConfig.MAX_EFFECTIVE_STRENGTH.get(), v -> WindConfig.MAX_EFFECTIVE_STRENGTH.set(v), .25, 2.5);
        number("Global Strength Multiplier", WindConfig.GLOBAL_STRENGTH_MULTIPLIER.get(), v -> WindConfig.GLOBAL_STRENGTH_MULTIPLIER.set(v), .25, 2.5);

        header("Weather");
        number("Rain Strength Bonus (Legacy)", WindConfig.RAIN_STRENGTH_BONUS.get(), v -> WindConfig.RAIN_STRENGTH_BONUS.set(v), 0, 1.5);
        number("Thunder Strength Bonus (Legacy)", WindConfig.THUNDER_STRENGTH_BONUS.get(), v -> WindConfig.THUNDER_STRENGTH_BONUS.set(v), 0, 1.5);
        boolTooltip("Custom Weather Strength Ranges", WindConfig.WEATHER_STRENGTH_RANGES_ENABLED.get(), WindConfig.WEATHER_STRENGTH_RANGES_ENABLED::set,
            "OFF (default): original wind/weather behavior. ON: set a separate min/max for clear, rain and thunderstorms. Gusts and pre-storm fronts stay independent.");
        number("Clear Weather Minimum", WindConfig.CLEAR_WEATHER_MIN_STRENGTH.get(), WindConfig.CLEAR_WEATHER_MIN_STRENGTH::set, 0, 2.5);
        number("Clear Weather Maximum", WindConfig.CLEAR_WEATHER_MAX_STRENGTH.get(), WindConfig.CLEAR_WEATHER_MAX_STRENGTH::set, 0, 2.5);
        number("Rain Minimum", WindConfig.RAIN_WEATHER_MIN_STRENGTH.get(), WindConfig.RAIN_WEATHER_MIN_STRENGTH::set, 0, 2.5);
        number("Rain Maximum", WindConfig.RAIN_WEATHER_MAX_STRENGTH.get(), WindConfig.RAIN_WEATHER_MAX_STRENGTH::set, 0, 2.5);
        number("Thunderstorm Minimum", WindConfig.THUNDER_WEATHER_MIN_STRENGTH.get(), WindConfig.THUNDER_WEATHER_MIN_STRENGTH::set, 0, 2.5);
        number("Thunderstorm Maximum", WindConfig.THUNDER_WEATHER_MAX_STRENGTH.get(), WindConfig.THUNDER_WEATHER_MAX_STRENGTH::set, 0, 2.5);
        percentageSlider("Regular Wind Fluctuation", WindConfig.WEATHER_STRENGTH_FLUCTUATION.get(), WindConfig.WEATHER_STRENGTH_FLUCTUATION::set);

        header("Wind Events");
        number("Pre-Storm Front Chance", WindConfig.PRE_STORM_FRONT_CHANCE.get(), v -> WindConfig.PRE_STORM_FRONT_CHANCE.set(v), 0, 1);
        integer("Pre-Storm Lead Ticks", WindConfig.PRE_STORM_LEAD_TICKS.get(), v -> WindConfig.PRE_STORM_LEAD_TICKS.set(v), 200, 12000);
        number("Calm Day Chance", WindConfig.CALM_DAY_CHANCE.get(), v -> WindConfig.CALM_DAY_CHANCE.set(v), 0, 1);
        number("Breezy Day Chance", WindConfig.BREEZY_DAY_CHANCE.get(), v -> WindConfig.BREEZY_DAY_CHANCE.set(v), 0, 1);
        number("Windy Period Chance", WindConfig.WINDY_PERIOD_CHANCE.get(), v -> WindConfig.WINDY_PERIOD_CHANCE.set(v), 0, 1);
        integer("Windy Period Min Ticks", WindConfig.WINDY_PERIOD_MIN_TICKS.get(), v -> WindConfig.WINDY_PERIOD_MIN_TICKS.set(v), 200, 24000);
        integer("Windy Period Max Ticks", WindConfig.WINDY_PERIOD_MAX_TICKS.get(), v -> WindConfig.WINDY_PERIOD_MAX_TICKS.set(v), 200, 24000);
        bool("Gust Clusters", WindConfig.GUST_CLUSTERS_ENABLED.get(), WindConfig.GUST_CLUSTERS_ENABLED::set);
        number("Gust Cluster Chance Multiplier", WindConfig.GUST_CLUSTER_CHANCE_MULTIPLIER.get(), WindConfig.GUST_CLUSTER_CHANCE_MULTIPLIER::set, 0, 3);

        header("Seasonal Compatibility (Wind)");
        bool("Seasonal Wind", WindConfig.SEASONAL_WIND_ENABLED.get(), WindConfig.SEASONAL_WIND_ENABLED::set);
        number("Seasonal Wind Intensity", WindConfig.SEASONAL_WIND_INTENSITY.get(), WindConfig.SEASONAL_WIND_INTENSITY::set, 0, 2);

        header("Create Windmills");
        bool("Wind-Driven Create Windmills", WindConfig.CREATE_WINDMILLS_ENABLED.get(), WindConfig.CREATE_WINDMILLS_ENABLED::set);
        number("Create Minimum Speed Multiplier", WindConfig.CREATE_WINDMILL_MIN_MULTIPLIER.get(), WindConfig.CREATE_WINDMILL_MIN_MULTIPLIER::set, 0.01, 2.0);
        number("Create Maximum Speed Multiplier", WindConfig.CREATE_WINDMILL_MAX_MULTIPLIER.get(), WindConfig.CREATE_WINDMILL_MAX_MULTIPLIER::set, 0.50, 4.0);
        number("Create RPM Step", WindConfig.CREATE_WINDMILL_RPM_STEP.get(), WindConfig.CREATE_WINDMILL_RPM_STEP::set, 0.25, 4.0);
        number("Create Min RPM Difference", WindConfig.CREATE_WINDMILL_MIN_RPM_CHANGE.get(), WindConfig.CREATE_WINDMILL_MIN_RPM_CHANGE::set, 0.25, 8.0);
        number("Create Max RPM Change per Update", WindConfig.CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE.get(), WindConfig.CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE::set, 0.50, 32.0);
        integer("Create Refresh Interval (Ticks)", WindConfig.CREATE_WINDMILL_UPDATE_TICKS.get(), WindConfig.CREATE_WINDMILL_UPDATE_TICKS::set, 20, 200);

        header("Valkyrien Skies 2 / Eureka / Clockwork");
        boolTooltip("Use Living Wind for VS Ships", WindConfig.VS_WIND_ENABLED.get(), WindConfig.VS_WIND_ENABLED::set,
            "Adds Living Wind as an aerodynamic wind source to Valkyrien Skies ships, including Eureka and Clockwork. Independent from Entity Wind.");
        slider("VS Wind Speed Scale", WindConfig.VS_WIND_SPEED_SCALE.get(), WindConfig.VS_WIND_SPEED_SCALE::set, 0, 20);
        slider("VS Maximum Wind Speed", WindConfig.VS_WIND_MAX_SPEED.get(), WindConfig.VS_WIND_MAX_SPEED::set, 0, 40);
        integer("VS Wind Update Interval (Ticks)", WindConfig.VS_WIND_UPDATE_TICKS.get(), WindConfig.VS_WIND_UPDATE_TICKS::set, 1, 40);
        boolTooltip("Combine with Weather2 Wind", WindConfig.VS_WIND_WITH_WEATHER2.get(), WindConfig.VS_WIND_WITH_WEATHER2::set,
            "OFF: Living Wind will not add wind when Weather2 is installed, avoiding stacked wind sources. ON: both supply wind to VS. Turn off Weather2's VS integration to use only Living Wind.");

        header("Small Ships 2.0");
        bool("Use Living Wind for Small Ships", WindConfig.SMALL_SHIPS_WIND_ENABLED.get(), WindConfig.SMALL_SHIPS_WIND_ENABLED::set);

        header("Aleki's Nifty Ships");
        bool("Use Living Wind for Nifty Ships", WindConfig.ALEKI_SHIPS_WIND_ENABLED.get(), WindConfig.ALEKI_SHIPS_WIND_ENABLED::set);
        number("Nifty Ships Wind Strength Scale", WindConfig.ALEKI_SHIPS_WIND_SCALE.get(), WindConfig.ALEKI_SHIPS_WIND_SCALE::set, 0, 1);

        header("Treasure Balloons");
        bool("Wind-Driven Treasure Balloons", WindConfig.TREASURE_BALLOONS_WIND_ENABLED.get(), WindConfig.TREASURE_BALLOONS_WIND_ENABLED::set);
        number("Treasure Balloon Wind Multiplier", WindConfig.TREASURE_BALLOONS_WIND_MULTIPLIER.get(), WindConfig.TREASURE_BALLOONS_WIND_MULTIPLIER::set, 0, 5);

        header("Gliders / Reliable Gliders / Paraglider");
        boolTooltip("Wind Affects Gliding", WindConfig.GLIDER_WIND_ENABLED.get(), WindConfig.GLIDER_WIND_ENABLED::set,
            "Applies directional wind only while using an active glider, independently of ordinary Entity Wind. Supports Gliders, Reliable Gliders and Paraglider.");
        slider("Glider Wind Influence", WindConfig.GLIDER_WIND_INFLUENCE.get(), WindConfig.GLIDER_WIND_INFLUENCE::set, 0, 3);

        header("Entity Wind");
        bool("Enable Entity Wind (Gameplay)", WindConfig.ENTITY_WIND_ENABLED.get(), WindConfig.ENTITY_WIND_ENABLED::set);
        boolTooltip("Check Entity Shelter", WindConfig.ENTITY_WIND_CHECK_SHELTER.get(), WindConfig.ENTITY_WIND_CHECK_SHELTER::set,
            "ON (default): wind pushes entities only in exposed areas. OFF: skips per-entity shelter checks for better performance, but entities will also react to wind indoors and underground. Particle and audio shelter remain unchanged.");
        subheader("Weather-Based Entity Wind Impact");
        sliderTooltip("Clear Weather Impact", WindConfig.ENTITY_WIND_CLEAR_WEATHER_MULTIPLIER.get(),
            WindConfig.ENTITY_WIND_CLEAR_WEATHER_MULTIPLIER::set, 0, 5);
        sliderTooltip("Rain Impact", WindConfig.ENTITY_WIND_RAIN_MULTIPLIER.get(),
            WindConfig.ENTITY_WIND_RAIN_MULTIPLIER::set, 0, 5);
        sliderTooltip("Thunderstorm Impact", WindConfig.ENTITY_WIND_THUNDER_MULTIPLIER.get(),
            WindConfig.ENTITY_WIND_THUNDER_MULTIPLIER::set, 0, 5);
        subheader("Entity Types");
        bool("Affect Players", WindConfig.ENTITY_WIND_PLAYERS.get(), WindConfig.ENTITY_WIND_PLAYERS::set);
        slider("Player Grounded Wind Impact", WindConfig.ENTITY_WIND_PLAYER_GROUNDED_MULTIPLIER.get(), WindConfig.ENTITY_WIND_PLAYER_GROUNDED_MULTIPLIER::set, 0, 5);
        slider("Player Airborne Wind Impact", WindConfig.ENTITY_WIND_PLAYER_MULTIPLIER.get(), WindConfig.ENTITY_WIND_PLAYER_MULTIPLIER::set, 0, 5);
        bool("Affect Mobs", WindConfig.ENTITY_WIND_MOBS.get(), WindConfig.ENTITY_WIND_MOBS::set);
        slider("Mob Grounded Wind Impact", WindConfig.ENTITY_WIND_MOB_GROUNDED_MULTIPLIER.get(), WindConfig.ENTITY_WIND_MOB_GROUNDED_MULTIPLIER::set, 0, 5);
        slider("Mob Airborne Wind Impact", WindConfig.ENTITY_WIND_MOB_MULTIPLIER.get(), WindConfig.ENTITY_WIND_MOB_MULTIPLIER::set, 0, 5);
        bool("Affect Projectiles", WindConfig.ENTITY_WIND_PROJECTILES.get(), WindConfig.ENTITY_WIND_PROJECTILES::set);
        slider("Projectile Wind Impact", WindConfig.ENTITY_WIND_PROJECTILE_MULTIPLIER.get(), WindConfig.ENTITY_WIND_PROJECTILE_MULTIPLIER::set, 0, 5);
        nav("Projectile Wind Blacklist", () -> minecraft.setScreen(new ProjectileBlacklistScreen(this)));
        bool("Affect Boats", WindConfig.ENTITY_WIND_BOATS.get(), WindConfig.ENTITY_WIND_BOATS::set);
        slider("Boat Wind Impact", WindConfig.ENTITY_WIND_BOAT_MULTIPLIER.get(), WindConfig.ENTITY_WIND_BOAT_MULTIPLIER::set, 0, 5);
        bool("Affect Dropped Items", WindConfig.ENTITY_WIND_DROPPED_ITEMS.get(), WindConfig.ENTITY_WIND_DROPPED_ITEMS::set);
        slider("Dropped Item Wind Impact", WindConfig.ENTITY_WIND_DROPPED_ITEM_MULTIPLIER.get(), WindConfig.ENTITY_WIND_DROPPED_ITEM_MULTIPLIER::set, 0, 5);

        header("Altitude Scaling");
        bool("Altitude Scaling", WindConfig.ALTITUDE_SCALING_ENABLED.get(), WindConfig.ALTITUDE_SCALING_ENABLED::set);
        integer("Altitude Minimum Y", WindConfig.ALTITUDE_MIN_Y.get(), WindConfig.ALTITUDE_MIN_Y::set, -64, 320);
        integer("Altitude Maximum Y", WindConfig.ALTITUDE_MAX_Y.get(), WindConfig.ALTITUDE_MAX_Y::set, -64, 512);
        number("Altitude Minimum Multiplier", WindConfig.ALTITUDE_MIN_MULTIPLIER.get(), WindConfig.ALTITUDE_MIN_MULTIPLIER::set, 0, 3);
        number("Altitude Maximum Multiplier", WindConfig.ALTITUDE_MAX_MULTIPLIER.get(), WindConfig.ALTITUDE_MAX_MULTIPLIER::set, 0, 3);

        header("Networking");
        integer("Sync Interval Ticks", WindConfig.SYNC_INTERVAL_TICKS.get(), WindConfig.SYNC_INTERVAL_TICKS::set, 10, 200);

        header("Particle Wind");
        bool("Affect Existing Particles", ClientWindConfig.AFFECT_EXISTING_PARTICLES.get(), ClientWindConfig.AFFECT_EXISTING_PARTICLES::set);
        number("Particle Wind Force", ClientWindConfig.PARTICLE_WIND_FORCE.get(), ClientWindConfig.PARTICLE_WIND_FORCE::set, 0, .25);
        nav("Particle Blacklist", () -> minecraft.setScreen(new ParticleFilterListScreen(this)));
        nav("Particle Response Multipliers", () -> minecraft.setScreen(new ParticleResponseMultiplierListScreen(this)));

        header("Ambient Particles");
        nav("Ambient Pixel Block Emitters", () -> minecraft.setScreen(new AmbientEmitterListScreen(this)));
        bool("Enabled", ClientWindConfig.AMBIENT_PARTICLES.get(), ClientWindConfig.AMBIENT_PARTICLES::set);
        integer("Samples Per Tick", ClientWindConfig.AMBIENT_SAMPLES_PER_TICK.get(), ClientWindConfig.AMBIENT_SAMPLES_PER_TICK::set, 0, 32);
        integer("Sample Radius", ClientWindConfig.AMBIENT_RADIUS.get(), ClientWindConfig.AMBIENT_RADIUS::set, 4, 48);
        number("Minimum Wind Strength", ClientWindConfig.AMBIENT_MIN_WIND.get(), ClientWindConfig.AMBIENT_MIN_WIND::set, 0, 1.5);
        number("Density Multiplier", ClientWindConfig.AMBIENT_DENSITY_MULTIPLIER.get(), ClientWindConfig.AMBIENT_DENSITY_MULTIPLIER::set, 0, 4);
        number("Wind Response", ClientWindConfig.AMBIENT_WIND_RESPONSE_SCALE.get(), ClientWindConfig.AMBIENT_WIND_RESPONSE_SCALE::set, 0, 4);
        number("Upward Lift", ClientWindConfig.AMBIENT_LIFT_MULTIPLIER.get(), ClientWindConfig.AMBIENT_LIFT_MULTIPLIER::set, 0, 4);
        number("Turbulence Sway", ClientWindConfig.AMBIENT_SWAY_MULTIPLIER.get(), ClientWindConfig.AMBIENT_SWAY_MULTIPLIER::set, 0, 4);
        integer("Minimum Lifetime Ticks", ClientWindConfig.AMBIENT_LIFETIME_MIN_TICKS.get(), ClientWindConfig.AMBIENT_LIFETIME_MIN_TICKS::set, 20, 600);
        integer("Maximum Lifetime Ticks", ClientWindConfig.AMBIENT_LIFETIME_MAX_TICKS.get(), ClientWindConfig.AMBIENT_LIFETIME_MAX_TICKS::set, 20, 600);

        header("Wind Streaks");
        bool("Enable Wind Streaks", ClientWindConfig.WIND_STREAKS_ENABLED.get(), ClientWindConfig.WIND_STREAKS_ENABLED::set);
        number("Streak Density", ClientWindConfig.WIND_STREAKS_DENSITY.get(), ClientWindConfig.WIND_STREAKS_DENSITY::set, 0, 4);
        number("Minimum Wind Strength", ClientWindConfig.WIND_STREAKS_MIN_STRENGTH.get(), ClientWindConfig.WIND_STREAKS_MIN_STRENGTH::set, 0, 2.5);
        integer("Maximum Active Streaks", ClientWindConfig.WIND_STREAKS_MAX_ACTIVE.get(), ClientWindConfig.WIND_STREAKS_MAX_ACTIVE::set, 0, 80);

        header("Windblown Leaves");
        bool("Enable Windblown Leaves", ClientWindConfig.LEAF_PARTICLES_ENABLED.get(), ClientWindConfig.LEAF_PARTICLES_ENABLED::set);
        bool("Texture-Derived Colors", ClientWindConfig.LEAF_TEXTURE_COLORS.get(), ClientWindConfig.LEAF_TEXTURE_COLORS::set);
        nav("Leaf Particle Types (by Block)", () -> minecraft.setScreen(new ForgeLeafTypeMappingScreen(this,
            ClientWindConfig.LEAF_TYPE_OVERRIDES.get(), values -> {
                ClientWindConfig.LEAF_TYPE_OVERRIDES.set(values);
                ClientWindConfig.LEAF_TYPE_OVERRIDES.save();
            })));
        number("Conifer Needle Density", ClientWindConfig.CONIFER_SPAWN_MULTIPLIER.get(), ClientWindConfig.CONIFER_SPAWN_MULTIPLIER::set, 0, 1);
        number("Leaf Density", ClientWindConfig.LEAF_PARTICLE_DENSITY.get(), ClientWindConfig.LEAF_PARTICLE_DENSITY::set, 0, 4);
        integer("Leaf Sampling Radius", ClientWindConfig.LEAF_PARTICLE_RADIUS.get(), ClientWindConfig.LEAF_PARTICLE_RADIUS::set, 8, 48);
        number("Leaf Wind Response", ClientWindConfig.LEAF_PARTICLE_WIND_RESPONSE.get(), ClientWindConfig.LEAF_PARTICLE_WIND_RESPONSE::set, 0, 3);

        header("Wind Sounds");
        bool("Wind Audio Enabled", ClientWindConfig.WIND_AUDIO_ENABLED.get(), ClientWindConfig.WIND_AUDIO_ENABLED::set);
        slider("Wind Audio Volume", ClientWindConfig.WIND_AUDIO_VOLUME.get(), ClientWindConfig.WIND_AUDIO_VOLUME::set, 0, 3);
        boolTooltip("Directional Wind Audio", ClientWindConfig.WIND_AUDIO_DIRECTIONAL_ENABLED.get(),
            ClientWindConfig.WIND_AUDIO_DIRECTIONAL_ENABLED::set,
            "Adds mono upwind sound above the main stereo wind bed. Can be isolated in Debug > Wind Audio Test Mode.");
        slider("Directional Wind Volume", ClientWindConfig.WIND_AUDIO_DIRECTIONAL_VOLUME.get(),
            ClientWindConfig.WIND_AUDIO_DIRECTIONAL_VOLUME::set, 0, 3);
        bool("Gust Accents", ClientWindConfig.WIND_AUDIO_GUSTS.get(), ClientWindConfig.WIND_AUDIO_GUSTS::set);
        bool("Environmental Accents", ClientWindConfig.WIND_AUDIO_ENVIRONMENTAL.get(), ClientWindConfig.WIND_AUDIO_ENVIRONMENTAL::set);
        enumAudioMode();
        enumDynamicSurroundingsAudioMode();
        enumAmbiWindMode();

        header("Seasonal Compatibility (Particles)");
        bool("Seasonal Particles", ClientWindConfig.SEASONAL_PARTICLES_ENABLED.get(), ClientWindConfig.SEASONAL_PARTICLES_ENABLED::set);
        number("Seasonal Particle Intensity", ClientWindConfig.SEASONAL_PARTICLES_INTENSITY.get(), ClientWindConfig.SEASONAL_PARTICLES_INTENSITY::set, 0, 2);
        bool("Winter Snow Effects", ClientWindConfig.SEASONAL_SNOW_ENABLED.get(), ClientWindConfig.SEASONAL_SNOW_ENABLED::set);
        enumEclipticLeavesSource();

        header("Pretty Rain");
        bool("Pretty Rain Wind", ClientWindConfig.PRETTY_RAIN_COMPAT.get(), ClientWindConfig.PRETTY_RAIN_COMPAT::set);
        number("Pretty Rain Wind Strength", ClientWindConfig.PRETTY_RAIN_WIND_MULTIPLIER.get(), ClientWindConfig.PRETTY_RAIN_WIND_MULTIPLIER::set, 0, 2);
        header("Particle Rain");
        bool("Particle Rain v4 Wind", ClientWindConfig.PARTICLE_RAIN_COMPAT.get(), ClientWindConfig.PARTICLE_RAIN_COMPAT::set);
        number("Particle Rain v4 Strength", ClientWindConfig.PARTICLE_RAIN_WIND_MULTIPLIER.get(), ClientWindConfig.PARTICLE_RAIN_WIND_MULTIPLIER::set, 0, 2);
        header("Particular");
        enumFallingLeavesSource();
        bool("Particular Leaf Density Follows Wind", ClientWindConfig.PARTICULAR_FALLING_LEAVES_COMPAT.get(), ClientWindConfig.PARTICULAR_FALLING_LEAVES_COMPAT::set);
        number("Particular Min Leaf Density", ClientWindConfig.PARTICULAR_LEAF_MIN_DENSITY.get(), ClientWindConfig.PARTICULAR_LEAF_MIN_DENSITY::set, 0, 4);
        number("Particular Max Leaf Density", ClientWindConfig.PARTICULAR_LEAF_MAX_DENSITY.get(), ClientWindConfig.PARTICULAR_LEAF_MAX_DENSITY::set, 0, 8);
        number("Particular Full-Wind Strength", ClientWindConfig.PARTICULAR_LEAF_FULL_WIND_STRENGTH.get(), ClientWindConfig.PARTICULAR_LEAF_FULL_WIND_STRENGTH::set, .1, 3);

        header("Advanced Chimneys");
        bool("Outdoor Smoke Wind", ClientWindConfig.ADVANCED_CHIMNEYS_WIND_COMPAT.get(), ClientWindConfig.ADVANCED_CHIMNEYS_WIND_COMPAT::set);
        header("Supplementaries");
        bool("Supplementaries Wind Vane", ClientWindConfig.SUPPLEMENTARIES_WIND_VANE_COMPAT.get(), ClientWindConfig.SUPPLEMENTARIES_WIND_VANE_COMPAT::set);
        header("Swinging Hanging Signs");
        bool("Wind-Driven Hanging Signs", ClientWindConfig.SWINGING_SIGNS_COMPAT.get(), ClientWindConfig.SWINGING_SIGNS_COMPAT::set);
        number("Hanging Signs Wind Strength", ClientWindConfig.SWINGING_SIGNS_WIND_MULTIPLIER.get(), ClientWindConfig.SWINGING_SIGNS_WIND_MULTIPLIER::set, 0, 3);
        header("Swinging Lanterns");
        bool("Swinging Lanterns Wind", ClientWindConfig.SWINGING_LANTERNS_COMPAT.get(), ClientWindConfig.SWINGING_LANTERNS_COMPAT::set);
        number("Swinging Lanterns Wind Strength", ClientWindConfig.SWINGING_LANTERNS_WIND_MULTIPLIER.get(), ClientWindConfig.SWINGING_LANTERNS_WIND_MULTIPLIER::set, 0, 3);
        header("Chimes");
        bool("Wind-Driven Chimes", ClientWindConfig.CHIMES_WIND_COMPAT.get(), ClientWindConfig.CHIMES_WIND_COMPAT::set);
        number("Chimes Animation Wind", ClientWindConfig.CHIMES_ANIMATION_WIND_MULTIPLIER.get(), ClientWindConfig.CHIMES_ANIMATION_WIND_MULTIPLIER::set, 0, 5);
        number("Chimes Sound Wind", ClientWindConfig.CHIMES_SOUND_WIND_MULTIPLIER.get(), ClientWindConfig.CHIMES_SOUND_WIND_MULTIPLIER::set, 0, 3);
        header("Fancy World Animations");
        bool("FWA Wind-Driven Swings", ClientWindConfig.FWA_WIND_COMPAT.get(), ClientWindConfig.FWA_WIND_COMPAT::set);
        slider("FWA Wind Impact", ClientWindConfig.FWA_WIND_IMPACT.get(), ClientWindConfig.FWA_WIND_IMPACT::set, 0, 12);
        number("FWA Chain Wind", ClientWindConfig.FWA_CHAIN_INTENSITY.get(), ClientWindConfig.FWA_CHAIN_INTENSITY::set, 0, 3);
        number("FWA Lantern Wind", ClientWindConfig.FWA_LANTERN_INTENSITY.get(), ClientWindConfig.FWA_LANTERN_INTENSITY::set, 0, 3);


        header("Windblown");
        bool("Windblown Wind Integration", ClientWindConfig.WINDBLOWN_WIND_COMPAT.get(), ClientWindConfig.WINDBLOWN_WIND_COMPAT::set);
        number("Windblown Wind Multiplier", ClientWindConfig.WINDBLOWN_WIND_MULTIPLIER.get(), ClientWindConfig.WINDBLOWN_WIND_MULTIPLIER::set, 0, 5);

        header("Interactive Foliage");
        bool("Interactive Foliage Wind", ClientWindConfig.INTERACTIVE_FOLIAGE_WIND_COMPAT.get(), ClientWindConfig.INTERACTIVE_FOLIAGE_WIND_COMPAT::set);

        header("Exposure");
        bool("Reduce Wind Indoors", ClientWindConfig.REDUCE_WIND_INDOORS.get(), ClientWindConfig.REDUCE_WIND_INDOORS::set);
        number("Shelter Effectiveness", ClientWindConfig.SHELTER_EFFECTIVENESS.get(), ClientWindConfig.SHELTER_EFFECTIVENESS::set, 0, 1);
        number("Indoor Sound Attenuation", ClientWindConfig.INDOOR_AUDIO_ATTENUATION.get(), ClientWindConfig.INDOOR_AUDIO_ATTENUATION::set, 0, 1);
        bool("Low-Cost Shelter Checks", ClientWindConfig.SHELTER_LOW_COST.get(), ClientWindConfig.SHELTER_LOW_COST::set);

        header("Debug");
        enumDebugHudMode();
        enumWindAudioDebugMode();

        // GUI navigation only: all config keys, validators, and persisted values stay put.
        final java.util.List<String> clientIntegrations = java.util.List.of(
            "Seasonal Compatibility (Particles)", "Pretty Rain", "Particle Rain", "Particular", "Advanced Chimneys",
            "Supplementaries", "Swinging Hanging Signs", "Swinging Lanterns", "Chimes", "Fancy World Animations",
            "Windblown", "Interactive Foliage");

        String section = "";
        java.util.List<Row> hidden = new ArrayList<>();
        for (Row row : rows) {
            if (row.header && !row.subheader) section = row.label.getString();
            boolean show = page.equals(section);  // leaf pages and normal settings pages
            if (!show) hidden.add(row);
        }
        for (Row row : hidden) {
            rows.remove(row);
            if (row.widget != null) removeWidget(row.widget);
        }
        if ("main".equals(page)) {
            nav("Common Options", () -> openPage("common"));
            nav("Client Options", () -> openPage("client"));
        } else if ("common".equals(page)) {
            for (String category : new String[] {"Wind Simulation", "Weather", "Wind Events",
                    "Altitude Scaling"}) {
                nav(category, () -> openPage(category));
            }
            nav("Dimension Settings", () -> minecraft.setScreen(new DimensionSettingsScreen(this)));
            nav("Entity Wind", () -> openPage("Entity Wind"));
            nav("Networking", () -> openPage("Networking"));
            nav("Compatibility", () -> openPage("commonCompatibility"));
        } else if ("client".equals(page)) {
            for (String category : new String[] {"Particle Wind", "Ambient Particles",
                    "Wind Streaks", "Windblown Leaves", "Wind Sounds", "Exposure", "Debug"}) {
                nav(category, () -> openPage(category));
            }
            nav("Compatibility", () -> openPage("clientCompatibility"));
        } else if ("commonCompatibility".equals(page)) {
            for (String integration : new String[] {"Seasonal Compatibility (Wind)", "Create Windmills", "Valkyrien Skies 2 / Eureka / Clockwork", "Gliders / Reliable Gliders / Paraglider", "Small Ships 2.0", "Aleki's Nifty Ships", "Treasure Balloons"}) {
                nav(integration, () -> openPage(integration));
            }
        } else if ("clientCompatibility".equals(page)) {
            for (String integration : clientIntegrations) {
                nav(integration, () -> openPage(integration));
            }
        }
        contentHeight = rows.size() * ROW_H;
        scroll = clamp(scroll);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose()).bounds(width / 2 - 75, height - 28, 150, 20).build());
        updatePositions();
    }

    private void header(String s) { rows.add(Row.header(Component.literal(s).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))); }
    // A visual subsection stays on its parent page instead of becoming a separate navigation page.
    private void subheader(String s) { rows.add(Row.subheader(Component.literal(s).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))); }
    private void bool(String label, boolean initial, Consumer<Boolean> setter) {
        boolTooltip(label, initial, setter, null);
    }
    private void boolTooltip(String label, boolean initial, Consumer<Boolean> setter, String help) {
        final boolean[] v={initial};
        Button.Builder builder=Button.builder(onOff(v[0]), x->{v[0]=!v[0]; setter.accept(v[0]); x.setMessage(onOff(v[0]));}).bounds(0,0,180,20);
        if (help != null) builder.tooltip(Tooltip.create(Component.literal(help)));
        Button b=builder.build();
        rows.add(Row.value(Component.literal(label), b, null)); addRenderableWidget(b);
    }
    private void number(String label, double initial, Consumer<Double> setter, double min, double max) {
        EditBox box=new EditBox(font,0,0,180,20,Component.literal(label)); box.setValue(Double.toString(initial));
        box.setFilter(LivingWindConfigScreen::partialDecimal);
        rows.add(Row.value(Component.literal(label), box, ()->{try{setter.accept(Math.max(min,Math.min(max,Double.parseDouble(box.getValue()))));}catch(Exception ignored){}})); addRenderableWidget(box);
    }
    private void integer(String label, int initial, Consumer<Integer> setter, int min, int max) {
        EditBox box=new EditBox(font,0,0,180,20,Component.literal(label)); box.setValue(Integer.toString(initial));
        box.setFilter(v->v.isEmpty()||v.equals("-")||v.matches("-?\\d+"));
        rows.add(Row.value(Component.literal(label), box, ()->{try{setter.accept(Math.max(min,Math.min(max,Integer.parseInt(box.getValue()))));}catch(Exception ignored){}})); addRenderableWidget(box);
    }
    private void slider(String label, double initial, Consumer<Double> setter, double min, double max) {
        double normalized = max <= min ? 0.0 : Math.max(0.0, Math.min(1.0, (initial - min) / (max - min)));
        // A named slider class avoids javac's duplicate synthetic-local error on
        // Forge 1.20.1 while preserving the same UI and update behavior.
        AbstractSliderButton slider = new ConfigSlider(initial, normalized, setter, min, max);
        rows.add(Row.value(Component.literal(label), slider, null));
        addRenderableWidget(slider);
    }

    private void sliderTooltip(String label, double initial, Consumer<Double> setter, double min, double max) {
        double normalized = max <= min ? 0.0 : Math.max(0.0, Math.min(1.0, (initial - min) / (max - min)));
        AbstractSliderButton slider = new ConfigSlider(initial, normalized, setter, min, max);
        slider.setTooltip(Tooltip.create(Component.literal(
            "Scales all enabled entity wind effects only (players, mobs, projectiles, boats and items). " +
            "0 = no push in this weather, 1.0 = unchanged, higher = stronger. " +
            "Blends smoothly between weather states. Does not change particles or the shared Wind API.")));
        rows.add(Row.value(Component.literal(label), slider, null));
        addRenderableWidget(slider);
    }

    private void percentageSlider(String label, double initial, Consumer<Double> setter) {
        AbstractSliderButton slider = new ConfigSlider(initial, Math.max(0.0, Math.min(1.0, initial)),
            setter, 0.0, 1.0, true);
        slider.setTooltip(Tooltip.create(Component.literal(
            "0% = steady ordinary wind, 100% = full natural variation. With custom weather ranges enabled, " +
            "this controls variation inside each range. Gusts, pre-storm buildup and wind direction remain independent.")));
        rows.add(Row.value(Component.literal(label), slider, null));
        addRenderableWidget(slider);
    }

    private void enumWindAudioDebugMode() {
        Button button = Button.builder(Component.literal(audioTestLabel(ClientWindConfig.WIND_AUDIO_DEBUG_MODE.get())), b -> {
            var modes = ClientWindConfig.WindAudioDebugMode.values();
            var current = ClientWindConfig.WIND_AUDIO_DEBUG_MODE.get();
            var next = modes[(current.ordinal() + 1) % modes.length];
            ClientWindConfig.WIND_AUDIO_DEBUG_MODE.set(next);
            b.setMessage(Component.literal(audioTestLabel(next)));
        }).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "NORMAL: regular soundscape. STEREO ONLY: wind beds without positional audio or accents. " +
              "DIRECTIONAL ONLY: just upwind mono wind and spatial gusts, even if the directional toggle is off. " +
              "Use headphones; reset to NORMAL after testing.")))
          .build();
        rows.add(Row.value(Component.literal("Wind Audio Test Mode"), button, null));
        addRenderableWidget(button);
    }

    private static String audioTestLabel(ClientWindConfig.WindAudioDebugMode mode) {
        return switch (mode) {
            case NORMAL -> "Normal";
            case STEREO_ONLY -> "Stereo Only";
            case DIRECTIONAL_ONLY -> "Directional Only";
        };
    }

    private void enumDebugHudMode() {
        Button button = Button.builder(Component.literal(ClientWindConfig.debugHudMode().name()), b -> {
            ClientWindConfig.DebugHudMode[] modes = ClientWindConfig.DebugHudMode.values();
            ClientWindConfig.DebugHudMode current = ClientWindConfig.debugHudMode();
            ClientWindConfig.DebugHudMode next = modes[(current.ordinal() + 1) % modes.length];
            ClientWindConfig.setDebugHudMode(next);
            b.setMessage(Component.literal(next.name()));
        }).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal("OFF: hidden. SIMPLE: wind strength, direction and gusts. ALL: full diagnostic overlay.")))
          .build();
        rows.add(Row.value(Component.literal("Debug HUD"), button, null));
        addRenderableWidget(button);
    }

    private void enumEclipticLeavesSource() {
        Button button = Button.builder(Component.literal(eclipticLeavesLabel(ClientWindConfig.ECLIPTIC_LEAVES_SOURCE.get())), b -> {
            var modes = ClientWindConfig.EclipticLeavesSource.values();
            var current = ClientWindConfig.ECLIPTIC_LEAVES_SOURCE.get();
            var next = modes[(current.ordinal() + 1) % modes.length];
            ClientWindConfig.ECLIPTIC_LEAVES_SOURCE.set(next);
            b.setMessage(Component.literal(eclipticLeavesLabel(next)));
        }).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "Living Wind Only (default): disables Ecliptic falling leaves. Ecliptic Seasons Only: disables Living Wind tree leaves. Both: both. Other seasonal particles stay enabled. If Ecliptic is absent, Living Wind leaves remain.")))
          .build();
        rows.add(Row.value(Component.literal("Ecliptic Falling Leaves"), button, null));
        addRenderableWidget(button);
    }

    private static String eclipticLeavesLabel(ClientWindConfig.EclipticLeavesSource source) {
        return switch (source) {
            case LIVING_WIND -> "Living Wind Only";
            case ECLIPTIC_SEASONS -> "Ecliptic Seasons Only";
            case BOTH -> "Both";
        };
    }

    private void enumFallingLeavesSource() {
        Button b = Button.builder(Component.literal(fallingLeavesLabel(ClientWindConfig.FALLING_LEAVES_SOURCE.get())), x -> {
            var values = ClientWindConfig.FallingLeavesSource.values();
            int next = (ClientWindConfig.FALLING_LEAVES_SOURCE.get().ordinal() + 1) % values.length;
            ClientWindConfig.FALLING_LEAVES_SOURCE.set(values[next]);
            x.setMessage(Component.literal(fallingLeavesLabel(values[next])));
        }).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "Living Wind Only (default): disables Particular leaves. Particular Only: disables Living Wind tree leaves. Both: both emit leaves. Flowers and grass stay enabled. If Particular is missing, Living Wind leaves remain active.")))
          .build();
        rows.add(Row.value(Component.literal("Falling Leaves Source"), b, null));
        addRenderableWidget(b);
    }

    private static String fallingLeavesLabel(ClientWindConfig.FallingLeavesSource source) {
        return switch (source) {
            case LIVING_WIND -> "Living Wind Only";
            case PARTICULAR -> "Particular Only";
            case BOTH -> "Both";
        };
    }

    private void enumAudioMode() {
        Button b=Button.builder(Component.literal(ClientWindConfig.WIND_AUDIO_MODE.get().name()), x->{
            var values=ClientWindConfig.WindAudioMode.values();
            int next=(ClientWindConfig.WIND_AUDIO_MODE.get().ordinal()+1)%values.length;
            ClientWindConfig.WIND_AUDIO_MODE.set(values[next]); x.setMessage(Component.literal(values[next].name()));
        }).bounds(0,0,180,20)
          .tooltip(Tooltip.create(Component.literal("Choose which mod supplies wind audio when Ambient Sounds is installed. LIVING_WIND - Plays only Living Wind's wind sounds. AMBIENT_SOUNDS - Plays only Ambient Sounds's wind sounds. BOTH - Plays both wind sound systems.")))
          .build(); rows.add(Row.value(Component.literal("Ambient Sounds Wind"),b,null)); addRenderableWidget(b);
    }
    private void enumDynamicSurroundingsAudioMode() {
        Button button = Button.builder(
            Component.literal(dynamicSurroundingsAudioLabel(ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get())),
            widget -> {
                var modes = ClientWindConfig.DynamicSurroundingsWindMode.values();
                int next = (ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get().ordinal() + 1) % modes.length;
                ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.set(modes[next]);
                widget.setMessage(Component.literal(dynamicSurroundingsAudioLabel(modes[next])));
            }
        ).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "Choose Living Wind only (default), Dynamic Surroundings only, or both wind soundtracks. " +
              "Other Dynamic Surroundings audio remains unchanged. Ambient Sounds is configured separately.")))
          .build();
        rows.add(Row.value(Component.literal("Dynamic Surroundings Wind"), button, null));
        addRenderableWidget(button);
    }

    private static String dynamicSurroundingsAudioLabel(ClientWindConfig.DynamicSurroundingsWindMode mode) {
        // Match NeoForge's enum buttons and the Ambient Sounds selector on Forge.
        return mode.name();
    }

    private void enumAmbiWindMode() {
        Button button = Button.builder(
            Component.literal(ambiWindAudioLabel(ClientWindConfig.AMBI_WIND_MODE.get())),
            widget -> {
                var modes = ClientWindConfig.AmbiWindMode.values();
                int next = (ClientWindConfig.AMBI_WIND_MODE.get().ordinal() + 1) % modes.length;
                ClientWindConfig.AMBI_WIND_MODE.set(modes[next]);
                widget.setMessage(Component.literal(ambiWindAudioLabel(modes[next])));
            }
        ).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "Choose Living Wind only (default), Ambi only, or both wind soundtracks. " +
              "Other Ambi ambience remains unchanged. Ambient Sounds and Dynamic Surroundings are configured separately.")))
          .build();
        rows.add(Row.value(Component.literal("Ambi Wind"), button, null));
        addRenderableWidget(button);
    }

    private static String ambiWindAudioLabel(ClientWindConfig.AmbiWindMode mode) {
        return mode.name();
    }

    private void enumNetherWindMode() {
        Button b=Button.builder(Component.literal(WindConfig.NETHER_WIND_MODE.get().name()), x->{
            var values=WindConfig.NetherWindMode.values();
            int next=(WindConfig.NETHER_WIND_MODE.get().ordinal()+1)%values.length;
            WindConfig.NETHER_WIND_MODE.set(values[next]); x.setMessage(Component.literal(values[next].name()));
        }).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal("Nether Wind Mode"),b,null)); addRenderableWidget(b);
    }
    private void enumEndWindMode() {
        Button b=Button.builder(Component.literal(WindConfig.END_WIND_MODE.get().name()), x->{
            var values=WindConfig.EndWindMode.values();
            int next=(WindConfig.END_WIND_MODE.get().ordinal()+1)%values.length;
            WindConfig.END_WIND_MODE.set(values[next]); x.setMessage(Component.literal(values[next].name()));
        }).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal("End Wind Mode"),b,null)); addRenderableWidget(b);
    }
    private void nav(String label, Runnable action) {
        Button b=Button.builder(Component.literal("Configure"), x->action.run()).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal(label),b,null)); addRenderableWidget(b);
    }

    private void openPage(String key) {
        commitAndSave();
        minecraft.setScreen(new LivingWindConfigScreen(this, key));
    }

    private void commitAndSave(){ for(Row r:rows) if(r.commit!=null) r.commit.run(); ParticleWindFilter.refresh(); ClientWindConfig.WIND_AUDIO_VOLUME.save(); WindConfig.NETHER_WIND_MODE.save(); }
    private void saveAndClose(){ commitAndSave(); minecraft.setScreen(parent); }
    @Override public void onClose(){ commitAndSave(); minecraft.setScreen(parent); }
    @Override public boolean mouseScrolled(double mx,double my,double delta){ if(my>=TOP&&my<height-BOTTOM&&maxScroll()>0){scroll=clamp(scroll-delta*ROW_H);updatePositions();return true;} return super.mouseScrolled(mx,my,delta); }
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int labelX = labelLeft;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = rowY(i);
            if (y + ROW_H <= TOP || y >= height - BOTTOM) continue;
            if (row.header) {
                g.drawCenteredString(font, row.label, width / 2, y + 8, 0xFFFF55);
            } else {
                g.drawString(font, row.label, labelX, y + 8, 0xFFFFFF, false);
            }
        }
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        drawBar(g);
    }

    /** Match NeoForge's compact centered columns instead of stretching across the whole screen. */
    private int controlX() { return width / 2 + 4; }
    private int controlWidth() { return Math.min(150, Math.max(70, width / 2 - 18)); }
    private int labelX() {
        int longest = 0;
        for (Row row : rows) {
            if (!row.header) longest = Math.max(longest, font.width(row.label));
        }
        return Math.max(8, width / 2 - Math.max(155, longest + 14));
    }

    private void updatePositions() {
        labelLeft = labelX();
        int cx = controlX(), cw = controlWidth(), bottom = height - BOTTOM;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.widget == null) continue;
            int y = rowY(i);
            row.widget.setX(cx);
            row.widget.setY(y + 2);
            row.widget.setWidth(cw);
            boolean visible = y >= TOP && y + ROW_H <= bottom;
            row.widget.visible = visible;
            row.widget.active = visible;
        }
    }

    private void drawBar(GuiGraphics g) {
        double max = maxScroll();
        if (max <= 0) return;
        int top = TOP, bottom = height - BOTTOM, visible = bottom - top;
        int thumb = Math.max(20, (int) ((double) visible * visible / contentHeight));
        int y = top + (int) ((scroll / max) * (visible - thumb));
        int x = controlX() + controlWidth() + 9;
        g.fill(x, top, x + 4, bottom, 0x40202020);
        g.fill(x, y, x + 4, y + thumb, 0xFFA0A0A0);
    }

    private int rowY(int i) { return TOP + i * ROW_H - (int) Math.round(scroll); }
    private double maxScroll() { return Math.max(0, contentHeight - (height - BOTTOM - TOP)); }
    private double clamp(double v) { return Math.max(0, Math.min(maxScroll(), v)); }
    private static Component onOff(boolean v){return Component.translatable(v?"options.on":"options.off");}
    private static boolean partialDecimal(String v){return v.isEmpty()||v.equals("-")||v.equals(".")||v.equals("-.")||v.matches("-?(?:\\d+\\.?\\d*|\\.\\d*)");}
    private static final class ConfigSlider extends AbstractSliderButton {
        private final Consumer<Double> setter;
        private final double min;
        private final double max;
        private final boolean percent;

        private ConfigSlider(double initial, double normalized, Consumer<Double> setter, double min, double max) {
            this(initial, normalized, setter, min, max, false);
        }

        private ConfigSlider(double initial, double normalized, Consumer<Double> setter, double min, double max, boolean percent) {
            super(0, 0, 180, 20, Component.literal(format(initial, percent)), normalized);
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.percent = percent;
        }

        private static String format(double actual, boolean percent) {
            return percent ? String.format(Locale.ROOT, "%.0f%%", actual * 100.0)
                : String.format(Locale.ROOT, "%.2f", actual);
        }

        @Override protected void updateMessage() {
            double actual = min + (max - min) * value;
            setMessage(Component.literal(format(actual, percent)));
        }

        @Override protected void applyValue() {
            double actual = min + (max - min) * value;
            actual = Math.round(actual * 100.0) / 100.0;
            setter.accept(actual);
            updateMessage();
        }
    }

    private static final class Row {
        final Component label;
        final AbstractWidget widget;
        final Runnable commit;
        final boolean header, subheader;
        private Row(Component label, AbstractWidget widget, Runnable commit, boolean header, boolean subheader) {
            this.label = label;
            this.widget = widget;
            this.commit = commit;
            this.header = header;
            this.subheader = subheader;
        }
        static Row header(Component label) { return new Row(label, null, null, true, false); }
        static Row subheader(Component label) { return new Row(label, null, null, true, true); }
        static Row value(Component label, AbstractWidget widget, Runnable commit) {
            return new Row(label, widget, commit, false, false);
        }
    }
}
