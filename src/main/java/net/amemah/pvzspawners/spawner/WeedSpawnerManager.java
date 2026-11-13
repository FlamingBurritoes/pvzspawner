package net.amemah.pvzspawners.spawner;

import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.config.WeedSpawnerConfig;
import net.amemah.pvzspawners.mixin.WeedVariantAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import zombieman.pea_extension.PeaExtensionEntities;
import zombieman.pea_extension.entity.GWPlant;
import zombieman.pea_extension.entity.Weed;

import java.util.Map;
import java.util.UUID;

public final class WeedSpawnerManager {
	private static final Weed.Variant[] VARIANTS = Weed.Variant.values();
	private static final int SPAWN_ATTEMPTS = 48;

	private WeedSpawnerManager() {
	}

	/** Called before normal taming grants ownership; UUID ownership, not nearby distance, determines scope. */
	public static boolean despawnForTaming(Entity entity) {
		if (!(entity instanceof Weed weed) || weed.isRemoved() || !(weed.level() instanceof ServerLevel level)) {
			return false;
		}
		WeedSpawnerData data = WeedSpawnerData.get(level);
		if (data.find(weed.getUUID()) == null) {
			return false;
		}
		// Start the replacement cooldown exactly once, then use the normal death animation/sound.
		data.recordDeath(weed.getUUID(), level.getGameTime());
		weed.kill(level);
		return true;
	}

	/** Stop physics/AI/knockback from walking across the boundary, rather than relying only on recall. */
	public static Vec3 limitMovement(Entity entity, Vec3 movement) {
		if (!(entity instanceof Weed) || !entity.entityTags().contains(WeedSpawnerData.MANAGED_TAG)
			|| !(entity.level() instanceof ServerLevel level)) {
			return movement;
		}
		var owner = WeedSpawnerData.get(level).find(entity.getUUID());
		if (owner == null) {
			return movement;
		}
		BlockPos origin = owner.spawner().pos();
		int radius = WeedSpawnerConfig.get().roamRadius();
		Vec3 next = entity.position().add(movement);
		double x = movement.x;
		double z = movement.z;
		if (!SpawnerRange.contains(origin.getX() + 0.5, origin.getZ() + 0.5, next.x, next.z, radius)) {
			x = 0;
			z = 0;
		}
		double y = Math.abs(next.y - origin.getY()) <= radius ? movement.y : 0;
		return new Vec3(x, y, z);
	}

	public static void initialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> WeedSpawnerConfig.load());
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Weed && entity.level() instanceof ServerLevel level) {
				WeedSpawnerData.get(level).recordDeath(entity.getUUID(), level.getGameTime());
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			// Chunk unloading and server shutdown do NOT free the slot. A real discard does.
			if (entity instanceof Weed && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
				WeedSpawnerData.get(level).recordDeath(entity.getUUID(), level.getGameTime());
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Weed weed) {
				WeedSpawnerData data = WeedSpawnerData.get(level);
				if (data.consumePendingDeath(weed.getUUID())) {
					weed.kill(level);
				} else if (data.consumeRetired(weed.getUUID())) {
					weed.discard();
				} else if (weed.isTame() && data.find(weed.getUUID()) != null) {
					despawnForTaming(weed);
				} else if (weed.entityTags().contains(WeedSpawnerData.MANAGED_TAG) && data.find(weed.getUUID()) == null) {
					WeedSpawnerData.release(weed);
				}
			}
		});
		ServerTickEvents.END_LEVEL_TICK.register(WeedSpawnerManager::tick);
	}

	private static void tick(ServerLevel level) {
		WeedSpawnerData data = WeedSpawnerData.get(level);
		// This pass also runs when the source block's chunk is not ticking.
		for (Map.Entry<UUID, WeedSpawnerData.Ownership> entry : data.ownership().entrySet()) {
			Entity entity = level.getEntity(entry.getKey());
			if (entity instanceof Weed weed && weed.isAlive()) {
				if (weed.isTame()) {
					// Fallback for old saves or mods/commands that set the tame flag without calling tame().
					despawnForTaming(weed);
				} else {
					confine(level, data, entry.getValue().spawner().pos(), weed);
				}
			}
		}
		if (level.getGameTime() % 20 != 0) {
			return;
		}
		for (WeedSpawnerData.Spawner spawner : data.spawners()) {
			if (!level.hasChunkAt(spawner.pos())) {
				continue;
			}
			if (!level.getBlockState(spawner.pos()).is(ModBlocks.WEED_SPAWNER)) {
				data.unregister(level, spawner.pos());
				continue;
			}
			if (!level.isPositionEntityTicking(spawner.pos())) {
				continue;
			}
			data.resize(spawner, level);
			for (SpawnSlot slot : spawner.slots()) {
				if (slot.isReady(level.getGameTime())) {
					spawn(level, data, spawner, slot);
				}
			}
		}
	}

	private static void spawn(ServerLevel level, WeedSpawnerData data, WeedSpawnerData.Spawner spawner, SpawnSlot slot) {
		Weed weed = PeaExtensionEntities.WEED.create(level, EntitySpawnReason.SPAWNER);
		if (weed == null) {
			return;
		}
		Weed.Variant variant = VARIANTS[level.getRandom().nextInt(VARIANTS.length)];
		weed.getEntityData().set(WeedVariantAccessor.pvzpawners$variant(), (byte) variant.getId());
		weed.getEntityData().set(WeedVariantAccessor.pvzpawners$shieldPool(), variant.getShieldPool());
		Vec3 location = findSafePosition(level, spawner.pos(), weed, WeedSpawnerConfig.get().spawnRadius());
		if (location == null) {
			return; // Keep this slot empty; retry later without shortening a death cooldown.
		}
		// Do not call Weed.finalizeSpawn: it has a wild-group hook that can spawn untracked extras.
		weed.snapTo(location, level.getRandom().nextFloat() * 360, 0);
		weed.setPersistenceRequired();
		weed.setHomeTo(spawner.pos(), WeedSpawnerConfig.get().roamRadius());
		weed.setCommand(GWPlant.Command.WANDER);
		if (level.addFreshEntity(weed)) {
			data.occupy(spawner, slot, weed.getUUID());
			weed.entityTags().add(WeedSpawnerData.MANAGED_TAG);
			data.lastInside.put(weed.getUUID(), location);
			level.sendParticles(ParticleTypes.SMOKE, weed.getX(), weed.getY() + weed.getBbHeight() * 0.5,
				weed.getZ(), 20, 0.35, weed.getBbHeight() * 0.35, 0.35, 0.02);
			weed.spawnAnim();
		}
	}

	private static void confine(ServerLevel level, WeedSpawnerData data, BlockPos origin, Weed weed) {
		int radius = WeedSpawnerConfig.get().roamRadius();
		weed.setHomeTo(origin, radius);
		if (weed.getTarget() != null && !inside(origin, weed.getTarget().position(), radius)) {
			weed.setTarget(null);
		}
		if (inside(origin, weed.position(), radius)) {
			if (isSafe(level, weed, weed.position())) {
				data.lastInside.put(weed.getUUID(), weed.position());
			}
			return;
		}
		Vec3 destination = data.lastInside.get(weed.getUUID());
		if (destination == null || !inside(origin, destination, radius) || !isSafe(level, weed, destination)) {
			destination = findSafePosition(level, origin, weed, radius);
		}
		weed.getNavigation().stop();
		weed.setTarget(null);
		weed.setDeltaMovement(Vec3.ZERO);
		if (destination != null) {
			weed.teleportTo(destination.x, destination.y, destination.z);
			weed.fallDistance = 0;
			data.lastInside.put(weed.getUUID(), destination);
		}
	}

	private static boolean inside(BlockPos origin, Vec3 position, int radius) {
		return SpawnerRange.contains(origin.getX() + 0.5, origin.getZ() + 0.5, position.x, position.z, radius)
			&& Math.abs(position.y - origin.getY()) <= radius;
	}

	private static Vec3 findSafePosition(ServerLevel level, BlockPos origin, Weed weed, int radius) {
		for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
			int dx = level.getRandom().nextInt(radius * 2 + 1) - radius;
			int dz = level.getRandom().nextInt(radius * 2 + 1) - radius;
			if ((dx == 0 && dz == 0) || dx * dx + dz * dz > radius * radius) {
				continue;
			}
			// Try nearby terrain, not the top of the world's heightmap (which could be a distant roof).
			for (int dy = 3; dy >= -3; dy--) {
				Vec3 candidate = new Vec3(origin.getX() + dx + 0.5, origin.getY() + dy, origin.getZ() + dz + 0.5);
				if (inside(origin, candidate, radius) && isSafe(level, weed, candidate)) {
					return candidate;
				}
			}
		}
		return null;
	}

	private static boolean isSafe(ServerLevel level, Weed weed, Vec3 location) {
		BlockPos feet = BlockPos.containing(location);
		if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.above()) || !level.getWorldBorder().isWithinBounds(feet)) {
			return false;
		}
		BlockPos ground = feet.below();
		if (!weed.canSurviveOn(level.getBlockState(ground))
			|| !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
			|| !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
			return false;
		}
		var box = weed.getBoundingBox().move(location.subtract(weed.position()));
		return level.noCollision(weed, box) && level.getEntities(weed, box, Entity::isAlive).isEmpty();
	}
}
