# Minecraft modding: reusable lessons

This is a portable learning document for future Minecraft projects, not a specification for this particular mod. Add verified lessons as problems are discovered. It is not possible to guarantee mistake-free work; these checks prevent repeatable mistakes.

## Establish compatibility before editing

- Read the current project's instructions, Git status, build files and mod metadata first. Preserve existing user changes.
- Identify the exact Minecraft version, loader, mappings and Java requirement. Do not copy Forge, NeoForge, old Fabric or old NBT APIs into a different target version.
- Inspect the actual dependency JAR's `fabric.mod.json`, not just its download title. Project names, registry namespaces and Fabric mod IDs can differ.
- Verify compatible release pairs. Add required support mods explicitly when the Maven host does not publish transitive dependency metadata.
- Declare both build dependencies and loader-enforced runtime dependencies. Do not bundle another author's entire mod just to make local development work.
- In unobfuscated Minecraft 26.1+ Fabric projects, follow the installed Loom/template's `implementation` pattern rather than blindly using older `modImplementation` examples.

## Inspect behavior, not just names

- A variant may be entity data on one entity type, rather than a separate entity registry entry.
- A configurable entity spawner should consult the dependency's canonical packet-to-type mapping and capability API, not names alone. Keep menu slot filtering and server inventory validation aligned; reject fake items carrying valid-looking entity components. Capability checks should create an unspawned probe, not add mobs to the world.
- A constructor, spawn hook, seed item or AI goal can add randomization, ownership, squad spawning or movement constraints. Inspect those paths before reusing them.
- Never assume that `finalizeSpawn` creates only one mob, that all variants are equally likely, or that normal spawning and seed-packet placement are identical.
- Prefer public APIs. When an essential field has no public API, isolate a narrowly scoped mixin and document the dependency version it was checked against.

## Keep persistent ownership separate from proximity

- A nearby-entity count is not ownership. Overlapping spawners must track their own UUIDs.
- When adding a second spawner type, centralize shared lifecycle rules and persist the source kind alongside UUID ownership. Select cooldowns, caps and radii from that kind, including death events while the source is unloaded. Provide backward-compatible defaults for old saves and avoid replacing existing users' configuration.
- An unloaded entity is not dead. Do not refill a slot merely because a UUID cannot be found in loaded chunks.
- Persist ownership and respawn deadlines. Listen for confirmed deaths even when the owning block's chunk is unloaded.
- Decide whether cooldowns use world ticks or real time, whether initial slots fill immediately, and what happens when a source block is broken.
- Initial placement/selection cooldowns are distinct from death cooldowns. Calculate the initial deadline from the current world time, not zero, and persist it. Restart it on block replacement and selection reinsertion to prevent instant refill abuse; do not shorten later death cooldowns. Specify whether rounding happens in seconds or ticks and test odd values.
- When source destruction must remove its entities, clear ownership before discarding loaded entities and persist pending removals for unloaded UUIDs. Otherwise death/removal callbacks may schedule replacements or unloaded tamed mobs may survive cleanup. Test tame and wild mobs, nearby unrelated sources, and save/load of pending cleanup separately.
- Clarify whether “remove tamed mobs” means on source destruction or immediately on taming. These are different policies. To prevent pets, intercept the normal tame operation before ownership/advancements are granted, check the source UUID link, and keep a tick/load fallback for old saves or direct tame-state changes. Test that unrelated natural mobs remain tamable.
- AI home ranges are usually soft guidance, not hard barriers. If strict confinement is required, enforce it server-side as well, without loading chunks or teleporting mobs into solids.
- Never overwrite vanilla entity save fields such as `Tags` with a mod's unrelated data. Use namespaced storage.

## Resources and configuration

- A block model, outline/selection shape, collision shape and light-occlusion shape are different things. A full-size visual does not require full-block collision.
- Top-down block-item icons may look too dark under normal side/block lighting. Set `gui_light` to `front` on the item model instead of modifying PNG brightness or world lighting; verify the final appearance in a client.
- Matching a vanilla block's mining behavior needs more than hardness. Copy appropriate block properties, set the correct mining-tool tag, and verify hand drops and tool speed. Copying dirt's properties does not automatically copy its shovel tag.
- Modern inventory items may require an `assets/<namespace>/items/` definition in addition to a model. Match the exact version's resource layout.
- Inventory UIs need a persistent server container/menu plus a client-only screen. Persist actual slot contents; closing the UI must not erase them. Clear ownership before killing replaced selections, and clear/drop contents exactly once on block destruction. Minecraft 26.1 uses `ValueInput`/`ValueOutput`, `BlockEntity.preRemoveSideEffects`, and `GuiGraphicsExtractor`; older GUI rendering and container-listener examples may not apply.
- Fabric API names also change with versions/mappings: in the checked 26.1.2 API, creative additions use `CreativeModeTabEvents.modifyOutputEvent`, not older `ItemGroupEvents` examples; server world ticks are named `END_LEVEL_TICK`. `Level.random` is protected: use `getRandom()`. These mismatches were caught by compiling against the installed libraries.
- Use lowercase resource paths and valid namespaces. Reuse placeholder vanilla textures until original art is ready.
- Exported Java model JSON stores texture identifiers, not embedded PNGs. Renaming/moving a texture requires updating its resource reference; renaming a texture key requires updating face references too. Reference unchanged vanilla textures through `minecraft:block/...` rather than duplicating them into the mod.
- Bare exported texture names like `dirt` do not resolve to `minecraft:block/dirt`. Missing vanilla side/bottom textures can be incorrect paths, not missing PNG copies. Preserve custom UVs/geometry while repairing the identifiers.
- `discard()` skips the normal death presentation. Use normal lethal damage for a death animation/sound, and consider ordinary loot side effects. Start/clear ownership timers before death callbacks to avoid duplicate replacements. Broadcast spawn particles server-side only after an entity was successfully added.
- Validate configuration before applying it. Keep malformed user files intact, report problems clearly, and avoid allowing clients to override multiplayer server rules.
- Do not invent recipes, assets or gameplay rules that the user explicitly plans to supply later; document temporary assumptions.

## Verify honestly

- Confirm the actual installed JDK and Gradle JVM. A terminal may lack `java` on `PATH` even when the IDE has a working JDK; use a process-local `JAVA_HOME` rather than changing the user's machine settings.
- A stale IDE import/index can report unresolved Minecraft classes even while Gradle compiles and real server tests pass. Refresh the Gradle model and let indexing finish before changing otherwise verified code to satisfy editor diagnostics.
- Verify the imported module actually references the Minecraft JAR and that the JAR contains the missing classes before diagnosing a missing dependency. If both are present and Gradle succeeds, use IDE sync/cache repair rather than rewriting imports, deleting user project metadata, or editing IntelliJ's live internal workspace caches.
- If normal invalidation fails, distinguish corrupt archives from a stale archive/filesystem view: validate the actual JAR and ask whether its classes appear in library searches. Any manual IDE-cache repair requires explicit consent and a fully closed IDE. Rename only the identified cache directory to a backup; preserve project metadata, settings and Local History, and verify the editor result after reopening rather than assuming success.
- In this case, the JAR was healthy and standard index invalidation was insufficient. A reversible rename of the closed IDE's `caches` directory followed by reopening resolved the missing Minecraft classes, confirmed by the user. No Minecraft dependency, import rewrite or project deletion was needed.
- Build after changes and check that resources and runtime dependencies resolve. Compilation alone does not prove mixins, rendering or gameplay work.
- Test on a dedicated server too: accidental client-only imports can hide in single-player development.
- Put integration tests in a separate test mod/source set, not in the released mod. Fabric server GameTests can exercise real registries, mixins and entity AI; keep fast unit tests for timer and validation edge cases too. Do not accept the Minecraft EULA on someone else's behalf when configuring test clients or normal servers.
- Cover death, unload/reload, save/restart, overlapping ownership, invalid config and obstructed spawn locations. State clearly which checks were run and which remain manual.
- Cloud-synced folders can interfere with Gradle and file locks. If Loom warns about OneDrive, suggest a nonsynced workspace instead of relocating user files without permission.
