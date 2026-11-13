package net.amemah.pvzspawners.menu;

import net.amemah.pvzspawners.Pvzpawners;
import net.amemah.pvzspawners.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SpawnerMenus {
	public static final MenuType<PlantzSpawnerMenu> PLANTZ = Registry.register(BuiltInRegistries.MENU,
		Pvzpawners.id("plantz_spawner"), new MenuType<>(PlantzSpawnerMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final BlockEntityType<PlantzSpawnerBlockEntity> BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Pvzpawners.id("plantz_spawner"), FabricBlockEntityTypeBuilder.create(PlantzSpawnerBlockEntity::new, ModBlocks.PLANTZ_SPAWNER).build());
	private SpawnerMenus() { }
	public static void initialize() { }
}
