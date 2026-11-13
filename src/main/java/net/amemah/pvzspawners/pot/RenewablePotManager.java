package net.amemah.pvzspawners.pot;

import joshxviii.plantz.entity.plant.Plant;
import net.amemah.pvzspawners.block.ModBlocks;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

public final class RenewablePotManager {
	private RenewablePotManager() { }
	public static BlockPos support(Entity entity) {
		return BlockPos.containing(entity.getX(), entity.getY() - 0.001, entity.getZ());
	}
	public static void planted(Entity entity, Player player) {
		if (entity instanceof Plant plant && !plant.canWalk() && entity.level() instanceof ServerLevel level
			&& level.getBlockState(support(entity)).is(ModBlocks.RENEWABLE_PLANT_POT)) {
			plant.setPersistenceRequired();
			RenewablePotData.get(level).adopt(support(entity), entity.getType(), player.getUUID(), entity.getUUID());
		}
	}
	public static void clearForShovel(Entity entity) {
		if (entity.level() instanceof ServerLevel level) RenewablePotData.get(level).clear(entity.getUUID());
	}
	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Plant && entity.level() instanceof ServerLevel level)
				RenewablePotData.get(level).death(entity.getUUID(), level.getGameTime());
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Plant && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy())
				RenewablePotData.get(level).death(entity.getUUID(), level.getGameTime());
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Plant && RenewablePotData.get(level).consumePendingDeath(entity.getUUID())) entity.kill(level);
		});
		ServerTickEvents.END_LEVEL_TICK.register(RenewablePotManager::tick);
	}
	private static void tick(ServerLevel level) {
		if (level.getGameTime() % 20 != 0) return;
		var data = RenewablePotData.get(level);
		for (var ref : data.references()) {
			if (!level.hasChunkAt(ref.pos())) continue;
			if (!level.getBlockState(ref.pos()).is(ModBlocks.RENEWABLE_PLANT_POT)) {
				data.remove(level, ref.pos());
				continue;
			}
			// Missing/unloaded UUIDs are not proof of death. Only death/removal callbacks free the slot.
			if (ref.entity() != null || level.getGameTime() < ref.readyAt() || !level.isPositionEntityTicking(ref.pos())) continue;
			var entity = ref.type().create(level, EntitySpawnReason.SPAWNER);
			if (!(entity instanceof Plant plant) || plant.canWalk()) continue;
			float angle = level.getBlockState(ref.pos()).getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
			plant.snapTo(ref.pos().getX() + 0.5, ref.pos().getY() + 1, ref.pos().getZ() + 0.5, angle, 0);
			plant.yHeadRot = angle;
			plant.yBodyRot = angle;
			if (!plant.canPlaceOn(level.getBlockState(ref.pos())) || !level.noCollision(plant, plant.getBoundingBox())
				|| !level.getEntitiesOfClass(Plant.class, new AABB(ref.pos().above()), Entity::isAlive).isEmpty()) continue;
			plant.setPersistenceRequired();
			// Restore normal planter ownership, including the dependency's healing and shovel permissions.
			plant.setOwnerReference(EntityReference.of(ref.planter()));
			plant.setTame(true, true);
			if (level.addFreshEntity(plant)) {
				data.adopt(ref.pos(), ref.type(), ref.planter(), plant.getUUID());
				level.sendParticles(ParticleTypes.SMOKE, plant.getX(), plant.getY() + plant.getBbHeight() * 0.5,
					plant.getZ(), 20, 0.35, plant.getBbHeight() * 0.35, 0.35, 0.02);
				plant.spawnAnim();
			}
		}
	}
}
