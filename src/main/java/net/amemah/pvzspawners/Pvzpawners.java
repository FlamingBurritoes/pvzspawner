package net.amemah.pvzspawners;

import net.fabricmc.api.ModInitializer;
import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.spawner.WeedSpawnerManager;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pvzpawners implements ModInitializer {
	public static final String MOD_ID = "pvzpawners";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		WeedSpawnerManager.initialize();
		LOGGER.info("Initialized Weed Spawner");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
