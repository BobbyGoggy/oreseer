package com.oreseer;

/** One detected ore block. {@code cluster} is the vein size (only computed for Ancient Debris). */
public record OreHit(int x, int y, int z, OreType type, int cluster) {
	public long key() {
		return net.minecraft.core.BlockPos.asLong(x, y, z);
	}
}
