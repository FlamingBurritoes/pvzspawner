package net.amemah.pvzspawners.spawner;

import com.mojang.serialization.Codec;
import net.amemah.pvzspawners.Pvzpawners;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dimension-local ownership survives unloaded source chunks and server restarts. */
public final class WeedSpawnerData extends SavedData {
	public static final String MANAGED_TAG = "pvzpawners:spawner_weed";
	static final Codec<WeedSpawnerData> CODEC = CompoundTag.CODEC.xmap(WeedSpawnerData::read, WeedSpawnerData::write);
	private static final SavedDataType<WeedSpawnerData> TYPE = new SavedDataType<>(
		Pvzpawners.id("weed_spawners"), WeedSpawnerData::new, CODEC, null);
	private final Map<BlockPos, Spawner> spawners = new HashMap<>();
	private final Map<UUID, Ownership> ownership = new HashMap<>();
	private final Set<UUID> retired = new HashSet<>();
	private final Set<UUID> pendingDeaths = new HashSet<>();
	// Transient safe return points are refreshed when an owned mob is loaded and inside its range.
	final Map<UUID, Vec3> lastInside = new HashMap<>();

	public record Spawner(BlockPos pos, List<SpawnSlot> slots, SpawnerKind kind) {
	}

	public record Ownership(Spawner spawner, SpawnSlot slot) {
	}

	public static WeedSpawnerData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Spawner> spawners() {
		return List.copyOf(spawners.values());
	}

	public Map<UUID, Ownership> ownership() {
		return Map.copyOf(ownership);
	}

	public Ownership find(UUID entityId) {
		return ownership.get(entityId);
	}

	public void register(BlockPos pos) {
		register(pos, SpawnerKind.WEED);
	}

	public void register(BlockPos pos, SpawnerKind kind) {
		if (!spawners.containsKey(pos)) {
			List<SpawnSlot> slots = new ArrayList<>();
			for (int i = 0; i < kind.settings().maxWeeds(); i++) {
				slots.add(new SpawnSlot(null, 0));
			}
			spawners.put(pos.immutable(), new Spawner(pos.immutable(), slots, kind));
			setDirty();
		}
	}

	public void occupy(Spawner spawner, SpawnSlot slot, UUID entityId) {
		slot.occupy(entityId);
		ownership.put(entityId, new Ownership(spawner, slot));
		setDirty();
	}

	public void recordDeath(UUID entityId, long gameTime) {
		Ownership owner = ownership.remove(entityId);
		if (owner != null) {
			owner.slot().onDeath(gameTime, owner.spawner().kind().settings().respawnDelayTicks());
			lastInside.remove(entityId);
			setDirty();
		}
	}

	public void resize(Spawner spawner, ServerLevel level) {
		int maximum = spawner.kind().settings().maxWeeds();
		while (spawner.slots().size() > maximum) {
			SpawnSlot removed = spawner.slots().removeLast();
			if (removed.entityId() != null) {
				retire(level, removed.entityId(), false);
			}
			setDirty();
		}
		while (spawner.slots().size() < maximum) {
			spawner.slots().add(new SpawnSlot(null, 0));
			setDirty();
		}
	}

	public boolean consumeRetired(UUID uuid) {
		if (retired.remove(uuid)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean consumePendingDeath(UUID uuid) {
		if (pendingDeaths.remove(uuid)) {
			setDirty();
			return true;
		}
		return false;
	}

	public void unregister(ServerLevel level, BlockPos pos) {
		Spawner removed = spawners.remove(pos);
		if (removed == null) {
			return;
		}
		for (SpawnSlot slot : removed.slots()) {
			if (slot.entityId() != null) {
				retire(level, slot.entityId(), true);
			}
		}
		setDirty();
	}

	private void retire(ServerLevel level, UUID uuid, boolean normalDeath) {
		// Remove ownership first so death/removal callbacks cannot start a replacement cooldown.
		ownership.remove(uuid);
		lastInside.remove(uuid);
		Entity entity = level.getEntity(uuid);
		if (entity != null) {
			if (normalDeath) {
				entity.kill(level);
			} else {
				entity.discard();
			}
		} else {
			// Persist cleanup for unloaded weeds instead of force-loading their chunks.
			(normalDeath ? pendingDeaths : retired).add(uuid);
		}
	}

	public static void release(Mob mob) {
		mob.entityTags().remove(MANAGED_TAG);
		mob.clearHome();
	}

	private CompoundTag write() {
		CompoundTag root = new CompoundTag();
		ListTag entries = new ListTag();
		for (Spawner spawner : spawners.values()) {
			CompoundTag entry = new CompoundTag();
			entry.putLong("position", spawner.pos().asLong());
			entry.putString("kind", spawner.kind().name());
			ListTag slots = new ListTag();
			for (SpawnSlot slot : spawner.slots()) {
				CompoundTag savedSlot = new CompoundTag();
				if (slot.entityId() != null) {
					savedSlot.putString("entity", slot.entityId().toString());
				}
				savedSlot.putLong("ready_at", slot.readyAt());
				slots.add(savedSlot);
			}
			entry.put("slots", slots);
			entries.add(entry);
		}
		root.put("spawners", entries);
		ListTag retiredIds = new ListTag();
		for (UUID uuid : retired) {
			CompoundTag entry = new CompoundTag();
			entry.putString("entity", uuid.toString());
			retiredIds.add(entry);
		}
		root.put("retired", retiredIds);
		ListTag deaths = new ListTag();
		for (UUID uuid : pendingDeaths) {
			CompoundTag entry = new CompoundTag();
			entry.putString("entity", uuid.toString());
			deaths.add(entry);
		}
		root.put("pending_deaths", deaths);
		return root;
	}

	private static WeedSpawnerData read(CompoundTag root) {
		WeedSpawnerData data = new WeedSpawnerData();
		ListTag entries = root.getListOrEmpty("spawners");
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompoundOrEmpty(i);
			BlockPos pos = BlockPos.of(entry.getLongOr("position", 0));
			// Saves from the original Weed-only implementation have no kind field.
			SpawnerKind kind = SpawnerKind.valueOf(entry.getStringOr("kind", "WEED"));
			Spawner spawner = new Spawner(pos, new ArrayList<>(), kind);
			ListTag slots = entry.getListOrEmpty("slots");
			for (int j = 0; j < slots.size(); j++) {
				CompoundTag savedSlot = slots.getCompoundOrEmpty(j);
				UUID uuid = savedSlot.getString("entity").map(UUID::fromString).orElse(null);
				SpawnSlot slot = new SpawnSlot(uuid, savedSlot.getLongOr("ready_at", 0));
				spawner.slots().add(slot);
				if (uuid != null) {
					data.ownership.put(uuid, new Ownership(spawner, slot));
				}
			}
			data.spawners.put(pos, spawner);
		}
		ListTag retiredIds = root.getListOrEmpty("retired");
		for (int i = 0; i < retiredIds.size(); i++) {
			retiredIds.getCompoundOrEmpty(i).getString("entity").map(UUID::fromString).ifPresent(data.retired::add);
		}
		ListTag deaths = root.getListOrEmpty("pending_deaths");
		for (int i = 0; i < deaths.size(); i++) {
			deaths.getCompoundOrEmpty(i).getString("entity").map(UUID::fromString).ifPresent(data.pendingDeaths::add);
		}
		return data;
	}
}
