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

	private ModBlocks() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> entries.accept(WEED_SPAWNER_ITEM));
	}
}
