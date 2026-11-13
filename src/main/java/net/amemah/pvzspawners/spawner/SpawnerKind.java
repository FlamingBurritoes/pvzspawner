package net.amemah.pvzspawners.spawner;

import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.config.WeedSpawnerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import zombieman.pea_extension.PeaExtensionEntities;
import zombieman.pea_extension.entity.GWPlant;

/** Per-block differences; ownership, spawning and lifecycle code remain shared. */
public enum SpawnerKind {
	WEED,
	DANDELION_WEED;

	public WeedSpawnerConfig.Settings settings() {
		return this == WEED ? WeedSpawnerConfig.get() : WeedSpawnerConfig.getDandelion();
	}

	public GWPlant create(ServerLevel level) {
		return this == WEED ? PeaExtensionEntities.WEED.create(level, EntitySpawnReason.SPAWNER)
			: PeaExtensionEntities.DANDELION_WEED.create(level, EntitySpawnReason.SPAWNER);
	}

	public boolean matches(BlockState state) {
		return state.is(this == WEED ? ModBlocks.WEED_SPAWNER : ModBlocks.DANDELION_WEED_SPAWNER);
	}

	public static SpawnerKind forBlock(BlockState state) {
		return state.is(ModBlocks.DANDELION_WEED_SPAWNER) ? DANDELION_WEED : WEED;
	}
}
