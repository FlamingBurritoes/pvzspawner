# Mod file guide

## Initial timers, upgrade recipe and inventory lighting

- All source kinds now create empty slots with `readyAt = currentWorldGameTime + floor(respawnDelaySeconds / 2) * 20`. Defaults: Weed 30 seconds, Dandelion Weed 10 seconds, Plantz 90 seconds. Rounding is in whole seconds (61 seconds becomes 30, not 30.5); a custom 1-second regular cooldown gives a 0-second initial delay as specified by the formula.
- `WeedSpawnerConfig.Settings.initialDelayTicks()` centralizes this calculation. `WeedSpawnerBlock.onPlace` passes the current world time into SavedData registration. Plantz packet changes also pass the current world time, so inserting/replacing/reinserting a packet cannot skip the initial wait. Added capacity after a config increase gets the same initial wait.
- Normal deaths/taming retain the full original cooldown. Initial and replacement deadlines share the existing saved `ready_at` field, so saving/reopening does not restart them. Already-recorded deadlines in older saves are preserved. Breaking/replacing a source starts a new wait, rather than spawning instantly. Spawn attempts still run once per second and require safe ground/a ticking source.
- Added `src/main/resources/data/pvzpawners/recipe/plantz_spawner.json`: `SSS / SBS / SSS`, S = `plantz:sun_bottle`; B = either `pvzpawners:weed_spawner` or `pvzpawners:dandelion_weed_spawner`. Output: one Plantz Spawner.
- All three `assets/pvzpawners/models/item/*_spawner.json` files now set `gui_light` to `front`. Their top-down GUI transforms, supplied textures and placed-block models are preserved. Actual brightness remains a client visual check.
- Tests now wait through initial cooldowns before checking spawning and full death cooldowns. Added tests cover both recipe alternatives, requiring eight full Sun Bottles, floored odd-second settings, persisted initial deadlines, and breaking/replacing without bypassing the wait.
- Verification: full build passed with **20 server GameTests** (19 addon tests and Fabric's test) and **12 JUnit tests**. Both upgrade ingredients matched the actual recipe; an empty bottle in place of Sun was rejected. The release JAR was checked for the upgrade recipe and `gui_light: front` on all three inventory models. Client brightness remains a visual follow-up.

## Plantz Spawner addition

### Behavior and validation

- Registry ID: `pvzpawners:plantz_spawner`. No initial spawn type. The supplied model/top PNG are reused with repaired texture identifiers, plus a GUI-only top-down item transform.
- Right-click opens a small menu with exactly one input slot above the 36 player inventory/hotbar slots; no output, progress arrow, fuel slot, buttons or extra fields.
- Packet stays in the slot (maximum one); closing the screen does not remove it. Removing it kills its linked entity and clears the selection. Replacing it starts a fresh selection and kills the old one. Breaking the source drops its packet once and kills linked plants, including pending cleanup when owned entities are unloaded.
- Default one plant, 90-second initial insertion delay, 180-second death/taming replacement delay, 3-block spawn radius and 6-block roam radius. A new selection starts a fresh initial wait. Existing cooldowns survive menu closing/save/load.
- `config/pvzpawners-plantz.json` is independent; fields are `respawnDelaySeconds`, `spawnRadius`, `roamRadius`, `maxWeeds` (generic entity cap). Existing source configs/saves remain compatible.
- Validation uses `SeedPacketItem.Companion.typeFromStack`, checks the canonical packet item returned by `stackFor`, then checks `Plant.canWalk()` on a temporary, unspawned entity. Immobile plants and non-packets (even ordinary items with spoofed entity data) are rejected by the server container and menu slot. Capability results are cached by entity type in a concurrent map; no English name matching or guessed tags. This checks the dependency's movement API, not whether terrain/temporary combat behavior lets a particular mob move at every instant.
- Only the entity type is selected; arbitrary saved entity NBT from a packet is not applied to the spawned mob. This prevents ownership/NoAI/UUID data from bypassing the spawner rules. Seed-packet special placement/upgrade actions are not executed.
- Upgrade recipe uses either existing spawner in the middle and eight Bottles of Sun around it; Creative/`give` also remain available.

### New files

Paths relative to `src/main/java/net/amemah/pvzspawners/`:

| File | Purpose |
| --- | --- |
| `block/PlantzSpawnerBlock.java` | Existing slab/mining behavior plus block entity and right-click menu opening. |
| `menu/SpawnerMenus.java` | Registers the menu and persistent block-entity types. |
| `menu/PlantzSpawnerBlockEntity.java` | Stores/saves the packet, validates insertion, handles selection changes, drops contents on destruction. |
| `menu/PlantzSpawnerMenu.java` | Server-synchronized one-slot menu, inventory slots, one-item limit and shift-click rules. |
| `client/PvzpawnersClient.java` | Registers the screen only on the client; dedicated servers do not load screen classes. |
| `client/PlantzSpawnerScreen.java` | Compact vanilla-style panel and slot borders without a new GUI texture asset. |
| `spawner/SeedPacketFilter.java` | Authoritative packet-to-type and movement-capability checks. |

Resources added under `src/main/resources/`: `assets/pvzpawners/blockstates/plantz_spawner.json`, `assets/pvzpawners/models/item/plantz_spawner.json`, `assets/pvzpawners/items/plantz_spawner.json`, and `data/pvzpawners/loot_table/blocks/plantz_spawner.json`. Existing model `assets/pvzpawners/models/block/plantz_spawner.json` and custom top PNG are preserved apart from fixing texture paths. Block/item/Creative registrations, translations, shovel tag, config and client entrypoint were extended.

The shared manager now operates on the base `Plant` API rather than only the extension's `GWPlant`, retaining GW-specific wander commands only where supported. Persistent ownership/death handling, range boundaries, normal deaths, smoke and anti-taming remain shared for all source kinds. Only PLANTZ sources resolve their type from a block entity's packet.

`src/gametest/java/net/amemah/pvzspawners/spawner/PlantzSpawnerGameTests.java` covers immobile/non-packet rejection, server/menu validation and shift-click limits, inactive empty sources, one matching entity, retained packet, three-minute replacement deadline/respawn, packet serialization, selection replacement/removal, normal source-break death and packet/block drops. Client screen appearance remains a manual check.

Verification: full Gradle `build` passed with **20 server GameTests** (19 addon tests plus Fabric's test) and **12 JUnit tests**. Plantz tests verify actual right-click menu opening, closing without losing the packet, initial/full replacement cooldowns, and anti-taming. The release JAR contains the screen, menu/block entity, supplied model/PNG, item resources, upgrade recipe and loot table, and excludes test classes. Generated config was confirmed as 180/3/6/1. Dedicated-server startup passed; client screen layout/visuals still need an in-game check.

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

## Dandelion Weed Spawner addition

- ID: `pvzpawners:dandelion_weed_spawner`; entity: `pea_extension:dandelion_weed`; seed packet: `pea_extension:dandelion_weed_packet`.
- Defaults: two living entities, 20-second replacement cooldown, 3-block spawn radius, 6-block roam radius. Independent settings use `config/pvzpawners-dandelion.json`, with the same validated field names as the regular config (`maxWeeds` is the entity cap for that file).
- Shares dirt-like mining, half-slab collision, smoke/spawn effect, normal death when its source is destroyed, anti-taming death/cooldown, unloaded-entity ownership and pending-death persistence. No normal Weed variant accessor is applied to Dandelion Weeds.
- `spawner/SpawnerKind.java` contains per-source entity/settings differences. The existing block class, manager and SavedData now share behavior for both kinds instead of maintaining divergent copies. SavedData writes the source kind; old records without it remain regular Weed sources. Existing regular config is not replaced or migrated.
- Registered both block/item pairs in `block/ModBlocks.java`; both appear in Functional Blocks. The shovel tag and English translations include the new block.
- Added `assets/pvzpawners/blockstates/dandelion_weed_spawner.json`, `assets/pvzpawners/models/item/dandelion_weed_spawner.json`, `assets/pvzpawners/items/dandelion_weed_spawner.json`, `data/pvzpawners/recipe/dandelion_weed_spawner.json` and `data/pvzpawners/loot_table/blocks/dandelion_weed_spawner.json` under `src/main/resources/`.
- Uses the user's existing `models/block/dandelion_weed_spawner.json` and `textures/block/dandelion_weed_spawner_top.png` under `assets/pvzpawners/`. Only texture resource identifiers were corrected; geometry, UVs and PNG art were preserved. The item GUI rotates 90 degrees to show the top, like the regular spawner.
- Recipe: `WWW / WDW / DDD`, consuming five Dandelion Weed Seed Packets and four Dirt to produce one Dandelion Weed Spawner.
- `src/gametest/java/net/amemah/pvzspawners/spawner/DandelionSpawnerGameTests.java` checks the entity type/cap, 400-tick cooldown and replacement, range enforcement, anti-taming/source-break deaths, saved source kind, recipe/output, mining/drop behavior, and mixed source limits/cooldowns.
- Verification: Gradle `build` passed with all **14 server GameTests** (13 addon tests plus Fabric's test), including all existing regular Weed tests. Confirmed the generated dandelion config has 20/3/6/2 defaults and the release JAR contains its model, PNG, item definition, blockstate, recipe and loot table. Existing save records without a kind field are explicitly tested as regular Weed sources. Client rendering remains a manual check.

## Regular Weed Spawner file inventory

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
- Three slots per spawner by default. Initial slots wait half the configured delay rounded down to whole seconds (30 seconds by default), then fill when safe ground is available and the source is ticking (checked once per second).
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
- **12 JUnit tests passed** (timer/ownership state, circular range checks, config validation and whole-second initial-delay rounding).
- **19 addon GameTests passed** in a real headless Minecraft server: eleven regular Weed tests, three dandelion tests, and five generic Plantz tests covering packet filtering, inventory/menu behavior, selection lifecycle, initial/full cooldowns and the upgrade recipe. The runner reported **20 passed** including Fabric's own test.
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
