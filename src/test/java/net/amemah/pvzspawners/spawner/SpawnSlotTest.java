package net.amemah.pvzspawners.spawner;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpawnSlotTest {
	@Test
	void newSlotIsImmediatelyReady() {
		assertTrue(new SpawnSlot(null, 0).isReady(0));
	}

	@Test
	void livingOrUnloadedEntityNeverBecomesAnEmptySlot() {
		SpawnSlot slot = new SpawnSlot(UUID.randomUUID(), 0);
		assertFalse(slot.isReady(1_000_000));
		assertThrows(IllegalStateException.class, () -> slot.occupy(UUID.randomUUID()));
	}

	@Test
	void deathStartsExactlyOneMinuteOfCooldown() {
		SpawnSlot slot = new SpawnSlot(UUID.randomUUID(), 0);
		slot.onDeath(500, 1200);
		assertNull(slot.entityId());
		assertFalse(slot.isReady(1699));
		assertTrue(slot.isReady(1700));
		slot.occupy(UUID.randomUUID());
		assertFalse(slot.isReady(10000));
	}

	@Test
	void duplicateDeathNotificationDoesNotRestartTimer() {
		SpawnSlot slot = new SpawnSlot(UUID.randomUUID(), 0);
		slot.onDeath(500, 1200);
		slot.onDeath(510, 1200);
		assertEquals(1700, slot.readyAt());
	}

	@Test
	void restoredEmptySlotKeepsItsDeadline() {
		SpawnSlot slot = new SpawnSlot(null, 1700);
		assertFalse(slot.isReady(1699));
		assertTrue(slot.isReady(1700));
	}

	@Test
	void nearbySpawnersHaveIndependentSlots() {
		SpawnSlot first = new SpawnSlot(UUID.randomUUID(), 0);
		SpawnSlot second = new SpawnSlot(UUID.randomUUID(), 0);
		first.onDeath(0, 1200);
		assertFalse(second.isReady(1200));
		assertNotNull(second.entityId());
	}
}
