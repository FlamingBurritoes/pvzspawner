package net.amemah.pvzspawners.spawner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpawnerRangeTest {
	@Test
	void boundaryIsIncludedButSquareCornersAreNot() {
		assertTrue(SpawnerRange.contains(0.5, 0.5, 3.5, 0.5, 3));
		assertFalse(SpawnerRange.contains(0.5, 0.5, 3.5, 3.5, 3));
		assertFalse(SpawnerRange.contains(0.5, 0.5, 3.501, 0.5, 3));
	}

	@Test
	void rangeIsMeasuredFromTheCorrectSpawner() {
		assertTrue(SpawnerRange.contains(100.5, -99.5, 106.5, -99.5, 6));
		assertFalse(SpawnerRange.contains(0.5, 0.5, 106.5, -99.5, 6));
	}
}
