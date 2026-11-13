package net.amemah.pvzspawners.menu;

import net.amemah.pvzspawners.spawner.SeedPacketFilter;
import net.amemah.pvzspawners.spawner.SpawnerKind;
import net.amemah.pvzspawners.spawner.WeedSpawnerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PlantzSpawnerBlockEntity extends BlockEntity implements MenuProvider {
	private boolean loading;
	private ItemStack previous = ItemStack.EMPTY;
	private final SimpleContainer packet = new SimpleContainer(1) {
		@Override public void setChanged() { selectionChanged(); }
		@Override public int getMaxStackSize() { return 1; }
		@Override public boolean canPlaceItem(int slot, ItemStack stack) {
			return level != null && SeedPacketFilter.resolve(level, stack) != null;
		}
		@Override public void setItem(int slot, ItemStack stack) {
			if (!loading && !stack.isEmpty() && !canPlaceItem(slot, stack)) return;
			super.setItem(slot, stack);
		}
	};

	public PlantzSpawnerBlockEntity(BlockPos pos, BlockState state) {
		super(SpawnerMenus.BLOCK_ENTITY, pos, state);
	}

	public SimpleContainer packet() { return packet; }

	private void selectionChanged() {
		setChanged();
		ItemStack selected = packet.getItem(0);
		if (!loading && level instanceof ServerLevel server && !ItemStack.matches(previous, selected)) {
			// Empty/replacement means kill the old selection, including unloaded owned plants.
			WeedSpawnerData data = WeedSpawnerData.get(server);
			data.unregister(server, worldPosition);
			data.register(worldPosition, SpawnerKind.PLANTZ, server.getGameTime());
		}
		previous = selected.copy();
	}

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		loading = true;
		try { packet.setItem(0, input.read("packet", ItemStack.CODEC).orElse(ItemStack.EMPTY)); }
		finally { loading = false; previous = packet.getItem(0).copy(); }
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!packet.getItem(0).isEmpty()) output.store("packet", ItemStack.CODEC, packet.getItem(0));
	}
	@Override public void preRemoveSideEffects(BlockPos pos, BlockState newState) {
		if (level instanceof ServerLevel server) {
			WeedSpawnerData.get(server).unregister(server, worldPosition);
			// Clear the slot before dropping so repeated callbacks cannot duplicate its packet.
			ItemStack drop = packet.removeItemNoUpdate(0);
			if (!drop.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
		}
		super.preRemoveSideEffects(pos, newState);
	}
	@Override public Component getDisplayName() { return Component.translatable("block.pvzpawners.plantz_spawner"); }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new PlantzSpawnerMenu(id, inventory, packet, ContainerLevelAccess.create(level, worldPosition));
	}
}
