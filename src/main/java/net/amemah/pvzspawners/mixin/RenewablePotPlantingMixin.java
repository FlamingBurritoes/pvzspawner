package net.amemah.pvzspawners.mixin;

import joshxviii.plantz.entity.plant.Plant;
import joshxviii.plantz.item.SeedPacketItem;
import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.pot.RenewablePotManager;
import net.amemah.pvzspawners.spawner.SeedPacketFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SeedPacketItem.Companion.class)
public abstract class RenewablePotPlantingMixin {
	@Inject(method = "tryPlant", at = @At("HEAD"), cancellable = true)
	private void pvzpawners$checkPacket(Level level, Player player, ItemStack stack, BlockPos clicked, Direction face,
		Direction horizontal, boolean ignorePlacement, boolean free, boolean consume, CallbackInfoReturnable<InteractionResult> callback) {
		BlockPos feet = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
		if (level.getBlockState(feet.below()).is(ModBlocks.RENEWABLE_PLANT_POT)
			&& SeedPacketFilter.resolveStationary(level, stack) == null) callback.setReturnValue(InteractionResult.FAIL);
	}
	@Redirect(method = "tryPlant", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean pvzpawners$stationaryOnly(ServerLevel level, Entity entity) {
		if (level.getBlockState(RenewablePotManager.support(entity)).is(ModBlocks.RENEWABLE_PLANT_POT)
			&& (!(entity instanceof Plant plant) || plant.canWalk())) return false;
		return level.addFreshEntity(entity);
	}
	@Redirect(method = "tryPlant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;tame(Lnet/minecraft/world/entity/player/Player;)V"))
	private void pvzpawners$adoptAfterInitialOwnership(TamableAnimal animal, Player player) {
		animal.tame(player);
		RenewablePotManager.planted(animal, player);
	}
}
