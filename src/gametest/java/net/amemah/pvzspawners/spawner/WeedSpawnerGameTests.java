package net.amemah.pvzspawners.spawner;

import net.amemah.pvzspawners.Pvzpawners;
import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.mixin.WeedVariantAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import zombieman.pea_extension.PeaExtensionEntities;
import zombieman.pea_extension.PeaExtensionItems;
import zombieman.pea_extension.entity.Weed;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WeedSpawnerGameTests {
	@GameTest(maxTicks = 850)
	public void breakingAndReplacingRestartsInitialDelayAndDeadlineSurvivesSave(GameTestHelper helper) {
		BlockPos pos = placeSpawner(helper);
		var level = helper.getLevel();
		var data = WeedSpawnerData.get(level);
		var first = data.spawners().stream().filter(source -> source.pos().equals(pos)).findFirst().orElseThrow();
		long originalDeadline = first.slots().getFirst().readyAt();
		helper.assertTrue(originalDeadline == level.getGameTime() + 600, "Initial deadline must be relative to placement world time");
		var restored = WeedSpawnerData.CODEC.parse(NbtOps.INSTANCE,
			WeedSpawnerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
		var savedSource = restored.spawners().stream().filter(source -> source.pos().equals(pos)).findFirst().orElseThrow();
		helper.assertTrue(savedSource.slots().getFirst().readyAt() == originalDeadline, "Saving must preserve the initial deadline");
		helper.runAtTickTime(100, () -> {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(pos, ModBlocks.WEED_SPAWNER.defaultBlockState());
			var replaced = data.spawners().stream().filter(source -> source.pos().equals(pos)).findFirst().orElseThrow();
			helper.assertTrue(replaced.slots().getFirst().readyAt() == level.getGameTime() + 600,
				"Replacement must wait a fresh initial delay, not reuse the old deadline");
		});
		helper.runAtTickTime(650, () -> helper.assertTrue(owned(level, pos).isEmpty(), "Breaking/replacing must not bypass the initial delay"));
		helper.runAtTickTime(740, () -> {
			helper.assertTrue(owned(level, pos).size() == 3, "Replaced source must eventually fill after its new initial delay");
			helper.succeed();
		});
	}

	@GameTest
	public void craftsWeedSpawnerFromRequestedPattern(GameTestHelper helper) {
		List<ItemStack> ingredients = new ArrayList<>();
		for (String row : List.of("WWW", "WDW", "DDD")) {
			for (char symbol : row.toCharArray()) {
				ingredients.add(new ItemStack(symbol == 'W' ? PeaExtensionItems.WEED_PACKET : Items.DIRT));
			}
		}
		CraftingInput input = CraftingInput.of(3, 3, ingredients);
		var recipe = helper.getLevel().getServer().getRecipeManager()
			.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
		helper.assertTrue(recipe.id().identifier().equals(Pvzpawners.id("weed_spawner")), "Requested pattern did not match Weed Spawner recipe");
		ItemStack output = recipe.value().assemble(input);
		helper.assertTrue(output.is(ModBlocks.WEED_SPAWNER_ITEM) && output.getCount() == 1, "Recipe must output one Weed Spawner");
		ingredients.set(4, new ItemStack(PeaExtensionItems.WEED_PACKET));
		helper.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()),
			"Center ingredient must be dirt, not a sixth seed packet");
		helper.succeed();
	}

	private BlockPos placeSpawner(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
			}
		}
		BlockPos relative = new BlockPos(4, 1, 4);
		helper.setBlock(relative, ModBlocks.WEED_SPAWNER);
		return helper.absolutePos(relative);
	}

	private List<Weed> owned(ServerLevel level, BlockPos pos) {
		return WeedSpawnerData.get(level).ownership().entrySet().stream()
			.filter(entry -> entry.getValue().spawner().pos().equals(pos))
			.map(entry -> level.getEntity(entry.getKey()))
			.filter(entity -> entity instanceof Weed && entity.isAlive())
			.map(entity -> (Weed) entity).toList();
	}

	@GameTest(maxTicks = 2100)
	public void fillsThreeSlotsAndRespawnsOnlyAfterOneMinute(GameTestHelper helper) {
		BlockPos pos = placeSpawner(helper);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(590, () -> helper.assertTrue(owned(level, pos).isEmpty(), "Initial spawn must wait thirty seconds"));
		helper.runAtTickTime(640, () -> {
			List<Weed> weeds = owned(level, pos);
			helper.assertTrue(weeds.size() == 3, "Spawner must own exactly three weeds");
			for (Weed weed : weeds) {
				helper.assertTrue(SpawnerRange.contains(pos.getX() + 0.5, pos.getZ() + 0.5,
					weed.getX(), weed.getZ(), 3), "Initial weed is outside spawn radius");
			}
			UUID killedId = weeds.getFirst().getUUID();
			long deathTime = level.getGameTime();
			weeds.getFirst().kill(level);
			helper.assertTrue(WeedSpawnerData.get(level).find(killedId) == null, "Death must free only the killed weed's slot");
			helper.runAtTickTime(1839, () -> {
				helper.assertTrue(level.getGameTime() - deathTime < 1200, "Pre-deadline check ran too late");
				helper.assertTrue(owned(level, pos).size() == 2, "Weed respawned before its 60-second cooldown");
			});
			helper.runAtTickTime(1880, () -> {
				helper.assertTrue(owned(level, pos).size() == 3, "Dead weed was not replaced after its cooldown");
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 750)
	public void overlappingSpawnersKeepSeparateOwnership(GameTestHelper helper) {
		BlockPos first = placeSpawner(helper);
		BlockPos secondRelative = new BlockPos(2, 1, 4);
		helper.setBlock(secondRelative, ModBlocks.WEED_SPAWNER);
		BlockPos second = helper.absolutePos(secondRelative);
		helper.runAtTickTime(660, () -> {
			List<Weed> firstWeeds = owned(helper.getLevel(), first);
			List<Weed> secondWeeds = owned(helper.getLevel(), second);
			helper.assertTrue(firstWeeds.size() == 3 && secondWeeds.size() == 3,
				"Overlapping spawners adopted each other's weeds or exceeded their own cap");
			helper.assertTrue(firstWeeds.stream().noneMatch(secondWeeds::contains), "One weed belongs to two spawners");
			helper.getLevel().setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
			helper.assertTrue(firstWeeds.stream().noneMatch(Weed::isAlive), "Broken source did not kill all its weeds");
			helper.assertTrue(secondWeeds.stream().allMatch(Weed::isAlive) && owned(helper.getLevel(), second).size() == 3,
				"Breaking one source affected the other source's weeds");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 750)
	public void confinesMovementAndDespawnsOnBlockRemoval(GameTestHelper helper) {
		BlockPos pos = placeSpawner(helper);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(640, () -> {
			List<Weed> weeds = owned(level, pos);
			helper.assertTrue(weeds.size() == 3, "Expected three initial weeds");
			Weed weed = weeds.getFirst();
			weed.move(MoverType.SELF, new Vec3(20, 0, 0));
			helper.assertTrue(SpawnerRange.contains(pos.getX() + 0.5, pos.getZ() + 0.5,
				weed.getX(), weed.getZ(), 6), "Movement escaped the six-block boundary");
			weed.teleportTo(pos.getX() + 12.5, pos.getY(), pos.getZ() + 0.5);
			helper.runAtTickTime(645, () -> {
				helper.assertTrue(SpawnerRange.contains(pos.getX() + 0.5, pos.getZ() + 0.5,
					weed.getX(), weed.getZ(), 6), "Out-of-range teleport was not recalled");
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				helper.assertTrue(WeedSpawnerData.get(level).find(weed.getUUID()) == null, "Broken spawner retained ownership");
				helper.assertTrue(weeds.stream().noneMatch(Weed::isAlive), "Source destruction must kill all linked weeds");
				helper.assertTrue(weeds.stream().noneMatch(Weed::isRemoved), "Dying weeds must remain briefly for their death animation");
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 750)
	public void tamingManagedWeedDespawnsImmediatelyButNaturalWeedsRemainTamable(GameTestHelper helper) {
		BlockPos pos = placeSpawner(helper);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(640, () -> {
			List<Weed> weeds = owned(level, pos);
			helper.assertTrue(weeds.size() == 3, "Expected three initial weeds");
			Weed weed = weeds.getFirst();
			SpawnSlot slot = WeedSpawnerData.get(level).find(weed.getUUID()).slot();
			long now = level.getGameTime();
			var player = helper.makeMockPlayer(GameType.SURVIVAL);
			weed.tame(player);
			helper.assertTrue(!weed.isAlive() && !weed.isRemoved(), "Managed weed must die during taming and remain for its death animation");
			helper.assertTrue(!weed.isTame(), "Managed weed must not be granted tamed ownership");
			helper.assertTrue(owned(level, pos).size() == 2, "Only the attempted tame should be removed");
			helper.assertTrue(slot.readyAt() == now + 1200 && !slot.isReady(now + 1199), "Taming must start the normal one-minute replacement cooldown");
			Weed natural = PeaExtensionEntities.WEED.create(level, EntitySpawnReason.COMMAND);
			helper.assertTrue(natural != null, "Could not create an untracked weed");
			natural.tame(player);
			helper.assertTrue(natural.isTame() && !natural.isRemoved(), "Natural/untracked weeds must remain tamable");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 750)
	public void directTameFlagCannotBypassTheRestriction(GameTestHelper helper) {
		BlockPos pos = placeSpawner(helper);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(640, () -> {
			Weed weed = owned(level, pos).getFirst();
			weed.setTame(true, true);
			helper.runAtTickTime(645, () -> {
				helper.assertTrue(!weed.isAlive(), "Directly setting the tame flag must not bypass the restriction");
				helper.assertTrue(owned(level, pos).size() == 2, "The other weeds must remain and the tamed slot must wait for cooldown");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void unloadedCleanupSurvivesSaveAndRunsWhenWeedLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Weed weed = PeaExtensionEntities.WEED.create(level, EntitySpawnReason.SPAWNER);
		helper.assertTrue(weed != null, "Could not create a weed for queued cleanup");
		weed.tame(helper.makeMockPlayer(GameType.SURVIVAL));
		weed.snapTo(helper.absolutePos(new BlockPos(4, 1, 4)), 0, 0);
		weed.entityTags().add(WeedSpawnerData.MANAGED_TAG);
		WeedSpawnerData data = WeedSpawnerData.get(level);
		BlockPos source = helper.absolutePos(new BlockPos(4, 1, 4));
		data.register(source);
		var spawner = data.spawners().stream().filter(entry -> entry.pos().equals(source)).findFirst().orElseThrow();
		data.occupy(spawner, spawner.slots().getFirst(), weed.getUUID());
		// This UUID is not in the world's loaded entities, just like a weed in an unloaded chunk.
		data.unregister(level, source);
		var encoded = WeedSpawnerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
		WeedSpawnerData restored = WeedSpawnerData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(restored.consumePendingDeath(weed.getUUID()), "Unloaded death did not survive save/load");
		helper.assertTrue(!restored.consumePendingDeath(weed.getUUID()), "Queued death must be consumed only once");
		level.addFreshEntity(weed);
		helper.assertTrue(!weed.isAlive() && !weed.isRemoved(), "Queued weed must die on load, with time for its death animation");
		helper.succeed();
	}

	@GameTest
	public void minesAndDropsLikeDirt(GameTestHelper helper) {
		BlockPos relative = new BlockPos(4, 1, 4);
		helper.setBlock(relative, ModBlocks.WEED_SPAWNER);
		BlockPos pos = helper.absolutePos(relative);
		var state = ModBlocks.WEED_SPAWNER.defaultBlockState();
		var dirt = Blocks.DIRT.defaultBlockState();
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(state.getDestroySpeed(helper.getLevel(), pos) == dirt.getDestroySpeed(helper.getLevel(), pos),
			"Weed Spawner hardness must match dirt");
		helper.assertTrue(player.hasCorrectToolForDrops(state), "Empty hand must be sufficient to collect the block");
		ItemStack shovel = new ItemStack(Items.WOODEN_SHOVEL);
		helper.assertTrue(shovel.getDestroySpeed(state) == shovel.getDestroySpeed(dirt), "Shovel mining speed must match dirt");
		helper.getLevel().destroyBlock(pos, true, player);
		helper.assertItemEntityCountIs(ModBlocks.WEED_SPAWNER_ITEM, relative, 2, 1);
		helper.succeed();
	}

	@GameTest
	public void savedOwnershipAndDeadlinesRoundTrip(GameTestHelper helper) {
		WeedSpawnerData original = new WeedSpawnerData();
		BlockPos pos = new BlockPos(20, 64, -20);
		original.register(pos);
		var spawner = original.spawners().getFirst();
		UUID living = UUID.randomUUID();
		UUID dead = UUID.randomUUID();
		original.occupy(spawner, spawner.slots().get(0), living);
		original.occupy(spawner, spawner.slots().get(1), dead);
		original.recordDeath(dead, 500);
		var encoded = WeedSpawnerData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		// Simulate a pre-dandelion save: these entries did not include a source kind.
		encoded.asCompound().orElseThrow().getListOrEmpty("spawners").getCompoundOrEmpty(0).remove("kind");
		WeedSpawnerData restored = WeedSpawnerData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(restored.find(living) != null, "Unloaded living entity lost its slot during save/load");
		helper.assertTrue(restored.find(living).spawner().kind() == SpawnerKind.WEED, "Old saves must remain regular Weed sources");
		helper.assertTrue(restored.find(dead) == null, "Dead entity retained ownership during save/load");
		var restoredSlots = restored.spawners().getFirst().slots();
		helper.assertTrue(!restoredSlots.get(0).isReady(100000), "Unloaded entity was treated as dead");
		helper.assertTrue(!restoredSlots.get(1).isReady(1699) && restoredSlots.get(1).isReady(1700),
			"Saved cooldown deadline changed");
		helper.succeed();
	}

	@GameTest
	public void allNineVariantsHaveWorkingDataAccessors(GameTestHelper helper) {
		Weed weed = PeaExtensionEntities.WEED.create(helper.getLevel(), EntitySpawnReason.SPAWNER);
		helper.assertTrue(weed != null, "Could not create extension Weed");
		helper.assertTrue(Weed.Variant.values().length == 9, "Dependency's variant set changed; review integration");
		for (Weed.Variant variant : Weed.Variant.values()) {
			weed.getEntityData().set(WeedVariantAccessor.pvzpawners$variant(), (byte) variant.getId());
			weed.getEntityData().set(WeedVariantAccessor.pvzpawners$shieldPool(), variant.getShieldPool());
			helper.assertTrue(weed.getVariant() == variant, "Variant assignment failed: " + variant);
		}
		helper.succeed();
	}
}
