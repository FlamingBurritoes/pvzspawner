package net.amemah.pvzspawners.client;

import net.amemah.pvzspawners.menu.SpawnerMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PvzpawnersClient implements ClientModInitializer {
	@Override public void onInitializeClient() { MenuScreens.register(SpawnerMenus.PLANTZ, PlantzSpawnerScreen::new); }
}
