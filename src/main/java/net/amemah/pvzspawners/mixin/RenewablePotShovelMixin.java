package net.amemah.pvzspawners.mixin;

import joshxviii.plantz.entity.plant.Plant;
import net.amemah.pvzspawners.pot.RenewablePotManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Plant.class)
public abstract class RenewablePotShovelMixin {
	// This point is reached only after the normal permission checks and successful packet removal.
	// Clear before discard's unload callback can mistake shovelling for a renewable death.
	@Inject(method = "dropAsSeedPacketItem", at = @At(value = "INVOKE", target = "Ljoshxviii/plantz/entity/plant/Plant;discard()V"))
	private void pvzpawners$forgetRemovedPlant(boolean drop, CallbackInfoReturnable<Boolean> callback) {
		RenewablePotManager.clearForShovel((Entity) (Object) this);
	}
}
