package net.amemah.pvzspawners.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import zombieman.pea_extension.entity.Weed;

/** Pea Extension 1.2.8 has variant getters but no setter. Keep this dependency-specific hook isolated. */
@Mixin(value = Weed.class, remap = false)
public interface WeedVariantAccessor {
	@Accessor(value = "VARIANT", remap = false)
	static EntityDataAccessor<Byte> pvzpawners$variant() {
		throw new AssertionError("Mixin accessor was not applied");
	}

	@Accessor(value = "VASE", remap = false)
	static EntityDataAccessor<Integer> pvzpawners$shieldPool() {
		throw new AssertionError("Mixin accessor was not applied");
	}
}
