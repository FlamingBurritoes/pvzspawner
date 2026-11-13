package net.amemah.pvzspawners.spawner;

import java.util.UUID;

/** One owned entity, or one empty slot with a world-tick deadline. No chunk-presence inference. */
public final class SpawnSlot {
	private UUID entityId;
	private long readyAt;

	public SpawnSlot(UUID entityId, long readyAt) {
		this.entityId = entityId;
		this.readyAt = readyAt;
	}

	public UUID entityId() {
		return entityId;
	}

	public long readyAt() {
		return readyAt;
	}

	public boolean isReady(long gameTime) {
		return entityId == null && gameTime >= readyAt;
	}

	public void occupy(UUID uuid) {
		if (entityId != null) {
			throw new IllegalStateException("Cannot replace a living or unloaded weed");
		}
		entityId = java.util.Objects.requireNonNull(uuid);
	}

	public void onDeath(long gameTime, long delayTicks) {
		if (entityId != null) {
			entityId = null;
			readyAt = gameTime + delayTicks;
		}
	}
}
