package net.amemah.pvzspawners.pot;

import com.mojang.serialization.Codec;
import net.amemah.pvzspawners.Pvzpawners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dimension-local links are retained even while the pot or plant is unloaded. */
public final class RenewablePotData extends SavedData {
	static final Codec<RenewablePotData> CODEC = CompoundTag.CODEC.xmap(RenewablePotData::read, RenewablePotData::write);
	private static final SavedDataType<RenewablePotData> TYPE = new SavedDataType<>(
		Pvzpawners.id("renewable_pots"), RenewablePotData::new, CODEC, null);
	private final Map<BlockPos, Reference> pots = new HashMap<>();
	private final Set<UUID> pendingDeaths = new HashSet<>();

	public record Reference(BlockPos pos, EntityType<?> type, UUID planter, UUID entity, long readyAt) { }

	public static RenewablePotData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public Reference at(BlockPos pos) { return pots.get(pos); }
	public List<Reference> references() { return List.copyOf(pots.values()); }
	public Reference find(UUID uuid) {
		return pots.values().stream().filter(ref -> uuid.equals(ref.entity())).findFirst().orElse(null);
	}
	public void adopt(BlockPos pos, EntityType<?> type, UUID planter, UUID entity) {
		pots.put(pos.immutable(), new Reference(pos.immutable(), type, planter, entity, 0));
		setDirty();
	}
	public void death(UUID uuid, long now) {
		Reference ref = find(uuid);
		if (ref != null) {
			pots.put(ref.pos(), new Reference(ref.pos(), ref.type(), ref.planter(), null, now + 1200));
			setDirty();
		}
	}
	public void clear(UUID uuid) {
		Reference ref = find(uuid);
		if (ref != null) { pots.remove(ref.pos()); setDirty(); }
	}
	public void remove(ServerLevel level, BlockPos pos) {
		Reference ref = pots.remove(pos);
		if (ref == null) return;
		setDirty();
		if (ref.entity() != null) {
			var entity = level.getEntity(ref.entity());
			if (entity != null) entity.kill(level);
			else pendingDeaths.add(ref.entity());
		}
	}
	public boolean consumePendingDeath(UUID uuid) {
		if (!pendingDeaths.remove(uuid)) return false;
		setDirty();
		return true;
	}
	private CompoundTag write() {
		CompoundTag root = new CompoundTag();
		ListTag entries = new ListTag();
		for (Reference ref : pots.values()) {
			CompoundTag entry = new CompoundTag();
			entry.putLong("position", ref.pos().asLong());
			entry.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(ref.type()).toString());
			entry.putString("planter", ref.planter().toString());
			if (ref.entity() != null) entry.putString("entity", ref.entity().toString());
			entry.putLong("ready_at", ref.readyAt());
			entries.add(entry);
		}
		root.put("pots", entries);
		ListTag deaths = new ListTag();
		for (UUID uuid : pendingDeaths) {
			CompoundTag entry = new CompoundTag();
			entry.putString("entity", uuid.toString());
			deaths.add(entry);
		}
		root.put("pending_deaths", deaths);
		return root;
	}
	private static RenewablePotData read(CompoundTag root) {
		RenewablePotData data = new RenewablePotData();
		ListTag entries = root.getListOrEmpty("pots");
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompoundOrEmpty(i);
			var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entry.getStringOr("type", "")));
			if (type.isEmpty()) continue;
			BlockPos pos = BlockPos.of(entry.getLongOr("position", 0));
			data.pots.put(pos, new Reference(pos, type.get(), UUID.fromString(entry.getStringOr("planter", "")),
				entry.getString("entity").map(UUID::fromString).orElse(null), entry.getLongOr("ready_at", 0)));
		}
		ListTag deaths = root.getListOrEmpty("pending_deaths");
		for (int i = 0; i < deaths.size(); i++) {
			deaths.getCompoundOrEmpty(i).getString("entity").map(UUID::fromString).ifPresent(data.pendingDeaths::add);
		}
		return data;
	}
}
