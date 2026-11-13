package net.amemah.pvzspawners.block;

import net.amemah.pvzspawners.Pvzpawners;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	public static final Block WEED_SPAWNER = Registry.register(BuiltInRegistries.BLOCK,
		Pvzpawners.id("weed_spawner"), new WeedSpawnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
			.setId(ResourceKey.create(Registries.BLOCK, Pvzpawners.id("weed_spawner")))
			.noOcclusion()));
	public static final Item WEED_SPAWNER_ITEM = Registry.register(BuiltInRegistries.ITEM,
		Pvzpawners.id("weed_spawner"), new BlockItem(WEED_SPAWNER, new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, Pvzpawners.id("weed_spawner")))
			.useBlockDescriptionPrefix()));
	public static final Block DANDELION_WEED_SPAWNER = Registry.register(BuiltInRegistries.BLOCK,
		Pvzpawners.id("dandelion_weed_spawner"), new WeedSpawnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
			.setId(ResourceKey.create(Registries.BLOCK, Pvzpawners.id("dandelion_weed_spawner")))
			.noOcclusion()));
	public static final Item DANDELION_WEED_SPAWNER_ITEM = Registry.register(BuiltInRegistries.ITEM,
		Pvzpawners.id("dandelion_weed_spawner"), new BlockItem(DANDELION_WEED_SPAWNER, new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, Pvzpawners.id("dandelion_weed_spawner")))
			.useBlockDescriptionPrefix()));

	private ModBlocks() {
	}
	public static final Block PLANTZ_SPAWNER = Registry.register(BuiltInRegistries.BLOCK,
		Pvzpawners.id("plantz_spawner"), new PlantzSpawnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
			.setId(ResourceKey.create(Registries.BLOCK, Pvzpawners.id("plantz_spawner"))).noOcclusion()));
	public static final Item PLANTZ_SPAWNER_ITEM = Registry.register(BuiltInRegistries.ITEM,
		Pvzpawners.id("plantz_spawner"), new BlockItem(PLANTZ_SPAWNER, new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, Pvzpawners.id("plantz_spawner"))).useBlockDescriptionPrefix()));

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> {
				entries.accept(WEED_SPAWNER_ITEM);
				entries.accept(DANDELION_WEED_SPAWNER_ITEM);
				entries.accept(PLANTZ_SPAWNER_ITEM);
				entries.accept(RENEWABLE_PLANT_POT_ITEM);
			});
	}
	public static final Block RENEWABLE_PLANT_POT = Registry.register(BuiltInRegistries.BLOCK,
		Pvzpawners.id("renewable_plant_pot"), new RenewablePlantPotBlock(BlockBehaviour.Properties.ofFullCopy(joshxviii.plantz.PazBlocks.PLANT_POT)
			.setId(ResourceKey.create(Registries.BLOCK, Pvzpawners.id("renewable_plant_pot"))).noOcclusion()));
	public static final Item RENEWABLE_PLANT_POT_ITEM = Registry.register(BuiltInRegistries.ITEM,
		Pvzpawners.id("renewable_plant_pot"), new BlockItem(RENEWABLE_PLANT_POT, new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, Pvzpawners.id("renewable_plant_pot"))).useBlockDescriptionPrefix()));
}
