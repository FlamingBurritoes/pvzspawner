# pvzpawners

## Plantz Spawner

`/give @s pvzpawners:plantz_spawner` gives the configurable **Plantz Spawner**, also in Creative's Functional Blocks tab. Right-click it for a small screen containing one seed-packet slot above your inventory. It starts empty and accepts only genuine packets whose associated plant reports `canWalk() == true`.

The packet stays in the slot and is not consumed. A valid packet waits **90 seconds** before initially spawning one matching plant; after death/taming, replacement takes the full **180 seconds** by default. Removing the packet immediately kills the linked plant and clears the selection. Replacing/reinserting the packet kills the previous selection and starts a fresh initial wait. Breaking the spawner kills its plant and drops the inserted packet separately from the block.

The existing smoke, 3-block spawning radius, 6-block roaming radius, dirt-like mining, top-down icon, anti-taming deaths and persistent UUID ownership are shared. Configure this source independently in `config/pvzpawners-plantz.json` (defaults: `180 / 3 / 6 / 1`, using the same field names as the other configs). Craft one Plantz Spawner with **either a Weed Spawner or a Dandelion Weed Spawner in the center, surrounded by eight Bottles of Sun**.

All spawners wait `floor(respawnDelaySeconds / 2)` whole seconds for their initial spawn: **Weed 30s, Dandelion Weed 10s, Plantz 90s** by default. Later deaths use the full delay. Breaking/replacing a source restarts the initial wait; saving/reopening preserves its recorded deadline. Inventory/Creative icons use front lighting to avoid darkened top-down previews.

Fabric addon for Minecraft **26.1.2** that adds custom blocks spawning entities from Plants & Zombies and Pea Extension. The first block is **Weed Spawner**.

Also includes **Dandelion Weed Spawner** (`pvzpawners:dandelion_weed_spawner`): the same features, but spawns up to **2 Dandelion Weeds** with a **20-second** replacement cooldown. It uses the supplied dandelion slab model/top texture and a top-down inventory icon. Its `WWW / WDW / DDD` recipe uses Dandelion Weed Seed Packets for W and Dirt for D. Both spawners are in Creative's Functional Blocks tab.

Dandelion settings are independent in `config/pvzpawners-dandelion.json` (defaults: `respawnDelaySeconds: 20`, `spawnRadius: 3`, `roamRadius: 6`, `maxWeeds: 2`). Existing `config/pvzpawners.json` settings for regular weeds are unchanged. Restart/reopen the world after editing either file.

## Weed Spawner quick start

- Find **Weed Spawner** in Creative's **Functional Blocks** tab, or use `/give @s pvzpawners:weed_spawner`.
- Place it beside open, solid ground. After an initial 30-second wait, it fills three owned slots within a horizontal three-block radius. Each dead weed is replaced after the full 60 seconds of game time, provided there is a safe spot and the source chunk is ticking.
- The nine Weed variants have equal chances. Movement is limited to six blocks from the source; this does not affect naturally spawned weeds.
- The block uses the supplied slab-like model and a half-height collision/selection shape. Craft one spawner using the pattern `WWW / WDW / DDD`, where W is a Weed Seed Packet and D is Dirt (five packets and four dirt).
- Mine and collect it by hand like dirt; shovels are the efficient tool. Breaking/removing it kills all linked weeds with their normal death animation. Unloaded linked weeds die when they next load.
- Spawner-owned weeds cannot be kept as pets: attempting to tame one kills it and starts that slot's normal replacement cooldown. Natural/untracked weeds remain tamable.
- Successful spawns produce a classic spawner-style smoke puff. Vanilla side/bottom textures are referenced directly; they do not need to be copied into the mod.
- Change server/world-wide defaults in `config/pvzpawners.json` (development: `run/config/pvzpawners.json`) and restart the server or reopen the single-player world.

See [MOD_FILES.md](MOD_FILES.md) for the full file inventory, exact behavior, configuration, texture instructions and testing checklist. [MINECRAFT_MODDING_LESSONS.md](MINECRAFT_MODDING_LESSONS.md) is a portable guide for future projects.

## Development setup

1. Install a **JDK 25 or newer supported by Gradle** and select it as both the project SDK and Gradle JVM in IntelliJ IDEA (Settings > Build, Execution, Deployment > Build Tools > Gradle). The existing local JDK 27 has been used successfully for builds.
2. Reload the Gradle project after changing dependencies. Gradle downloads the following pinned dependencies automatically; no manual JAR copying is needed for development.

| Dependency | Development version | Fabric mod ID |
| --- | --- | --- |
| [Plants & Zombies](https://www.curseforge.com/minecraft/mc-mods/plants-and-zombies/files/9347311) | 1.5.1 | `plantz` |
| [Plants and Zombies Extension / Pea Extension](https://www.curseforge.com/minecraft/mc-mods/plants-and-zombies-extension/files/9379288) | 1.2.8 | `pea_extension` |
| Fabric Language Kotlin | 1.14.2+kotlin.2.4.21 | `fabric-language-kotlin` |
| Fabric API | 0.155.3+26.1.2 | `fabric-api` |

3. Run `./gradlew.bat build` to build, or `./gradlew.bat runClient` to launch the development game. For a dedicated-server test, use `./gradlew.bat runServer` and accept Minecraft's EULA only if you agree to it.

The CurseForge project/file IDs are configured in `build.gradle` and `gradle.properties`. CurseMaven has no transitive mod dependency metadata, so Fabric Language Kotlin is declared explicitly. This project can remain Java-only; no Kotlin compiler plugin is needed.

For a normal player installation, install Fabric Loader 0.20.1 or newer for Minecraft 26.1.2, this mod, and all four dependencies above on both client and server. Dependencies are **not bundled** into this mod's JAR. Pea Extension 1.2.8 requires Plants & Zombies `>=1.5.1 <1.6.0`; do not upgrade the base mod to 1.6 without checking addon compatibility.

## Plan for additional spawners

1. **Choose the next spawner:** provide its name and the exact target entity registry ID (`plantz:...` or `pea_extension:...`, not just its display name). We can identify valid IDs from the installed mods or in-game `/summon` autocomplete.
2. **Define its behavior:** one block per entity or a configurable spawner; spawn interval/count/radius; player proximity; nearby-entity cap; redstone activation; allowed ground/light; and whether spawned plants should be wild or owned/tamed. Plant placement and taming may need the original mods' APIs rather than a generic entity spawn.
3. **Extend the Weed Spawner pattern:** register the next block and inventory item and adapt server-side spawning and persistence to the target entity's APIs. Use placeholders first so art does not block testing.
4. **Add resources:** blockstates, block/item models, display names, loot tables, mining tags and recipes. Most JSON resources can be generated; you only need to supply art and design choices.
5. **Test and expand:** verify spawn placement, cooldowns, entity limits, chunk reloads, redstone and multiplayer/dedicated-server behavior before adding more spawners.

### What you need to provide

- **Block art:** PNG textures, preferably **16x16** pixels per face for the vanilla style (32x32 is also fine). A simple cube needs one shared texture, or separate side/top/bottom textures. Use lowercase names such as `peashooter_spawner_side.png`.
- **Optional custom shape:** a Blockbench Java block/item model and its PNG textures if you want more than a cube. No new model or textures are needed for the spawned mobs: those stay in the dependency mods.
- **Inventory appearance:** the block's 3D model can be reused automatically; a separate item icon is optional.
- **Design choices:** block names, target entities, crafting ingredients, survival availability and spawning rules. Start with one or two targets.
- **Optional polish:** sound files, particles, a custom GUI design and a replacement mod icon. These are not necessary for the first prototype. Use original or appropriately licensed assets.

Future block textures belong in `src/main/resources/assets/pvzpawners/textures/block/`. Optional item icons belong in `assets/pvzpawners/textures/item/` under the same resources folder. The existing mod ID is **`pvzpawners`** (without the `s` in `spawners`); resource namespaces must match it unless we deliberately rename the mod later.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
