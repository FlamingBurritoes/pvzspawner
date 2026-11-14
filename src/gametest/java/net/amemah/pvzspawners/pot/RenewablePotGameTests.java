package net.amemah.pvzspawners.pot;

import joshxviii.plantz.PazBlocks;
import joshxviii.plantz.PazConfig;
import joshxviii.plantz.PazEntities;
import joshxviii.plantz.PazItems;
import joshxviii.plantz.PazTags;
import joshxviii.plantz.entity.plant.Plant;
import joshxviii.plantz.item.SeedPacketItem;
import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.block.WeedSpawnerBlock;
import net.amemah.pvzspawners.spawner.WeedSpawnerData;
import net.amemah.pvzspawners.menu.PlantzSpawnerBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import zombieman.pea_extension.PeaExtensionItems;

import java.util.ArrayList;
import java.util.UUID;

public class RenewablePotGameTests {
	private BlockPos place(GameTestHelper helper) {
		helper.setBlock(new BlockPos(4, 0, 4), Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(4, 1, 4), ModBlocks.RENEWABLE_PLANT_POT);
		return helper.absolutePos(new BlockPos(4, 1, 4));
	}
	private Plant plant(GameTestHelper helper, BlockPos pos, Player player, EntityType<?> type) {
		var result = SeedPacketItem.Companion.tryPlant(helper.getLevel(), player, SeedPacketItem.Companion.stackFor(type).copy(),
			pos, Direction.UP, Direction.SOUTH, false, true, false);
		helper.assertTrue(result.consumesAction(), "Normal packet planting should succeed");
		var ref = RenewablePotData.get(helper.getLevel()).at(pos);
		helper.assertTrue(ref != null && ref.entity() != null, "Successful seed planting must remember the plant");
		return (Plant) helper.getLevel().getEntity(ref.entity());
	}
	private Player player(GameTestHelper helper) { return helper.makeMockServerPlayerInLevel(); }

	@GameTest
	public void facingUsesPlayerAndRotationPreservesSpawnerTimers(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = player(helper);
		BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
		for (var block : java.util.List.of(ModBlocks.WEED_SPAWNER, ModBlocks.DANDELION_WEED_SPAWNER,
			ModBlocks.PLANTZ_SPAWNER, ModBlocks.RENEWABLE_PLANT_POT)) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				player.setYRot(direction.toYRot());
				var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
					new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
				var state = block.getStateForPlacement(context);
				Direction expected = block == ModBlocks.RENEWABLE_PLANT_POT ? direction.getOpposite() : direction;
				helper.assertTrue(state.getValue(WeedSpawnerBlock.FACING) == expected, "Spawners must face away from the player; the pot must retain its normal facing");
				helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90).getValue(WeedSpawnerBlock.FACING) == expected.getClockWise(), "Rotation must update facing");
				helper.assertTrue(state.mirror(Mirror.FRONT_BACK).mirror(Mirror.FRONT_BACK).equals(state), "Mirroring twice must restore facing");
				if (block != ModBlocks.RENEWABLE_PLANT_POT) {
					var cube = new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1);
					helper.assertTrue(state.getShape(level, pos).bounds().equals(cube), "Spawner selection must fill the full block");
					helper.assertTrue(state.getCollisionShape(level, pos).bounds().equals(cube), "Spawner collision must fill the full block");
				}
			}
			if (block == ModBlocks.RENEWABLE_PLANT_POT) continue;
			level.setBlockAndUpdate(pos, block.defaultBlockState());
			if (block == ModBlocks.PLANTZ_SPAWNER)
				((PlantzSpawnerBlockEntity) level.getBlockEntity(pos)).packet().setItem(0, new ItemStack(PeaExtensionItems.WEED_PACKET));
			var before = WeedSpawnerData.get(level).spawners().stream().filter(ref -> ref.pos().equals(pos)).findFirst().orElseThrow();
			level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(WeedSpawnerBlock.FACING, Direction.WEST));
			var after = WeedSpawnerData.get(level).spawners().stream().filter(ref -> ref.pos().equals(pos)).findFirst().orElseThrow();
			helper.assertTrue(before == after, "State-only rotation must not recreate a source or reset its timer");
			if (block == ModBlocks.PLANTZ_SPAWNER)
				helper.assertTrue(((PlantzSpawnerBlockEntity) level.getBlockEntity(pos)).packet().getItem(0).is(PeaExtensionItems.WEED_PACKET), "Rotation must keep the stored packet");
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 1350)
	public void emptyPotRejectsWalkersThenRenewsAfterOneMinute(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		var data = RenewablePotData.get(level);
		var player = player(helper);
		helper.assertTrue(data.at(pos) == null, "New pot must have no reference");
		helper.assertTrue(level.getBlockState(pos).is(PazTags.BlockTags.PLANT_POT), "Pot protection tag must match the dependency API");
		var walking = new ItemStack(PeaExtensionItems.WEED_PACKET);
		var result = SeedPacketItem.Companion.tryPlant(level, player, walking, pos, Direction.UP, Direction.SOUTH, false, true, true);
		helper.assertTrue(!result.consumesAction() && walking.getCount() == 1 && data.at(pos) == null, "Walking packets must be rejected without consumption");
		Plant first = plant(helper, pos, player, PazEntities.PEA_SHOOTER);
		helper.assertTrue(first.isAlive() && first.isTame(), "Initial automatic planter assignment must not kill the plant");
		helper.runAtTickTime(40, () -> {
			first.kill(level);
			helper.assertTrue(data.at(pos).entity() == null && data.at(pos).readyAt() == level.getGameTime() + 1200, "Death must start a full minute");
		});
		helper.runAtTickTime(1239, () -> helper.assertTrue(data.at(pos).entity() == null, "Must not renew before 60 seconds"));
		helper.runAtTickTime(1280, () -> {
			var next = (Plant) level.getEntity(data.at(pos).entity());
			helper.assertTrue(next != null && next.isAlive() && next.getType() == PazEntities.PEA_SHOOTER && !next.getUUID().equals(first.getUUID()), "Must renew the same stationary type with a fresh UUID");
			var config = PazConfig.INSTANCE.getServer();
			boolean coop = config.getCoopPlanting();
			try {
				config.setCoopPlanting(false);
				helper.assertTrue(next.verifyOwner(player), "Renewal must retain normal planter shovel permissions even without co-op");
			} finally { config.setCoopPlanting(coop); }
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 1350)
	public void shovellingClearsReferenceButUnauthorizedAttemptDoesNot(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Plant plant = plant(helper, pos, owner, PazEntities.PEA_SHOOTER);
		var stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
		var config = PazConfig.INSTANCE.getServer();
		boolean coop = config.getCoopPlanting();
		try {
			config.setCoopPlanting(false);
			plant.mobInteract(stranger, InteractionHand.MAIN_HAND);
			helper.assertTrue(plant.isAlive() && RenewablePotData.get(helper.getLevel()).at(pos) != null, "Unauthorized shovel must preserve plant and renewal");
			helper.assertTrue(stranger.getMainHandItem().getDamageValue() == 0, "Rejected shovelling must not damage tools");
		} finally { config.setCoopPlanting(coop); }
		ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
		owner.setItemInHand(InteractionHand.MAIN_HAND, shovel);
		plant.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(plant.isRemoved() && RenewablePotData.get(helper.getLevel()).at(pos) == null, "Successful shovelling must permanently forget the plant");
		helper.assertTrue(shovel.getDamageValue() == 4, "Normal shovel durability cost must remain");
		helper.assertItemEntityCountIs(SeedPacketItem.Companion.stackFor(PazEntities.PEA_SHOOTER).getItem(), new BlockPos(4, 1, 4), 3, 1);
		helper.runAtTickTime(1280, () -> {
			helper.assertTrue(RenewablePotData.get(helper.getLevel()).at(pos) == null, "Shovelled plant must never renew");
			helper.succeed();
		});
	}

	@GameTest
	public void cooperativeShovellingRetainsDependencyPermissions(GameTestHelper helper) {
		BlockPos pos = place(helper);
		Plant plant = plant(helper, pos, player(helper), PazEntities.PEA_SHOOTER);
		var stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
		var config = PazConfig.INSTANCE.getServer();
		boolean coop = config.getCoopPlanting();
		try {
			config.setCoopPlanting(true);
			plant.mobInteract(stranger, InteractionHand.MAIN_HAND);
			helper.assertTrue(plant.isRemoved() && RenewablePotData.get(helper.getLevel()).at(pos) == null, "Co-op mode must allow another player's successful shovel and clear renewal");
		} finally { config.setCoopPlanting(coop); }
		helper.succeed();
	}

	@GameTest(maxTicks = 1350)
	public void healingAndTamingAreAllowedAndBreakingPotCancelsRenewal(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		var owner = player(helper);
		Plant plant = plant(helper, pos, owner, PazEntities.PEA_SHOOTER);
		var reference = RenewablePotData.get(level).at(pos);
		plant.setHealth(plant.getMaxHealth() / 2);
		float injuredHealth = plant.getHealth();
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PazItems.SUN));
		plant.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(plant.isAlive() && plant.getHealth() > injuredHealth, "Normal Sun healing must be allowed");
		plant.tame(owner);
		helper.assertTrue(plant.isAlive() && plant.isTame(), "Later taming must not kill renewable-pot plants");
		helper.assertTrue(RenewablePotData.get(level).at(pos).equals(reference), "Healing/taming must retain the live reference without starting a cooldown");
		plant.kill(level);
		helper.assertTrue(RenewablePotData.get(level).at(pos).readyAt() == level.getGameTime() + 1200, "Only a real death must start renewal cooldown");
		level.destroyBlock(pos, true);
		helper.assertTrue(RenewablePotData.get(level).at(pos) == null, "Breaking during cooldown must cancel renewal");
		helper.assertItemEntityCountIs(ModBlocks.RENEWABLE_PLANT_POT_ITEM, new BlockPos(4, 1, 4), 2, 1);
		helper.runAtTickTime(40, () -> {
			level.setBlockAndUpdate(pos, ModBlocks.RENEWABLE_PLANT_POT.defaultBlockState());
			Plant replacement = plant(helper, pos, player(helper), PazEntities.PEA_SHOOTER);
			level.destroyBlock(pos, false);
			helper.assertTrue(!replacement.isAlive() && RenewablePotData.get(level).at(pos) == null, "Breaking a living plant's pot must kill it and forget its type");
		});
		helper.runAtTickTime(1280, () -> {
			helper.assertTrue(RenewablePotData.get(level).at(pos) == null, "Broken pots must not renew");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 2700)
	public void plantingDuringCooldownReplacesTypeAndWaitsForNewDeath(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		var player = player(helper);
		Plant old = plant(helper, pos, player, PazEntities.PEA_SHOOTER);
		old.kill(level);
		UUID[] replacement = new UUID[1];
		helper.runAtTickTime(40, () -> {
			Plant next = plant(helper, pos, player, PazEntities.SUNFLOWER);
			replacement[0] = next.getUUID();
		});
		helper.runAtTickTime(1280, () -> {
			var ref = RenewablePotData.get(level).at(pos);
			helper.assertTrue(ref.type() == PazEntities.SUNFLOWER && replacement[0].equals(ref.entity()), "New planting must cancel the old type/deadline and keep its living UUID");
			level.getEntity(replacement[0]).kill(level);
			helper.assertTrue(RenewablePotData.get(level).at(pos).readyAt() == level.getGameTime() + 1200, "New reference must count from its own death");
		});
		helper.runAtTickTime(2479, () -> helper.assertTrue(RenewablePotData.get(level).at(pos).entity() == null, "New type renewed too early"));
		helper.runAtTickTime(2520, () -> {
			var ref = RenewablePotData.get(level).at(pos);
			helper.assertTrue(ref.entity() != null && level.getEntity(ref.entity()).getType() == PazEntities.SUNFLOWER, "Only the new type may renew");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 1400)
	public void referencesDeadlinesAndUnloadedCleanupSurviveSaving(GameTestHelper helper) {
		var level = helper.getLevel();
		var data = new RenewablePotData();
		BlockPos pos = new BlockPos(20, 64, -20);
		UUID planter = UUID.randomUUID();
		UUID living = UUID.randomUUID();
		data.adopt(pos, PazEntities.PEA_SHOOTER, planter, living);
		var saved = RenewablePotData.CODEC.parse(NbtOps.INSTANCE, RenewablePotData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
		helper.assertTrue(saved.at(pos).entity().equals(living), "Unloaded entity UUID must survive and remain occupied");
		data.death(living, 500);
		saved = RenewablePotData.CODEC.parse(NbtOps.INSTANCE, RenewablePotData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
		helper.assertTrue(saved.at(pos).entity() == null && saved.at(pos).readyAt() == 1700 && saved.at(pos).planter().equals(planter), "Pending deadline and planter must persist");
		Plant unloaded = PazEntities.PEA_SHOOTER.create(level, EntitySpawnReason.SPAWNER);
		var actual = RenewablePotData.get(level);
		actual.adopt(pos, PazEntities.PEA_SHOOTER, planter, unloaded.getUUID());
		actual.remove(level, pos);
		saved = RenewablePotData.CODEC.parse(NbtOps.INSTANCE, RenewablePotData.CODEC.encodeStart(NbtOps.INSTANCE, actual).getOrThrow()).getOrThrow();
		helper.assertTrue(saved.consumePendingDeath(unloaded.getUUID()) && !saved.consumePendingDeath(unloaded.getUUID()), "Queued break cleanup must persist and consume once");
		unloaded.snapTo(helper.absolutePos(new BlockPos(4, 2, 4)), 0, 0);
		level.addFreshEntity(unloaded);
		helper.assertTrue(!unloaded.isAlive(), "Queued break must kill unloaded plant on load");
		BlockPos pot = place(helper);
		actual.adopt(pot, PazEntities.PEA_SHOOTER, planter, living);
		helper.runAtTickTime(1280, () -> {
			helper.assertTrue(actual.at(pot).entity().equals(living) && level.getEntity(living) == null, "An unloaded living UUID must not be inferred dead or duplicated");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 1400)
	public void obstructedRenewalWaitsAndRetriesWithoutLosingReference(GameTestHelper helper) {
		BlockPos pos = place(helper);
		var level = helper.getLevel();
		plant(helper, pos, player(helper), PazEntities.PEA_SHOOTER).kill(level);
		level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
		helper.runAtTickTime(1280, () -> {
			var ref = RenewablePotData.get(level).at(pos);
			helper.assertTrue(ref != null && ref.entity() == null && ref.type() == PazEntities.PEA_SHOOTER, "Obstruction must defer renewal without forgetting the reference");
			level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
		});
		helper.runAtTickTime(1320, () -> {
			var ref = RenewablePotData.get(level).at(pos);
			helper.assertTrue(ref.entity() != null && level.getEntity(ref.entity()).isAlive(), "Renewal must retry after obstruction is removed");
			helper.succeed();
		});
	}

	@GameTest
	public void recipeRequiresNormalPotAndEightSunBottles(GameTestHelper helper) {
		var ingredients = new ArrayList<ItemStack>();
		for (int i = 0; i < 9; i++) ingredients.add(new ItemStack(i == 4 ? PazBlocks.PLANT_POT.asItem() : PazItems.SUN_BOTTLE));
		var input = CraftingInput.of(3, 3, ingredients);
		var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
		helper.assertTrue(recipe.value().assemble(input).is(ModBlocks.RENEWABLE_PLANT_POT_ITEM), "Recipe must produce renewable pot");
		ingredients.set(0, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()), "All eight bottles must contain Sun");
		helper.succeed();
	}
}
