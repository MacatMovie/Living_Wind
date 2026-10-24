package com.macatmovie.livingwind.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Versioned, read-only particle exclusions. User additions are kept separately in the
 * existing particleWind.filter config key, so future releases can add defaults without
 * rewriting or resetting a player's saved custom list.
 */
public final class BuiltinParticleBlacklist {
    public static final List<String> IDS = List.of(
        "minecraft:block_marker",
        "minecraft:elder_guardian",
        "minecraft:explosion_emitter",
        "minecraft:flash",
        "particlerain:streak",
        "particlerain:ground_fog",
        "minecraft:dripping_water",
        "cosycritters:bird",
        "cosycritters:hat_man",
        "cosycritters:moth",
        "cosycritters:spider",
        "minecraft:dripping_honey",
        "minecraft:landing_honey",
        "minecraft:angry_villager",
        "minecraft:happy_villager",
        "minecraft:block",
        "minecraft:heart",
        "minecraft:wax_on",
        "minecraft:wax_off",
        // Vanilla: source-anchored droplets (falling water/lava is intentionally NOT
        // excluded), fluid-surface animations, and underwater visual paths.
        "minecraft:dripping_dripstone_water",
        "minecraft:dripping_dripstone_lava",
        "minecraft:dripping_lava",
        "minecraft:dripping_obsidian_tear",
        "minecraft:landing_lava",
        "minecraft:landing_obsidian_tear",
        "minecraft:bubble",
        "minecraft:bubble_column_up",
        "minecraft:bubble_pop",
        "minecraft:current_down",
        "minecraft:underwater",
        "minecraft:fishing",
        "minecraft:splash",
        "minecraft:rain",
        "minecraft:dolphin",
        "minecraft:squid_ink",
        "minecraft:glow_squid_ink",
        // Vanilla: player status and potion effects are feedback, not airborne dust.
        // ambient_entity_effect exists on older Minecraft versions; absent IDs are safe.
        "minecraft:effect",
        "minecraft:instant_effect",
        "minecraft:dragon_breath",
        "minecraft:entity_effect",
        "minecraft:ambient_entity_effect",
        "minecraft:witch",
        "minecraft:infested",
        "minecraft:item_cobweb",
        "minecraft:item_slime",
        "minecraft:raid_omen",
        "minecraft:trial_omen",
        "minecraft:small_gust",
        // Vanilla: tactical indicators, anchored connections and self-contained
        // animations must stay where their gameplay events occurred.
        "minecraft:note",
        "minecraft:composter",
        "minecraft:dust",
        "minecraft:dust_color_transition",
        "minecraft:enchant",
        "minecraft:nautilus",
        "minecraft:damage_indicator",
        "minecraft:crit",
        "minecraft:enchanted_hit",
        "minecraft:sweep_attack",
        "minecraft:egg_crack",
        "minecraft:dust_pillar",
        "minecraft:sculk_charge",
        "minecraft:sculk_charge_pop",
        "minecraft:sculk_soul",
        "minecraft:shriek",
        "minecraft:vibration",
        "minecraft:sonic_boom",
        "minecraft:trial_spawner_detection",
        "minecraft:trial_spawner_detection_ominous",
        "minecraft:vault_connection",
        "minecraft:ominous_spawning",
        "minecraft:totem_of_undying",
        "minecraft:gust",
        "minecraft:gust_emitter",
        "minecraft:explosion",
        "minecraft:portal",
        "minecraft:reverse_portal",
        // Effectual: minecart wheel sparks have a scripted wheel-contact burst;
        // footprints and snow residue are ground decals. Glows are tethered to
        // light sources, air trails follow Elytra flight, and post-swim droplets
        // initially follow the player's body. Keep genuinely airborne Effectual
        // dust, furnace/fire sparks, embers, and breath steam wind-responsive.
        "effectual:metal_spark",
        "effectual:snow_footprint",
        "effectual:snow_residue",
        "effectual:gold_glow",
        "effectual:soul_glow",
        "effectual:air_trail",
        "effectual:water_drip",
        // Create: encased-fan airflow and separately animated air paths. Wind is
        // already simulated by Create and must not distort these trajectories.
        "create:air_flow",
        "create:air",
        // Create's HUD-type animation and wireless-network indicators are anchored.
        "create:rotation_indicator",
        "create:wifi",
        // Particle Effects Reforged registers an effect-specific family at runtime;
        // see installedIds() below to cover the complete installed set.
        // Wavify: protect independently simulated wave spray and splashes.
        "wavify:spray_particle",
        "wavify:white_spray_particle",
        "wavify:splash",
        "wavify:bigsplash",
        // Particular: water surface effects and water cascade/splash animations are
        // controlled by Particular itself, not by horizontal atmospheric wind.
        "particular:water_ripple",
        "particular:waterfall_spray",
        "particular:cascade",
        "particular:water_splash_emitter",
        "particular:water_splash",
        "particular:water_splash_foam",
        "particular:water_splash_ring",
        "particular:firefly",
        // Subtle Effects: water animations, autonomous fireflies, static/attached effects
        // and gameplay indicators. Keep airborne smoke, pollen, spores, dust, feathers,
        // snow and petals wind-responsive.
        // Water / fluid-surface physics.
        "subtle_effects:splash",
        "subtle_effects:splash_emitter",
        "subtle_effects:splash_ripple",
        "subtle_effects:ripple",
        "subtle_effects:droplet",
        "subtle_effects:waterfall_cloud",
        "subtle_effects:waterfall_droplet",
        "subtle_effects:waterfall_mist",
        "subtle_effects:drowning_bubble",
        "subtle_effects:drowning_bubble_pop",
        "subtle_effects:geyser_spout",
        // Autonomous firefly motion.
        "subtle_effects:firefly",
        "subtle_effects:vanilla_firefly",
        "subtle_effects:firefly_emitter",
        // Fixed-position, attached or signaling animations.
        "subtle_effects:fallen_leaf",
        "subtle_effects:block_no_momentum",
        "subtle_effects:snoring",
        "subtle_effects:falling_snoring",
        "subtle_effects:command_block",
        "subtle_effects:beacon",
        "subtle_effects:item_rarity",
        "subtle_effects:end_portal",
        "subtle_effects:end_crystal_magic",
        "subtle_effects:ender_eye_placed_ring",
        "subtle_effects:ominous_vault_connection",
        "subtle_effects:potion_ring",
        "subtle_effects:potion_dot",
        "subtle_effects:potion_emitter",
        "subtle_effects:heart_pop",
        "subtle_effects:heart_growth",
        "subtle_effects:experience",
        "subtle_effects:slime_trail",
        "subtle_effects:magma_cube_trail",
        "subtle_effects:egg_splat",
        "subtle_effects:snow_splat",
        // Streams Reflowing: all five particles follow the stream's own water
        // flow simulation, even the floating leaf and stick variants.
        "streamsreflowing:stream_bubble",
        "streamsreflowing:stream_splash",
        "streamsreflowing:stream_foam",
        "streamsreflowing:stream_leaf",
        "streamsreflowing:stream_stick",
        // Explosive Enhancement: cohesive explosion sequences, sparks and shockwaves
        // should retain the motion specified by that effect.
        "explosiveenhancement:blastwave",
        "explosiveenhancement:fireball",
        "explosiveenhancement:blank_fireball",
        "explosiveenhancement:smoke",
        "explosiveenhancement:sparks",
        "explosiveenhancement:bubble",
        "explosiveenhancement:shockwave",
        "explosiveenhancement:blank_shockwave",
        "explosiveenhancement:underwaterblastwave",
        "explosiveenhancement:underwatersparks",
        // Scorched Guns: bullet holes are stationary block-surface decals, not airborne particles.
        "scguns:bullet_hole",
        // Footprint Particle: fixed decals, snow kick-up and water interaction effects.
        "footprintparticle:footprint",
        "footprintparticle:watermark",
        "footprintparticle:snowdust",
        "footprintparticle:watersplash",
        // Spawn (Ninni): insects and creature-driven effects have their own AI-like
        // movement, model animation, or positions tied to an entity/block surface.
        // Generic wind offsets make flies, dragonflies, and pond skaters jitter.
        // The pond skaters were added in Spawn 4.0.8 for 1.21.1; missing IDs on
        // Forge 1.20.1 are harmless. Keep sand/sea/volcano smoke wind-responsive.
        "spawn:fly",
        "spawn:wandering_fly",
        "spawn:orbiting_fly",
        "spawn:spring_dragonfly",
        "spawn:summer_dragonfly",
        "spawn:fall_dragonfly",
        "spawn:winter_dragonfly",
        "spawn:clear_pond_skater",
        "spawn:murky_pond_skater",
        // Animal-attached glows, eggs, footsteps, and effect animations.
        "spawn:angler_fish_lantern_glow",
        "spawn:tuna_egg",
        "spawn:iguana_sweep",
        "spawn:barnacle_puff",
        "spawn:booby_footstep",
        "spawn:blue_footed_boot_footstep",
        "spawn:angry",
        "spawn:frenzy",
        "spawn:stink",
        // Source-defined flash/blast sequences should keep their intended paths.
        "spawn:volcano_flare",
        "spawn:sun_flare",
        "spawn:sun_blast",
        // Visuality: crystal sparkles, slime drops, mob hit fragments, and water rings.
        // Some Visuality versions register these in a separate custom registry.
        // Matching particle classes are also protected by BuiltinParticleClassBlacklist.
        "visuality:sparkle",
        "visuality:small_slime_blob",
        "visuality:medium_slime_blob",
        "visuality:big_slime_blob",
        "visuality:bone",
        "visuality:wither_bone",
        "visuality:feather",
        "visuality:charge",
        "visuality:emerald",
        "visuality:water_circle"
    );

    private static final Set<String> LOOKUP = Set.copyOf(IDS);

    private BuiltinParticleBlacklist() {}

    /**
     * Particle Effects Reforged (NeoForge 1.21.1) registers a particle for every
     * vanilla status effect. These are feedback visuals, never free-floating dust.
     * Checking the namespace here makes the in-game picker label them as built-ins.
     */
    public static boolean contains(String id) {
        return LOOKUP.contains(id) || id.startsWith("particle_effects:");
    }

    /**
     * Called when refreshing config or opening the filter screen, never in the
     * per-particle movement hook. Optional mods are not loaded or linked here.
     */
    public static List<String> installedIds() {
        List<String> result = new ArrayList<>(IDS);
        for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            if ("particle_effects".equals(id.getNamespace())) result.add(id.toString());
        }
        return result;
    }
}
