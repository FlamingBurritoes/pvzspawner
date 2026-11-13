package net.amemah.pvzspawners.spawner;

public final class SpawnerRange {
	private SpawnerRange() {
	}

	/** Circular horizontal radius, not the square produced by an expanded AABB. */
	public static boolean contains(double centerX, double centerZ, double x, double z, double radius) {
		double dx = x - centerX;
		double dz = z - centerZ;
		return dx * dx + dz * dz <= radius * radius;
	}
}
