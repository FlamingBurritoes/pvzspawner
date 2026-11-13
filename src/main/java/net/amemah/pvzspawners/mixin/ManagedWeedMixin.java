package net.amemah.pvzspawners.mixin;

import net.amemah.pvzspawners.spawner.WeedSpawnerData;
import net.amemah.pvzspawners.spawner.WeedSpawnerManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ManagedWeedMixin {
	@ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
	private Vec3 pvzpawners$limitMovement(Vec3 movement) {
		return WeedSpawnerManager.limitMovement((Entity) (Object) this, movement);
	}

	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;",
		at = @At("HEAD"), cancellable = true)
	private void pvzpawners$preventDimensionEscape(TeleportTransition transition, CallbackInfoReturnable<Entity> callback) {
		Entity entity = (Entity) (Object) this;
		if (entity.entityTags().contains(WeedSpawnerData.MANAGED_TAG) && transition.newLevel() != entity.level()) {
			callback.setReturnValue(null);
		}
	}

	@Inject(method = "canUsePortal", at = @At("HEAD"), cancellable = true)
	private void pvzpawners$stayInDimension(boolean allowPassengers, CallbackInfoReturnable<Boolean> callback) {
		Entity entity = (Entity) (Object) this;
		if (entity.entityTags().contains(WeedSpawnerData.MANAGED_TAG)) {
			callback.setReturnValue(false);
		}
	}
}
