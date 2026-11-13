# Mod file guide

This is the project-specific inventory for `pvzpawners`. The Java package is `net.amemah.pvzspawners`, but the existing resource/mod namespace is `pvzpawners`; keep those spellings consistent with their respective uses.

## Existing project files

| File | Purpose |
| --- | --- |
| `build.gradle` | Configures Fabric Loom, Java compilation, CurseMaven, and the required mod dependencies. |
| `gradle.properties` | Pins Minecraft, Fabric, Kotlin runtime and CurseForge file versions. CurseForge file IDs are not mod version numbers. |
| `settings.gradle` | Configures plugin repositories and the Gradle project name. |
| `src/main/resources/fabric.mod.json` | Declares the mod ID, entrypoints and mandatory runtime dependencies. |
| `src/main/java/net/amemah/pvzspawners/Pvzpawners.java` | Main initialization and the shared identifier/logger helpers. |
| `src/main/java/net/amemah/pvzspawners/PvzpawnersDataGenerator.java` | Existing, currently empty resource-generation entrypoint. |
| `src/main/resources/pvzpawners.mixins.json` | Lists bytecode hooks used by the mod. |
| `README.md` | Dependency installation and project overview. |

## Weed Spawner

### Java files created

Paths below are relative to `src/main/java/net/amemah/pvzspawners/`.

| File | What it does |
| --- | --- |
| `block/ModBlocks.java` | Registers the Weed Spawner block and its inventory item, copies dirt's mining properties, and adds it to the Functional Blocks creative tab. |
| `block/WeedSpawnerBlock.java` | Provides the half-slab collision/selection box and registers/removes the source when placed/destroyed. Removal kills its linked weeds. The visual model is configured independently. |
| `config/WeedSpawnerConfig.java` | Creates/reads the server-authoritative JSON configuration, validates limits, and falls back to defaults without overwriting invalid files. |
| `spawner/SpawnSlot.java` | Tracks one owned entity UUID or a respawn deadline; a living/unloaded entity cannot be replaced. |
| `spawner/SpawnerRange.java` | Implements circular horizontal distance checks rather than square bounding-box checks. |
| `spawner/WeedSpawnerData.java` | Stores source positions, owned UUIDs, cooldown deadlines, pending deaths and silent cap-reduction cleanup in dimension-local SavedData. Kills linked weeds on source destruction, including unloaded ones when they next load. |
| `spawner/WeedSpawnerManager.java` | Handles spawning, equal variant selection, death/removal events, per-tick range enforcement, safe positions and configuration changes. |
| `mixin/WeedVariantAccessor.java` | Narrow Pea Extension 1.2.8 hook for its private variant/shield data. Sets both fields so armored variants are initialized correctly. |
| `mixin/ManagedWeedMixin.java` | Restricts movement and dimensional escapes of tagged spawner weeds only. Ordinary mobs and natural weeds are unchanged. |
| `mixin/ManagedWeedTamingMixin.java` | Intercepts taming of UUID-linked spawner weeds before ownership is granted, kills them, and starts the normal replacement cooldown. Natural/untracked weeds are unaffected. |

`Pvzpawners.java` and `pvzpawners.mixins.json` were updated to enable these systems. No custom block entity or renderer is necessary: ownership/timers live in dimension SavedData, and the block uses a normal JSON model.

### Resource files created

Paths below are relative to `src/main/resources/`.

| File | What it does |
| --- | --- |
| `assets/pvzpawners/blockstates/weed_spawner.json` | Connects the placed block to its model. |
| `assets/pvzpawners/models/block/weed_spawner.json` | User-supplied slab-like Blockbench model, preserved during gameplay changes. |
| `assets/pvzpawners/textures/block/weed_spawner_top.png` | User-supplied top texture, preserved during gameplay changes. |
| `assets/pvzpawners/models/item/weed_spawner.json` | Reuses the block model, with a GUI-only 90-degree rotation to show the top face in inventory/Creative slots. Held and placed appearances are unchanged. |
| `assets/pvzpawners/items/weed_spawner.json` | Modern inventory-item model definition. |
| `assets/pvzpawners/lang/en_us.json` | Gives the block its English display name, Weed Spawner. |
| `data/pvzpawners/loot_table/blocks/weed_spawner.json` | Makes the block drop itself when mined, with vanilla explosion-survival rules. |
| `data/pvzpawners/recipe/weed_spawner.json` | Crafts one Weed Spawner from five Weed Seed Packets and four Dirt in the requested pattern. |
| `data/minecraft/tags/block/mineable/shovel.json` | Marks shovels as the efficient mining tool, matching dirt. Replaces the previous pickaxe tag. |

### Tests and documentation

| File | What it does |
| --- | --- |
| `src/test/java/net/amemah/pvzspawners/spawner/SpawnSlotTest.java` | Tests cooldown boundaries, duplicate death events, unloaded ownership and independent slots. |
| `src/test/java/net/amemah/pvzspawners/spawner/SpawnerRangeTest.java` | Tests circular boundaries and source-relative distances. |
| `src/test/java/net/amemah/pvzspawners/config/WeedSpawnerConfigTest.java` | Tests defaults and invalid config values, including JSON deserialization. |
| `src/gametest/java/net/amemah/pvzspawners/spawner/WeedSpawnerGameTests.java` | Tests real crafting, entity spawning, cooldowns, variant accessors and confinement in a headless Minecraft test server. |
| `src/gametest/resources/fabric.mod.json` | Registers the development-only test mod; not shipped in the release JAR. |
| `MOD_FILES.md` | This project inventory, usage and customization guide. |
| `MINECRAFT_MODDING_LESSONS.md` | Reusable lessons for other Minecraft projects. |

`build.gradle` also configures JUnit and Fabric server GameTests. `fabric.mod.json` describes the implemented addon and bounds the extension dependency to the checked 1.2.x API family.

### Behavior and temporary decisions

- Registry ID: `pvzpawners:weed_spawner`. Creative tab: **Functional Blocks**. Command: `/give @s pvzpawners:weed_spawner`.
- Target: `pea_extension:weed`, the same entity type used by the extension's Weed seed packet. The separate Dandelion Weed is not part of this pool.
- Variant pool: **PLAIN, POTTED, FLAG, FLAG_POTTED, VASE, PORCELAIN_VASE, PUMPKIN, LEAF_SHIELD, WOOD_SHIELD**. Each new spawn has a 1/9 chance; repeats are allowed. This deliberately differs from the extension's normal weighted distribution.
- Three slots per spawner by default. Initial slots fill immediately when the source is ticking and safe ground is available (checked once per second), rather than waiting a minute after placing the block.
- Each confirmed death starts that slot's own 1,200-tick cooldown. This is 60 seconds at 20 TPS, not wall-clock time: paused games, server shutdowns and lag do not count as full-speed play. Replacement can be up to one additional second late because spawn attempts run once per second.
- Source positions, owned UUIDs and deadlines are saved per dimension. Chunk unloading is not death and never causes an automatic refill. Deaths of loaded weeds are recorded even if their source chunk is unloaded. Sources do not force-load chunks.
- The spawn radius is a horizontal circle measured from the source block's center, with safe ground searched up to three blocks above/below it. Roam containment uses the horizontal circle plus a vertical limit of +/- the configured roam radius. These are temporary explicit interpretations of “radius,” not a spherical distance rule.
- Weeds spawn wild/unowned in wander mode, not automatically tamed to the placer. They retain their dependency mod's combat and interaction behavior.
- A soft AI home is combined with a hard movement boundary. If something teleports a weed outside the range, it is recalled to a collision-free, dry, supported return position when one is available. Portals/dimensional teleports are blocked while managed. If every possible return location becomes obstructed, the mod does not teleport into solid blocks or load missing chunks; unblock the ground to allow recall/spawning.
- Permanently discarded weeds (for example, converted back into seed packets) also free their slot and start the cooldown. This differs from merely unloading a chunk.
- Breaking/removing the source stops spawning and **kills all weeds still linked to that source, including tamed weeds**, using normal lethal damage and the dependency's death animation/sound. Normal death loot rules apply. Ownership is cleared before death callbacks, so no replacement timers are started. Unloaded weeds are saved in a pending-death list and killed when they next load, without force-loading chunks. Other spawners' weeds and unrelated/natural weeds are not targeted.
- Attempting to tame a managed weed **immediately kills it** instead of granting ownership; it stays briefly as a dying entity for the normal animation rather than vanishing instantly. That slot starts the configurable replacement cooldown (60 seconds by default); the other weeds and source are unchanged. Natural/untracked weeds remain tamable. A tick/load fallback also kills already-tamed managed weeds from older saves or direct tame-flag changes. The addon does not refund resources consumed by the dependency's surrounding taming interaction.
- Each successfully added weed emits a server-synchronized puff of 20 smoke particles and the mob's standard spawn effect. Failed spawn attempts emit nothing. These are vanilla particles; no new texture or client renderer is needed.
- Mining properties are copied from dirt: hardness 0.5, collection by hand, dirt-like shovel efficiency, and no minimum tool tier. The loot table drops the spawner itself, not dirt.

### Configuration

The first server/world start creates `config/pvzpawners.json` in the game directory. Development uses `run/config/pvzpawners.json`; headless tests have their own configuration under `build/run/gameTest/config/`.

```json
{
  "respawnDelaySeconds": 60,
  "spawnRadius": 3,
  "roamRadius": 6,
  "maxWeeds": 3
}
```

Edit while the server is stopped, then restart. For single player, leave the world, edit, and reopen it. Settings apply to all Weed Spawners on that server/world session; there is no per-block GUI yet. Multiplayer clients cannot override the server's settings. Existing death deadlines remain as recorded; the new delay applies to future deaths.

Valid limits: delay 1..86400 seconds, spawn radius 1..32, roam radius from spawn radius to 64, count 1..64. Invalid/malformed files are kept intact, an error is logged, and defaults are used. Do not remove required keys. Increasing the count adds slots; lowering it retires/discards excess managed weeds (including when unloaded weeds next load), without death drops.

### Editing the supplied model/textures

1. Keep the editable Blockbench project and export the Java block model JSON to `src/main/resources/assets/pvzpawners/models/block/weed_spawner.json`. The supplied model is already at that path; changing it does not change the Java collision box.
2. Custom PNGs belong in `src/main/resources/assets/pvzpawners/textures/block/`. The current custom top texture is `weed_spawner_top.png`.
3. Use resource identifiers in the model's texture entries: `pvzpawners:block/weed_spawner_top` for the custom top, `minecraft:block/dirt` for vanilla dirt, and `minecraft:block/grass_block_side` for vanilla grass sides. Leave off `.png`; a bare `dirt` reference is not the same as `minecraft:block/dirt`. Existing art files were not changed as part of the gameplay update.
4. Restart the development client, or rebuild/copy the new mod JAR and restart a normal client. F3+T reloads textures only if the updated file is already present on the running game's resource classpath.
5. The item shares the block's textures/model, but its GUI-only display transform looks straight down at the top for inventory/Creative slots. Editing the top texture updates that preview too. Held and placed appearances are not rotated by this GUI transform. The namespace is **pvzpawners**, not pvzspawners.

### Testing

Run `./gradlew.bat test` for fast unit tests and `./gradlew.bat runGameTest` for the headless integration tests. `./gradlew.bat build` runs both through `check`. The integration cooldown test intentionally runs for just over one minute of game time.

Manual follow-up checklist: inspect the Creative item and model in a client; replace the texture; save/quit/reopen with both living weeds and pending cooldowns; unload the source chunk while a weed remains loaded; test obstructed terrain, taming/seed-packet conversion, changed config, and block destruction/explosions.

### Verified this session

- Gradle `build`: **passed**, including the requested crafting recipe.
- **11 JUnit tests passed** (timer/ownership state, circular range checks and config validation).
- **10 addon GameTests passed** in a real headless Minecraft server: the exact crafting pattern/output, variant field access, three initial weeds plus death cooldown/respawn, independent cleanup of overlapping sources, confinement/source-destruction death with a retained dying entity, ownership/deadline NBT round-trip, persisted pending death on entity load, dirt-like mining/drops, immediate death/cooldown on taming while natural weeds remain tamable, and the direct tame-flag fallback. The runner reported **11 passed** including Fabric's own test.
- Checked `build/libs/pvzpawners-1.0.0.jar`: block assets and classes are included; test classes are not.
- The release JAR contains the new shovel tag and no old pickaxe tag. Custom model geometry, UVs and PNG art were preserved; only the invalid vanilla texture identifiers were repaired. Both referenced vanilla PNG paths were confirmed present in Minecraft's JAR.
- Client rendering (including smoke/death appearance), a complete world restart/chunk-unload cycle, and actual seed-packet taming interactions remain manual checks. The cleanup test uses a real tamed-state weed and the actual entity-load callback; it does not simulate a complete chunk save/reload. Upstream mods emitted their own existing mixin/loot/resource warnings during the server tests; those were not changed by this addon.
- If IntelliJ shows unresolved Minecraft symbols despite the successful builds, reload/synchronize the Gradle project and allow indexing to finish. Do not “fix” valid imports by guessing an older version's mappings.

### IntelliJ unresolved Minecraft classes

The local imported Gradle model includes the Minecraft library and its on-disk JAR contains both `net.minecraft.world.level.block.Block` and `net.minecraft.core.Registry`. These are valid imports, not classes that need to be added to the mod itself. An IDE import/index problem remains possible even when the command-line build passes.

1. In IntelliJ's Gradle tool window, choose **Reload/Sync All Gradle Projects**, then wait for import and indexing to finish.
2. Check **File > Project Structure > Project SDK** and **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM**. Use the installed JDK 27, or a supported JDK 25+; prefer the project SDK for the Gradle JVM.
3. If Minecraft symbols remain red after a successful sync, use **File > Invalidate Caches > Invalidate and Restart**, then reload Gradle if necessary. Do not choose options deleting Local History or manually delete `.idea`/global Gradle caches.
4. If syncing itself fails, inspect the actual Gradle sync error instead of changing Java imports or adding manually copied Minecraft JARs. IDE cache repair must be completed in IntelliJ; a shell build cannot confirm whether the editor highlighting has recovered.

Follow-up investigation: normal cache invalidation/restart did not resolve the user's missing Minecraft search results. The Minecraft JAR also passed both 7-Zip's full archive test and the JDK JAR validator. With explicit permission and IntelliJ confirmed closed, only the IDE's `caches` directory was renamed to the reversible backup `C:\Users\jerem\AppData\Local\JetBrains\IntelliJIdea2026.3\caches.backup-20261114-015232`. Project files, IDE settings, Local History and Gradle caches were not changed by this repair. **After reopening and rebuilding the indexes, the user confirmed that the red references were fixed.** Keep the backup until no longer needed; no deletion was performed. This editor result was verified separately from the successful builds/game tests.

### Crafting recipe

The crafting-table pattern is:

```text
WWW
WDW
DDD
```

W = `pea_extension:weed_packet` (Weed Seed Packet); D = `minecraft:dirt`. Each craft consumes five packets and four dirt and produces one `pvzpawners:weed_spawner`.

Edit `src/main/resources/data/pvzpawners/recipe/weed_spawner.json` to change the pattern, ingredient IDs or output count. Minecraft loads recipe JSON automatically; no Java registration is needed. Rebuild/relaunch after editing project resources.

### Texture references in exported models

Model JSON stores texture resource paths, not image bytes. For example, `pvzpawners:block/weed_spawner` resolves to `assets/pvzpawners/textures/block/weed_spawner.png`. If you rename or move a custom PNG after exporting, update the JSON reference too; Blockbench's internal texture label alone is not the runtime path.

For an unchanged vanilla texture, reference its existing identifier (for example, `minecraft:block/dirt`) instead of copying the PNG into this mod. Export edited/original PNGs into this mod's own texture namespace. Texture keys such as `0`, `all` or `side` must continue to match the face references such as `#0` in the model.
