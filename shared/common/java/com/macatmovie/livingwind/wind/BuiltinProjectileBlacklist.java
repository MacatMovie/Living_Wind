package com.macatmovie.livingwind.wind;

import java.util.List;
import java.util.Set;

/** Registry IDs of scripted projectiles protected from gameplay wind by default. */
public final class BuiltinProjectileBlacklist {
    public static final List<String> IDS = List.of(
        // Iron's Spells 'n Spellbooks
        "irons_spellbooks:magic_missile",
        "irons_spellbooks:cone_of_cold",
        "irons_spellbooks:blood_slash",
        "irons_spellbooks:electrocute",
        "irons_spellbooks:firebolt",
        "irons_spellbooks:icicle",
        "irons_spellbooks:fire_breath",
        "irons_spellbooks:dragon_breath",
        "irons_spellbooks:lightning_lance",
        "irons_spellbooks:wither_skull",
        "irons_spellbooks:creeper_head",
        "irons_spellbooks:small_fireball",
        "irons_spellbooks:fireball",
        "irons_spellbooks:ice_block_projectile",
        "irons_spellbooks:poison_cloud",
        "irons_spellbooks:sunbeam",
        "irons_spellbooks:dragon_breath_pool",
        "irons_spellbooks:poison_breath",
        "irons_spellbooks:poison_splash",
        "irons_spellbooks:acid_orb",
        "irons_spellbooks:black_hole",
        "irons_spellbooks:blood_needle",
        "irons_spellbooks:fire_field",
        "irons_spellbooks:magma_ball",
        "irons_spellbooks:comet",
        "irons_spellbooks:healing_aoe",
        "irons_spellbooks:earthquake_aoe",
        "irons_spellbooks:blizzard_aoe",
        "irons_spellbooks:guiding_bolt",
        "irons_spellbooks:gust",
        "irons_spellbooks:chain_lightning",
        "irons_spellbooks:devour_jaw",
        "irons_spellbooks:arrow_volley",
        "irons_spellbooks:stomp_aoe",
        "irons_spellbooks:lightning_strike",
        "irons_spellbooks:echoing_strike",
        "irons_spellbooks:ball_lightning",
        "irons_spellbooks:ice_spike",
        "irons_spellbooks:fire_eruption",
        "irons_spellbooks:fiery_dagger",
        "irons_spellbooks:thunderstep_orb",
        "irons_spellbooks:frost_field",
        "irons_spellbooks:fang_swirl",
        "irons_spellbooks:arcane_shackle",
        "irons_spellbooks:echoing_sword",
        // L_Ender's Cataclysm
        "cataclysm:lava_bomb",
        "cataclysm:flare_bomb",
        "cataclysm:ender_guardian_bullet",
        "cataclysm:void_shard",
        "cataclysm:blazing_bone",
        "cataclysm:spark",
        "cataclysm:ignis_fireball",
        "cataclysm:ignis_abyss_fireball",
        "cataclysm:laser_beam",
        "cataclysm:wither_missile",
        "cataclysm:wither_homing_missile",
        "cataclysm:wither_howitzer",
        "cataclysm:accretion",
        "cataclysm:abyss_orb",
        "cataclysm:void_howitzer",
        "cataclysm:earthquake",
        "cataclysm:amethyst_cluster_projectile",
        "cataclysm:sandstorm_projectile",
        "cataclysm:cursed_sandstorm",
        "cataclysm:ancient_desert_stele",
        "cataclysm:octo_ink",
        "cataclysm:water_spear",
        "cataclysm:lightning_spear"
    );
    private static final Set<String> LOOKUP = Set.copyOf(IDS);

    public static boolean contains(String id) { return LOOKUP.contains(id); }
    private BuiltinProjectileBlacklist() {}
}
