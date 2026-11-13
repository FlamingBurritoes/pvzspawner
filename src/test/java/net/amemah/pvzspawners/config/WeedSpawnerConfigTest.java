package net.amemah.pvzspawners.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeedSpawnerConfigTest {
	@Test
	void initialDelayIsHalfRoundedDownToWholeSeconds() {
		assertEquals(600, new WeedSpawnerConfig.Settings(60, 3, 6, 3).initialDelayTicks());
		assertEquals(200, new WeedSpawnerConfig.Settings(20, 3, 6, 2).initialDelayTicks());
		assertEquals(1800, new WeedSpawnerConfig.Settings(180, 3, 6, 1).initialDelayTicks());
		assertEquals(600, new WeedSpawnerConfig.Settings(61, 3, 6, 3).initialDelayTicks());
		assertEquals(0, new WeedSpawnerConfig.Settings(1, 3, 6, 3).initialDelayTicks());
	}

	@Test
	void defaultsUseOneMinute() {
		assertEquals(1200, new WeedSpawnerConfig.Settings(60, 3, 6, 3).respawnDelayTicks());
	}

	@Test
	void invalidValuesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> new WeedSpawnerConfig.Settings(0, 3, 6, 3));
		assertThrows(IllegalArgumentException.class, () -> new WeedSpawnerConfig.Settings(60, 7, 6, 3));
		assertThrows(IllegalArgumentException.class, () -> new WeedSpawnerConfig.Settings(60, 3, 65, 3));
		assertThrows(IllegalArgumentException.class, () -> new WeedSpawnerConfig.Settings(60, 3, 6, 0));
	}

	@Test
	void jsonCannotBypassRecordValidation() {
		assertThrows(RuntimeException.class, () -> new Gson().fromJson(
			"{\"respawnDelaySeconds\":0,\"spawnRadius\":3,\"roamRadius\":6,\"maxWeeds\":3}",
			WeedSpawnerConfig.Settings.class));
	}
}
