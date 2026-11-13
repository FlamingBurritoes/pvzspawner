package net.amemah.pvzspawners.spawner;

import joshxviii.plantz.entity.plant.Plant;
import joshxviii.plantz.item.SeedPacketItem;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Consult the dependency's packet mapping and actual plant API, not localized names or guessed tags. */
public final class SeedPacketFilter {
	private static final Map<EntityType<?>, Boolean> MOVABLE = new ConcurrentHashMap<>();
	private SeedPacketFilter() { }

	public static EntityType<?> resolve(Level level, ItemStack stack) {
		if (stack.isEmpty()) return null;
		EntityType<?> type = SeedPacketItem.Companion.typeFromStack(stack);
		if (type == null || !stack.is(SeedPacketItem.Companion.stackFor(type).getItem())) return null;
		boolean movable = MOVABLE.computeIfAbsent(type, key -> {
			var probe = key.create(level, EntitySpawnReason.SPAWNER);
			return probe instanceof Plant plant && plant.canWalk();
		});
		return movable ? type : null;
	}
}
