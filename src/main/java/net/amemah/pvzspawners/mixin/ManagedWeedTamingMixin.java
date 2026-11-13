package net.amemah.pvzspawners.mixin;

import net.amemah.pvzspawners.spawner.WeedSpawnerManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class ManagedWeedTamingMixin {
	@Inject(method = "tame", at = @At("HEAD"), cancellable = true)
	private void pvzpawners$despawnInsteadOfTaming(Player player, CallbackInfo callback) {
		if (WeedSpawnerManager.despawnForTaming((Entity) (Object) this)) {
			// Do not grant ownership or taming advancements for an entity that cannot be kept.
			callback.cancel();
		}
	}
}
