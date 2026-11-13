package net.amemah.pvzspawners.menu;

import net.amemah.pvzspawners.block.ModBlocks;
import net.amemah.pvzspawners.spawner.SeedPacketFilter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PlantzSpawnerMenu extends AbstractContainerMenu {
	private final ContainerLevelAccess access;
	public PlantzSpawnerMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(1), ContainerLevelAccess.NULL);
	}
	public PlantzSpawnerMenu(int id, Inventory inventory, Container container, ContainerLevelAccess access) {
		super(SpawnerMenus.PLANTZ, id);
		this.access = access;
		addSlot(new Slot(container, 0, 80, 25) {
			@Override public boolean mayPlace(ItemStack stack) { return SeedPacketFilter.resolve(inventory.player.level(), stack) != null; }
			@Override public int getMaxStackSize() { return 1; }
		});
		addStandardInventorySlots(inventory, 8, 65);
	}
	@Override public boolean stillValid(Player player) { return stillValid(access, player, ModBlocks.PLANTZ_SPAWNER); }
	@Override public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index == 0) {
			if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
		} else {
			if (!slots.getFirst().mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}
}
