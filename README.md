# Living Wind

Living Wind is a synchronized wind system for Minecraft, featuring changing wind direction, strength, and gusts. Other mods can read its wind values to drive their own movement, physics, animations, and particle effects.

**Supported versions:** Forge 1.20.1 and NeoForge 1.21.1. The examples below describe the public API in Living Wind **v1.4.0**. Use the JAR for your Minecraft version and mod loader.

## For mod developers

You do **not** need to reproduce Living Wind's simulation. Read the wind values through its public API and apply them to your own mod's behavior. Server and client APIs serve different purposes.

### 1. Server-side wind API (gameplay and physics)

Use `WindApi` when the result needs to be authoritative, such as a boat's movement, a windmill's output, or another game mechanic. Call it with a `ServerLevel`, **not** a client `Level`.

```java
import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.wind.WindSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

// Inside server-side code, where serverLevel and pos are already available:
WindSnapshot wind = WindApi.getAt(serverLevel, pos);

float strength = wind.strength();
float direction = wind.directionDegrees();
float windX = wind.windX();
float windZ = wind.windZ();
```

**Which server method should you use?**

| Method | Returns | Good for |
| --- | --- | --- |
| `WindApi.get(serverLevel)` | The current atmospheric wind for that dimension, without altitude scaling. | A general wind reading for the dimension. |
| `WindApi.getAt(serverLevel, pos)` | Wind at `pos`, including the configured **Overworld altitude scaling** and **biome-specific strength modifiers**. | A windmill, balloon, or other object at a specific height. |

`getAt` checks the biome at the requested position, while `get` deliberately stays a dimension-wide reading. Both methods retain their original names, parameters and return type. Read `getAt` on the server thread at a loaded position; do not query world biomes from async particle or physics workers.

`getAt` does **not** calculate whether `pos` is sheltered inside a building. Living Wind's local shelter/exposure detection is client-side. If your server-side physics need shelter, implement and cache a suitable enclosure check in your mod.

`WindSnapshot` exposes these values:

| Value | Meaning |
| --- | --- |
| `strength()` | Current wind intensity. It is **not** limited to 0–1; stronger weather or gusts can exceed 1.0. |
| `directionDegrees()` | Direction the wind is blowing **toward**, in degrees: `0` = south (+Z), `90` = west (−X), `180` = north (−Z), `270` = east (+X). |
| `windX()` / `windZ()` | Horizontal directional vector already scaled by wind strength. Useful for applying a force or offset. |
| `turbulence()` | Turbulence information from the simulation. |
| `gustStrength()` | Current gust contribution. |
| `directionInstability()` | How variable the direction currently is. |
| `windyPeriod()` / `stormFront()` | Flags for wind events. |
| `gameTime()` | Game tick associated with the snapshot. |

The values are *wind data*, not Minecraft velocity units. Choose your own multiplier for movement and physics. Avoid also multiplying `windX()` and `windZ()` by `strength()` unless you specifically want a squared strength response.

**Dimension settings matter:** If Living Wind is disabled in a dimension, `WindApi.get(...)` and `getAt(...)` return calm wind (zero strength and vector). Otherwise, the selected dimension mode is handled by the simulation; integrations generally do not need to special-case the Nether or End. A dimension explicitly set to **Disabled** stays windless even in a modified biome.

### 2. Client-side wind API (visuals and animations)

Use `ClientWindApi` on the **physical client only**, for things like hanging decorations, particle motion, or visual sway. These values are synchronized from the server and include smoothed client readings.

```java
import com.macatmovie.livingwind.api.ClientWindApi;

// Inside client-side code, after checking that Living Wind is installed:
if (ClientWindApi.hasWindState()) {
    float atmosphericWind = ClientWindApi.globalStrength();
    float playerWind = ClientWindApi.localStrength(); // includes the player biome
    float direction = ClientWindApi.directionDegrees();
    float x = ClientWindApi.localWindX();
    float z = ClientWindApi.localWindZ();

    // Assume xPos, yPos and zPos are the world coordinates of your effect.
    // Outdoor visual/particle effect at that position (no shelter check):
    float strengthHere = ClientWindApi.strengthAt(xPos, yPos, zPos);
    float windXHere = ClientWindApi.windXAt(xPos, yPos, zPos);
    float windZHere = ClientWindApi.windZAt(xPos, yPos, zPos);
}
```

**Choose the appropriate client reading:**

| Method | Meaning |
| --- | --- |
| `hasWindState()` | Whether synchronized wind data has arrived from a server/world. Check this before relying on client readings. |
| `globalStrength()` | Dimension-wide atmospheric strength before altitude, biome and shelter modifiers. Unchanged for existing integrations. |
| `altitudeStrength()` | Altitude-scaled strength including the **local player's smoothed biome modifier**, before shelter reduction. |
| `localStrength()` | Strength at the **local player**, after altitude, biome and shelter/exposure reduction. |
| `directionDegrees()` | Current smoothed direction, following the same degrees convention as the server API. |
| `globalWindX()` / `globalWindZ()` | Dimension-wide vector before altitude, biome or shelter modifiers. |
| `localWindX()` / `localWindZ()` | Wind vector after the **local player's** altitude, biome and shelter modifiers. |
| `strengthAt(x, y, z)` | Position-aware, altitude- and biome-adjusted strength, **without shelter**. Reads a thread-safe sampled biome cache. |
| `windXAt(x, y, z)` / `windZAt(x, y, z)` | The position-aware wind vector, already multiplied by strength; **without shelter**. |
| `turbulence()`, `windyPeriod()`, `stormFront()` | Additional synchronized conditions for visual effects. |

**Important:** `localStrength()` describes the **player's exposure**, not the exposure at any arbitrary rendered block. For example, if the player stands inside a house, outdoor leaves should not necessarily stop moving. For distant/world-space animations, prefer `strengthAt(x, y, z)` or `windXAt` / `windZAt` and any position-specific exposure logic your own mod needs. These methods never look up chunks on particle threads. Their sampled biome cache follows the local player, covers nearby positions, and may temporarily return unmodified atmospheric wind outside its range or before the next refresh. It is intended for visuals, **not authoritative gameplay**.

For animated objects, smooth your animation in your own renderer if needed. For gameplay-affecting movement, use the **server** API instead of trusting a client reading.

### 3. Adding Living Wind as an optional integration

Living Wind's **mod ID is `livingwind`**. You can make compatibility optional, so your mod still works when Living Wind isn't installed:

1. Obtain the Living Wind JAR matching your development target (Forge 1.20.1 or NeoForge 1.21.1).
2. Add it as a **compile-only** dependency in your development project; do not bundle Living Wind's classes inside your JAR.
3. Check whether `livingwind` is loaded **before using its API**, and isolate code referring to Living Wind classes in a compatibility class that only loads when the mod is present.
4. Test with Living Wind installed **and** absent, on both a dedicated server and client as applicable.

For a simple local-JAR setup, place the appropriate JAR in your project's `libs/` folder. A Gradle starting point is:

```gradle
// Example for a NeoForge 1.21.1 project. Use the Forge 1.20.1 JAR for Forge.
dependencies {
    compileOnly files('libs/LivingWind-NeoForge-1.21.1-v1.4.0.jar')
}
```

This is a **generic Gradle file-dependency example**, not a universal loader-specific dependency recipe. Some Forge/NeoForge toolchains need an additional development remapping or mod-dependency configuration. Use the appropriate setup for your build system. **No public Maven artifact coordinates are provided here.**

For a conditional loading check, the class is `net.minecraftforge.fml.ModList` on Forge 1.20.1 or `net.neoforged.fml.ModList` on NeoForge 1.21.1:

```java
if (ModList.get().isLoaded("livingwind")) {
    // Call your own separate LivingWindCompat class here.
    // Do not load that class when Living Wind is absent.
}
```

For optional dependency metadata, declare `livingwind` as **optional** rather than required (`mandatory=false` in Forge `mods.toml`, or `type="optional"` in NeoForge `neoforge.mods.toml`). The JAR's filename includes `v`, but the mod metadata version is numeric, such as `1.4.0`.

**Do not run `ClientWindApi` or `ParticleWindApi` from common initialization on a dedicated server.** Keep calls to client-only APIs behind your loader's client setup/dist separation.

### 4. Particle compatibility: prevent wind forces

By default, Living Wind can push modded particles. If your particle already responds to wind, or should never be moved, you can make it immune. Three approaches are available:

**Option A: Data tag (no Java dependency required)**

Create `src/main/resources/data/livingwind/tags/particle_type/wind_immune.json` in **your mod**:

```json
{
  "replace": false,
  "values": [
    "yourmod:your_particle"
  ]
}
```

Use the registered **particle type ID**, not the Java class name. This is the easiest option for normally registered particle types.

**Option B: Register a particle ID at client setup**

```java
import com.macatmovie.livingwind.api.ParticleWindApi;

ParticleWindApi.registerImmune("yourmod:your_particle");
```

**Option C: Register a custom particle implementation class at client setup**

```java
import com.macatmovie.livingwind.api.ParticleWindApi;

ParticleWindApi.registerImmuneParticleClass(YourParticleClass.class);
```

Option C is useful for directly constructed particles that don't retain a registry ID. It also covers subclasses of the registered class. Replace `YourParticleClass` with your own class extending Minecraft's client `Particle`.

Use runtime registration **only when Living Wind is loaded**, and only on the client. Registration is intended to happen once during client initialization, not once per particle or frame.

### Reading biome-adjusted wind

Biome modifiers are included automatically when you use the **position-aware** API. Choose `WindApi.getAt(serverLevel, pos)` for server-side physics or `ClientWindApi.strengthAt(x, y, z)` / `windXAt` / `windZAt` for client visuals. You do not need to detect individual biomes or duplicate Living Wind's modifier logic. Global readings remain dimension-wide, and dimension-disabled wind remains disabled.

### API source references

The corresponding implementations in this repository are:

- `shared/common/java/com/macatmovie/livingwind/api/WindApi.java`
- `shared/common/java/com/macatmovie/livingwind/wind/WindSnapshot.java`
- `targets/forge-1.20.1/src/main/java/com/macatmovie/livingwind/api/ClientWindApi.java`
- `targets/neoforge-1.21.1/src/main/java/com/macatmovie/livingwind/api/ClientWindApi.java`
- `shared/common/java/com/macatmovie/livingwind/api/ParticleWindApi.java`

These are the current entry points for integrations; you do not need to access Living Wind's internal wind simulation or config classes to read wind values.
