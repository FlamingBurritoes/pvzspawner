package net.amemah.pvzspawners.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.amemah.pvzspawners.Pvzpawners;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WeedSpawnerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Settings current = new Settings(60, 3, 6, 3);
	private static Settings dandelion = new Settings(20, 3, 6, 2);

	private WeedSpawnerConfig() {
	}

	public record Settings(int respawnDelaySeconds, int spawnRadius, int roamRadius, int maxWeeds) {
		public Settings {
			if (respawnDelaySeconds < 1 || respawnDelaySeconds > 86400) {
				throw new IllegalArgumentException("respawnDelaySeconds must be between 1 and 86400");
			}
			if (spawnRadius < 1 || spawnRadius > 32 || roamRadius < spawnRadius || roamRadius > 64) {
				throw new IllegalArgumentException("spawnRadius must be 1..32; roamRadius must be spawnRadius..64");
			}
			if (maxWeeds < 1 || maxWeeds > 64) {
				throw new IllegalArgumentException("maxWeeds must be between 1 and 64");
			}
		}

		public long respawnDelayTicks() {
			return respawnDelaySeconds * 20L;
		}
	}

	public static Settings get() {
		return current;
	}

	public static void load() {
		current = loadFile("pvzpawners.json", new Settings(60, 3, 6, 3));
		dandelion = loadFile("pvzpawners-dandelion.json", new Settings(20, 3, 6, 2));
	}

	public static Settings getDandelion() {
		return dandelion;
	}

	private static Settings loadFile(String name, Settings defaults) {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(name);
		try {
			if (Files.notExists(file)) {
				Files.createDirectories(file.getParent());
				Files.writeString(file, GSON.toJson(defaults) + System.lineSeparator());
			} else {
				Settings parsed = GSON.fromJson(Files.readString(file), Settings.class);
				if (parsed == null) {
					throw new IllegalArgumentException("Configuration must be a JSON object");
				}
				return parsed;
			}
		} catch (IOException | RuntimeException exception) {
			Pvzpawners.LOGGER.error("Cannot read {}. Using defaults; the existing file was not overwritten.", file, exception);
		}
		return defaults;
	}
}
