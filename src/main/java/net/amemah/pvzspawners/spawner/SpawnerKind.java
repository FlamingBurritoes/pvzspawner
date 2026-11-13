package net.amemah.pvzspawners.spawner;

import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.config.WeedSpawnerConfig;
import net.amemah.pvzspawners.menu.PlantzSpawnerBlockEntity;
import joshxviii.plantz.entity.plant.Plant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import zombieman.pea_extension.PeaExtensionEntities;

/** Per-block differences; ownership, spawning and lifecycle code remain shared. */
public enum SpawnerKind {
	WEED,
	DANDELION_WEED,
	PLANTZ;

	public WeedSpawnerConfig.Settings settings() {
		return switch (this) {
			case WEED -> WeedSpawnerConfig.get();
			case DANDELION_WEED -> WeedSpawnerConfig.getDandelion();
			case PLANTZ -> WeedSpawnerConfig.getPlantz();
		};
	}

	public Plant create(ServerLevel level, BlockPos pos) {
		if (this == PLANTZ) {
			if (!(level.getBlockEntity(pos) instanceof PlantzSpawnerBlockEntity blockEntity)) return null;
			var type = SeedPacketFilter.resolve(level, blockEntity.packet().getItem(0));
			if (type == null) return null;
			var entity = type.create(level, EntitySpawnReason.SPAWNER);
			return entity instanceof Plant plant && plant.canWalk() ? plant : null;
		}
		return this == WEED ? PeaExtensionEntities.WEED.create(level, EntitySpawnReason.SPAWNER)
			: PeaExtensionEntities.DANDELION_WEED.create(level, EntitySpawnReason.SPAWNER);
	}

	public boolean matches(BlockState state) {
		if (this == PLANTZ) return state.is(ModBlocks.PLANTZ_SPAWNER);
		return state.is(this == WEED ? ModBlocks.WEED_SPAWNER : ModBlocks.DANDELION_WEED_SPAWNER);
	}

	public static SpawnerKind forBlock(BlockState state) {
		if (state.is(ModBlocks.PLANTZ_SPAWNER)) return PLANTZ;
		return state.is(ModBlocks.DANDELION_WEED_SPAWNER) ? DANDELION_WEED : WEED;
	}
}
