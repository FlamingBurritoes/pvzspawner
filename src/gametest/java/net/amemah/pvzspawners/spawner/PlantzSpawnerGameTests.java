package net.amemah.pvzspawners.spawner;

import joshxviii.plantz.PazEntities;
import joshxviii.plantz.PazItems;
import net.amemah.pvzspawners.Pvzpawners;
import joshxviii.plantz.item.SeedPacketItem;
import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.menu.PlantzSpawnerBlockEntity;
import net.amemah.pvzspawners.menu.PlantzSpawnerMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import joshxviii.plantz.entity.plant.Plant;
import zombieman.pea_extension.PeaExtensionEntities;
import zombieman.pea_extension.PeaExtensionItems;

import java.util.List;
import java.util.ArrayList;

public class PlantzSpawnerGameTests {
	@GameTest
	public void upgradeRecipeAcceptsEitherSpawnerAndEightSunBottles(GameTestHelper helper) {
		for (var base : List.of(ModBlocks.WEED_SPAWNER_ITEM, ModBlocks.DANDELION_WEED_SPAWNER_ITEM)) {
			List<ItemStack> ingredients = new ArrayList<>();
			for (int slot = 0; slot < 9; slot++) ingredients.add(new ItemStack(slot == 4 ? base : PazItems.SUN_BOTTLE));
			var input = CraftingInput.of(3, 3, ingredients);
			var recipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
			helper.assertTrue(recipe.id().identifier().equals(Pvzpawners.id("plantz_spawner")), "Upgrade must accept either existing spawner in the center");
			var result = recipe.value().assemble(input);
			helper.assertTrue(result.is(ModBlocks.PLANTZ_SPAWNER_ITEM) && result.getCount() == 1, "Upgrade must output one Plantz Spawner");
			ingredients.set(0, new ItemStack(Items.GLASS_BOTTLE));
			helper.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()), "Every outer slot must contain a Bottle of Sun");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 2000)
	public void rightClickOpensMenuClosingKeepsPacketAndTamingUsesGenericCooldown(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		var player = helper.makeMockServerPlayerInLevel();
		level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		helper.assertTrue(player.containerMenu instanceof PlantzSpawnerMenu, "Right click must open the Plantz menu");
		block(helper, pos).packet().setItem(0, new ItemStack(PeaExtensionItems.WEED_PACKET));
		player.closeContainer();
		helper.assertTrue(block(helper, pos).packet().getItem(0).is(PeaExtensionItems.WEED_PACKET), "Closing the menu must retain the selection");
		helper.runAtTickTime(1840, () -> {
			Plant plant = (Plant) owned(helper, pos).getFirst();
			var slot = WeedSpawnerData.get(level).find(plant.getUUID()).slot();
			long now = level.getGameTime();
			plant.tame(player);
			helper.assertTrue(!plant.isAlive() && !plant.isTame(), "Generic-source plants must retain anti-taming behavior");
			helper.assertTrue(slot.readyAt() == now + 3600, "Taming must use the generic source cooldown");
			helper.succeed();
		});
	}

	private BlockPos place(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
		BlockPos relative = new BlockPos(4, 1, 4);
		helper.setBlock(relative, ModBlocks.PLANTZ_SPAWNER);
		return helper.absolutePos(relative);
	}
	private PlantzSpawnerBlockEntity block(GameTestHelper helper, BlockPos pos) {
		return (PlantzSpawnerBlockEntity) helper.getLevel().getBlockEntity(pos);
	}
	private List<Entity> owned(GameTestHelper helper, BlockPos pos) {
		return WeedSpawnerData.get(helper.getLevel()).ownership().entrySet().stream()
			.filter(entry -> entry.getValue().spawner().pos().equals(pos))
			.map(entry -> helper.getLevel().getEntity(entry.getKey())).filter(entity -> entity != null && entity.isAlive()).toList();
	}

	@GameTest
	public void filterAndMenuRejectImmobilePacketsAndOrdinaryItems(GameTestHelper helper) {
		var level = helper.getLevel();
		ItemStack walking = new ItemStack(PeaExtensionItems.WEED_PACKET);
		ItemStack stationary = SeedPacketItem.Companion.stackFor(PazEntities.PEA_SHOOTER);
		helper.assertTrue(SeedPacketFilter.resolve(level, walking) == PeaExtensionEntities.WEED, "Walking packet must be accepted");
		helper.assertTrue(SeedPacketFilter.resolve(level, stationary) == null, "Immobile plant packet must be rejected");
		ItemStack spoof = new ItemStack(Items.DIRT);
		spoof.set(DataComponents.ENTITY_DATA, walking.get(DataComponents.ENTITY_DATA));
		helper.assertTrue(SeedPacketFilter.resolve(level, spoof) == null, "An ordinary item with entity data is not a seed packet");
		BlockPos pos = place(helper);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var be = block(helper, pos);
		var menu = new PlantzSpawnerMenu(1, player.getInventory(), be.packet(), ContainerLevelAccess.create(level, pos));
		helper.assertTrue(menu.slots.size() == 37 && menu.slots.getFirst().mayPlace(walking), "Expected one input slot and 36 player inventory slots");
		helper.assertTrue(!menu.slots.getFirst().mayPlace(stationary), "Menu must reject immobile plants");
		be.packet().setItem(0, stationary);
		helper.assertTrue(be.packet().isEmpty(), "Server inventory must also reject invalid insertion");
		player.getInventory().setItem(9, new ItemStack(PeaExtensionItems.WEED_PACKET, 5));
		menu.quickMoveStack(player, 1);
		helper.assertTrue(be.packet().getItem(0).getCount() == 1 && player.getInventory().getItem(9).getCount() == 4,
			"Shift-click must insert only one packet without loss/duplication");
		menu.quickMoveStack(player, 0);
		helper.assertTrue(be.packet().isEmpty(), "Shift-click must remove the selection");
		helper.succeed();
	}

	@GameTest(maxTicks = 5700)
	public void emptyUntilPacketThenOnePlantAndThreeMinuteRespawn(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		helper.runAtTickTime(40, () -> {
			helper.assertTrue(owned(helper, pos).isEmpty(), "Empty spawner must not create an entity");
			block(helper, pos).packet().setItem(0, new ItemStack(PeaExtensionItems.WEED_PACKET));
		});
		helper.runAtTickTime(1820, () -> helper.assertTrue(owned(helper, pos).isEmpty(), "Initial packet selection must wait ninety seconds"));
		helper.runAtTickTime(1880, () -> {
			var plants = owned(helper, pos);
			helper.assertTrue(plants.size() == 1 && plants.getFirst().getType() == PeaExtensionEntities.WEED, "Packet should select exactly one plant");
			var be = block(helper, pos);
			var saved = be.saveWithFullMetadata(level.registryAccess());
			var loaded = (PlantzSpawnerBlockEntity) BlockEntity.loadStatic(pos, be.getBlockState(), saved, level.registryAccess());
			helper.assertTrue(loaded != null && loaded.packet().getItem(0).is(PeaExtensionItems.WEED_PACKET), "Slot contents must survive serialization");
			var slot = WeedSpawnerData.get(level).find(plants.getFirst().getUUID()).slot();
			long now = level.getGameTime();
			plants.getFirst().kill(level);
			helper.assertTrue(slot.readyAt() == now + 3600, "Plantz replacement delay must be three minutes");
		});
		helper.runAtTickTime(5479, () -> helper.assertTrue(owned(helper, pos).isEmpty(), "Respawn occurred before three minutes"));
		helper.runAtTickTime(5520, () -> {
			var plants = owned(helper, pos);
			helper.assertTrue(plants.size() == 1, "Selected plant must respawn after the cooldown");
			helper.assertTrue(block(helper, pos).packet().getItem(0).getCount() == 1, "Spawning must not consume the packet");
			block(helper, pos).packet().removeItem(0, 1);
			helper.assertTrue(!plants.getFirst().isAlive(), "Removing the packet must kill its plant immediately");
		});
		helper.runAtTickTime(5560, () -> {
			helper.assertTrue(owned(helper, pos).isEmpty() && block(helper, pos).packet().isEmpty(), "Removing packet must clear selection and keep source inactive");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 3800)
	public void changingSelectionKillsOldPlantAndBreakingDropsPacket(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		block(helper, pos).packet().setItem(0, new ItemStack(PeaExtensionItems.WEED_PACKET));
		helper.runAtTickTime(1840, () -> {
			var old = owned(helper, pos).getFirst();
			block(helper, pos).packet().setItem(0, new ItemStack(PeaExtensionItems.DANDELION_WEED_PACKET));
			helper.assertTrue(!old.isAlive(), "Replacing a packet must kill the previous selection");
		});
		helper.runAtTickTime(3620, () -> helper.assertTrue(owned(helper, pos).isEmpty(), "Replacing a packet must also wait the initial delay"));
		helper.runAtTickTime(3680, () -> {
			var plant = owned(helper, pos).getFirst();
			helper.assertTrue(plant.getType() == PeaExtensionEntities.DANDELION_WEED, "New packet must select its own entity type");
			level.destroyBlock(pos, true);
			helper.assertTrue(!plant.isAlive(), "Breaking the source must kill its selected entity");
			helper.assertItemEntityCountIs(PeaExtensionItems.DANDELION_WEED_PACKET, new BlockPos(4, 1, 4), 2, 1);
			helper.assertItemEntityCountIs(ModBlocks.PLANTZ_SPAWNER_ITEM, new BlockPos(4, 1, 4), 2, 1);
			helper.succeed();
		});
	}
}
