package net.amemah.pvzspawners.client;

import net.amemah.pvzspawners.menu.PlantzSpawnerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Small, texture-free vanilla-style panel: one input, then the player's inventory. */
public final class PlantzSpawnerScreen extends AbstractContainerScreen<PlantzSpawnerMenu> {
	public PlantzSpawnerScreen(PlantzSpawnerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 147);
	}
	@Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
		for (var slot : menu.slots) {
			int x = leftPos + slot.x, y = topPos + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
			graphics.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
			graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
		}
		super.extractContents(graphics, mouseX, mouseY, partialTick);
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) { }
}
