package net.amemah.pvzspawners.spawner;

import net.amemah.pvzspawners.Pvzpawners;
import net.amemah.pvzspawners.block.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import zombieman.pea_extension.PeaExtensionItems;
import zombieman.pea_extension.entity.DandelionWeed;

import java.util.ArrayList;
import java.util.List;

public class DandelionSpawnerGameTests {
	@GameTest(maxTicks = 100)
	public void mixedSpawnerKindsKeepSeparateLimitsAndCooldowns(GameTestHelper helper) {
		BlockPos dandelionPos = place(helper);
		BlockPos normalRelative = new BlockPos(2, 1, 4);
		helper.setBlock(normalRelative, ModBlocks.WEED_SPAWNER);
		BlockPos normalPos = helper.absolutePos(normalRelative);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(60, () -> {
			var data = WeedSpawnerData.get(level);
			var normalIds = data.ownership().entrySet().stream()
				.filter(entry -> entry.getValue().spawner().pos().equals(normalPos)).map(java.util.Map.Entry::getKey).toList();
			var dandelions = owned(level, dandelionPos);
			helper.assertTrue(normalIds.size() == 3 && dandelions.size() == 2, "Mixed sources must retain their distinct limits");
			var normalSlot = data.find(normalIds.getFirst()).slot();
			var dandelionSlot = data.find(dandelions.getFirst().getUUID()).slot();
			long now = level.getGameTime();
			level.getEntity(normalIds.getFirst()).kill(level);
			dandelions.getFirst().kill(level);
			helper.assertTrue(normalSlot.readyAt() == now + 1200 && dandelionSlot.readyAt() == now + 400,
				"Mixed source cooldowns must not overwrite each other");
			level.setBlockAndUpdate(normalPos, Blocks.AIR.defaultBlockState());
			helper.assertTrue(dandelions.get(1).isAlive() && data.find(dandelions.get(1).getUUID()) != null,
				"Destroying regular source affected dandelion source");
			helper.succeed();
		});
	}

	private BlockPos place(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
			}
		}
		BlockPos relative = new BlockPos(4, 1, 4);
		helper.setBlock(relative, ModBlocks.DANDELION_WEED_SPAWNER);
		return helper.absolutePos(relative);
	}

	private List<DandelionWeed> owned(ServerLevel level, BlockPos pos) {
		return WeedSpawnerData.get(level).ownership().entrySet().stream()
			.filter(entry -> entry.getValue().spawner().pos().equals(pos))
			.map(entry -> level.getEntity(entry.getKey()))
			.filter(entity -> entity instanceof DandelionWeed && entity.isAlive())
			.map(entity -> (DandelionWeed) entity).toList();
	}

	@GameTest(maxTicks = 550)
	public void twoDandelionsRespawnAfterTwentySecondsAndShareProtection(GameTestHelper helper) {
		BlockPos pos = place(helper);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(40, () -> {
			List<DandelionWeed> weeds = owned(level, pos);
			helper.assertTrue(weeds.size() == 2, "Dandelion source must spawn exactly two Dandelion Weeds");
			DandelionWeed first = weeds.getFirst();
			first.move(MoverType.SELF, new Vec3(20, 0, 0));
			helper.assertTrue(SpawnerRange.contains(pos.getX() + 0.5, pos.getZ() + 0.5, first.getX(), first.getZ(), 6),
				"Dandelion escaped shared roam boundary");
			SpawnSlot slot = WeedSpawnerData.get(level).find(first.getUUID()).slot();
			long now = level.getGameTime();
			first.kill(level);
			helper.assertTrue(slot.readyAt() == now + 400, "Dandelion cooldown must be 400 ticks, not 1200");
			var data = WeedSpawnerData.get(level);
			var restored = WeedSpawnerData.CODEC.parse(NbtOps.INSTANCE,
				WeedSpawnerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
			helper.assertTrue(restored.find(weeds.get(1).getUUID()).spawner().kind() == SpawnerKind.DANDELION_WEED,
				"Dandelion source kind was lost during save/load");
			helper.runAtTickTime(439, () -> helper.assertTrue(owned(level, pos).size() == 1, "Dandelion respawned too soon"));
			helper.runAtTickTime(480, () -> {
				List<DandelionWeed> replacements = owned(level, pos);
				helper.assertTrue(replacements.size() == 2, "Dandelion did not respawn after twenty seconds");
				DandelionWeed tameAttempt = replacements.getFirst();
				tameAttempt.tame(helper.makeMockPlayer(GameType.SURVIVAL));
				helper.assertTrue(!tameAttempt.isAlive() && !tameAttempt.isTame(), "Managed dandelion must die instead of being tamed");
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				helper.assertTrue(replacements.stream().noneMatch(DandelionWeed::isAlive), "Breaking source must kill remaining dandelions");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void recipeUsesDandelionPacketsAndMiningMatchesDirt(GameTestHelper helper) {
		List<ItemStack> ingredients = new ArrayList<>();
		for (String row : List.of("WWW", "WDW", "DDD")) {
			for (char symbol : row.toCharArray()) {
				ingredients.add(new ItemStack(symbol == 'W' ? PeaExtensionItems.DANDELION_WEED_PACKET : Items.DIRT));
			}
		}
		CraftingInput input = CraftingInput.of(3, 3, ingredients);
		var recipe = helper.getLevel().getServer().getRecipeManager()
			.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
		helper.assertTrue(recipe.id().identifier().equals(Pvzpawners.id("dandelion_weed_spawner")), "Wrong crafting recipe");
		var result = recipe.value().assemble(input);
		helper.assertTrue(result.is(ModBlocks.DANDELION_WEED_SPAWNER_ITEM) && result.getCount() == 1, "Wrong recipe output");
		BlockPos pos = place(helper);
		var state = ModBlocks.DANDELION_WEED_SPAWNER.defaultBlockState();
		helper.assertTrue(state.getDestroySpeed(helper.getLevel(), pos) == Blocks.DIRT.defaultBlockState().getDestroySpeed(helper.getLevel(), pos), "Wrong hardness");
		helper.assertTrue(helper.makeMockPlayer(GameType.SURVIVAL).hasCorrectToolForDrops(state), "Must drop without a tool");
		helper.getLevel().destroyBlock(pos, true);
		helper.assertItemEntityCountIs(ModBlocks.DANDELION_WEED_SPAWNER_ITEM, new BlockPos(4, 1, 4), 2, 1);
		helper.succeed();
	}
}
