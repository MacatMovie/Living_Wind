# Living Wind — Multi-version source

**Status:** v1.3.0 development build for both Forge 1.20.1 and NeoForge 1.21.1. No temporary Sable or High Seas diagnostics or experimental sail-speed overrides are included. Final release verification is still recommended.

A server-authoritative Minecraft wind system for **NeoForge 1.21.1** and **Forge 1.20.1**. Wind changes direction and strength naturally over time, responds to weather and the environment, and drives visual, audio, and optional mod integrations.

## Features

- Natural wind variation, gust clusters, calmer/windier periods, weather and altitude influences, server synchronization, and configuration options.
- Windblown foliage and ambient particles, including wind-responsive leaves and grass blades with texture-derived colors.
- Configurable wind effects on existing vanilla and modded particles, with particle filters and per-particle response multipliers.
- Position-aware shelter: enclosed spaces and caves shelter particles, indoor sounds, and supported animations, using a bounded, worker-safe cache. Weather effects do not starve the nearby particle cache.
- Underwater protection: airborne foliage and particle wind avoid submerged areas.
- Wind audio, optional environmental accents, optional integrations, and the existing in-game wind Debug HUD.
- Wind vane variants, redstone direction indicators, windmills, and optional Create windmill integration.

## Optional integrations

Where the relevant mod is installed and supported, Living Wind includes integrations for **Swinging Lanterns 2.0.4**, **Particle Rain v4**, **Pretty Rain**, **Fancy World Animations**, **Particular / Particular Reforged**, **Windy**, **Ambient Sounds**, **Dynamic Surroundings**, **Supplementaries**, **Create**, **Create: Aeronautics/Sable**, **Create: High Seas**, **Interactive Foliage 2.2.0 / SWAY (optional)**, and related particle or shader effects present in this source. Individual features can be disabled or adjusted in the mod configuration. Compatibility is optional, not a hard dependency.

## v1.3.0 development — Conifer needles and configurable leaf types (both loaders)

- Four original grayscale 16×16 windblown needle sprites, recolored using the same source-leaf tint as broadleaf sprites. They have their own `livingwind:wind_needles` particle type, reuse the native tumbling/rendering, and have a deliberately restrained wind response (20% broadleaf horizontal drift, gust lift, and ground wind kick), quicker settling, and fewer ground hops than broadleaf particles. Native needles are excluded from the generic particle-wind hook to prevent an unintended second wind push. Broadleaf and grass motion remains unchanged.
- Eligible leaves resolve as **CONIFER** or **BROADLEAF**, in this priority: manually assigned block IDs > standard `minecraft:spawn_falling_needles` / `minecraft:spawn_falling_leaves` datapack tags (used by Vanilla Backport) > built-in conifer species-name defaults (spruce, pine, fir, redwood, cypress, cedar, hemlock, larch, etc.) > broadleaf fallback.
- New **Windblown Leaves → Leaf Particle Types (by Block)** browser. Search installed leaf IDs; click a row to cycle Auto → Conifer → Broadleaf → Auto. You can also type any installed custom block ID. User choices are stored in `livingwind-client.toml` as `leafTypeOverrides = ["modid:block=CONIFER", ...]`.
- **Conifer Needle Density** defaults to 0.65 (range 0–1), preserving the existing per-tick particle budget and natural broadleaf rates. Conifers release needles throughout the year without a seasons-mod dependency.
- Vanilla Backport is **not required** and its artwork/code is **not redistributed**. The optional vanilla-namespace block tags are recognized whenever present. If Vanilla Backport is also independently emitting its own particles, adjust that mod's settings separately to avoid duplicate visuals.
- **Still to test in-game:** textures at different angles, runtime config changes, modded tags, both build targets. This is a feature test, not a released v1.3.0 JAR.

## v1.2.0 Interactive Foliage integration (both maintained targets)

- Optional GPU wind bridge for Interactive Foliage 2.2.0: use Living Wind's synchronized strength and direction for GPU grass/foliage sway, while preserving Interactive Foliage's existing entity interaction and grass renderer.
- Client config `interactiveFoliage.enabled`, default on. Enable **Weather Wind** in Interactive Foliage itself for the wind uniform to exist.
- Forge 1.20.1 and NeoForge 1.21.1 support legacy terrain, shared foliage GLSL and Iris shader paths. NeoForge moving-contraption wind definitions are preserved.
- Smooth frame-based strength/direction easing and stationary calm behavior. Unknown future shader text is left untouched, with strength-only fallback. No shaders or code from Interactive Foliage or ARR Where Winds Blow are included.
- Shader transform adapted with attribution from MIT-licensed AeroWeather (see THIRD_PARTY_NOTICES.md). Interactive Foliage 2.2.0 integration has been confirmed in-game on both Forge 1.20.1 and NeoForge 1.21.1. Fresh release builds should still be verified before publishing.

## v1.2.0 development changes (NeoForge 1.21.1 ONLY)

- Optional server-side Create: Aeronautics / Sable wind forces. Recognizes Aeronautics balloon-envelope blocks (`#aeronautics:envelope`) and Create sails (`#create:windmill_sails`) on assembled Sable sublevels. Ordinary Sable objects without those blocks are unaffected.
- Uses Sable's pre-physics tick and registered force-group API. Force is applied at the cached center of the wind-catching blocks in Sable-local coordinates; Living Wind's existing server wind direction and strength power the calculation.
- New **Create: Aeronautics Physics** section in the NeoForge common config: **Wind Push on Contraptions** (default on) and **Contraption Wind Force Multiplier** (0–5x, default 1x, presented as a slider).
- One bounded scan per assembled sublevel; cached eligible-block ratio, dimensions, and force center; no world scans every physics substep. Profiles reset when the sublevel is removed. Structural edits after assembly may require reassembly to refresh the profile.
- Automatic double-wind prevention when AeroWeather is installed. If known Deep Seas/High Seas variants are installed, sail-only bodies are skipped so the ocean mod's separate sail propulsion isn't doubled; balloon-envelope craft can still respond.
- Uses a playtested force baseline: prior calibrated build 0.30x equals this build 1.00x (only the integration pressure coefficient changed). Sable resolves mass, and Living Wind estimates the projected wind-catching area. Force varies with wind-catching area and Sable mass handling.
- Optional dependencies are **compileOnly** and are never packed in Living Wind's JAR. Existing Forge particle, sound, shader, and wind behavior are preserved, apart from the optional Interactive Foliage integration.

**Configuration note:** If you previously saved the calibration value 0.30x, set the slider to 1.00x to restore the playtested force. Normal regression testing is still recommended for later v1.2.0 changes.

**Build dependencies:** On the first NeoForge build Gradle may need to download Sable API from Modrinth's Maven and Sable Companion API from the Sable Companion developer Maven. Your normal global Gradle setup is retained.

## v1.1.1 changes

- Fixed wind ambience sometimes going silent for an extended period after changing/reloading shaders, reloading resource packs, or other sound-engine restarts.
- Added a very lightweight client-side playback-state check every 40 active game ticks (~2 seconds). Only missing *continuous wind beds* are restarted, with a grace period for newly queued sounds.
- Interrupted gust and foliage accent sounds are simply cleaned up; their existing natural/random playback scheduling, volume, pitch, shelter attenuation, and crossfades are preserved.
- Works identically for Forge 1.20.1 and NeoForge 1.21.1 through shared audio code, with no changes to wind simulation, particles, shelter caches, or compatibility integrations.

## v1.1.0 changes

- Added **Swinging Lanterns** wind-driven, directional leaning using its native physics and constraints.
- Added support for the **official Particle Rain v4** wind system, while retaining Pretty Rain compatibility.
- Improved indoor/cave shelter for smoke and other affected particles with prioritization for nearby positions, short-lived continuity after confirmed shelter readings, and isolation from heavy weather-particle requests.
- Resolved the brief shelter disruption after leaving the singleplayer pause menu.
- Added shelter and underwater protection for Living Wind's own leaves, grass fragments, and ambient particles; preserved outdoor movement across distance-based cache boundaries.
- Reduced underwater-cache maintenance overhead and polished windmill rendering while preserving the original texture mapping.
- Removed the temporary shelter-test logging, configuration toggle, and diagnostic cache inspection helpers after successful in-game testing.

The wind simulation, configuration controls, Debug HUD, optional compatibilities, and original gameplay behavior are preserved unless described above.

## Building the mod

On Windows, run **`Build-One.bat`** to choose Forge, NeoForge, or both, or **`Build-All.bat`** to build everything. This project uses Java 17 / global Gradle 8.8 for Forge 1.20.1, and Java 21 / global Gradle 8.14.4 for NeoForge 1.21.1 (see `targets.json`). Build scripts keep the window open and collect release JARs in `dist/`.

Expected release JAR filenames:

- `LivingWind-Forge-1.20.1-v1.3.0.jar`
- `LivingWind-NeoForge-1.21.1-v1.3.0.jar`

**Version metadata:** Forge and NeoForge require plain numeric versions in their mod metadata (`1.2.0` for both targets). The `v` prefix belongs in the release JAR filenames (as shown above), not in the loader-parsed version field. Consequently, Minecraft’s standard Mods screen may display the numeric version without the `v`. The build collector retains the preferred `v`-prefixed filenames in `dist/`.

Source is shared in `shared/common/`, while loader-specific Java, configuration and resources are under `targets/`. The release ZIP packages the project contents directly at its root.

## Startup metadata fix

Corrected a packaging issue in the first v1.2.0 prototype: NeoForge rejected internal `mod_version=v1.2.0` with `Illegal version number specified v1.2.0` before Living Wind could initialize. Loader metadata is now numeric, while JAR naming remains `-v1.2.0`. No physics, sound, shelter, or particle logic was changed. Rebuild the JAR; renaming an already compiled JAR cannot repair its embedded metadata.

## Verification and release notes

The earlier v1.1.1 release was a focused audio-recovery hotfix based on v1.1.0; those fixes remain part of v1.2.0. Build and test both targets locally. A good regression test is to force audible wind, toggle shaders or resource packs, confirm wind returns within a few seconds, then confirm ordinary wind transitions, environmental accents, and intentionally muted sound settings still behave normally.

The removed **Shelter Diagnostics (TEST LOG)** option is unrelated to the regular wind **Debug HUD**, which remains available.

### NeoForge v1.2.0 development: Sable instruments

With Sable installed, Living Wind wind vanes, bird vanes, redstone vanes and both windmills resolve moving plot coordinates to their visible world location and correct their local heading as the contraption rotates. Local redstone outputs remain relative to the blockstate's coordinate frame. Without Sable, behavior is unchanged.

## v1.2.0 — Create: High Seas wind compatibility

NeoForge only. `livingwind-common.toml` setting `[highSeas] enabled = true` (default) redirects High Seas central `WindManager.getWind` queries to Living Wind's synchronized vector, keeping High Seas' own hull/sail physics. Client follows the server's toggle using an unused bit of the existing Living Wind wind-sync packet. Disable to restore High Seas procedural wind behavior. No additional High Seas runtime dependency is required if High Seas is not installed.

High Seas retains its native sailing, buoyancy, steering, and sail power calculations; this integration supplies only Living Wind's wind vector. The High Seas vane uses a slower, smoother interpolation while compatibility is on. The Living Wind wind-vane compass UVs are corrected on both block and item models. Disabling compatibility returns High Seas to its own wind provider and original vane interpolation.

The native sailing behavior of High Seas 3.3.0 is not changed here. No diagnostic mixins, log heartbeats, or experimental sail-speed tuning are part of this baseline.
