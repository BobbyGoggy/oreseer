package com.oreseer;

/** Colour presets for the menu. Index 0 is a special "rainbow" that cycles over time. */
public final class Palette {
	public static final String[] NAMES = {
		"Rainbow", "Magma Orange", "Hot Pink", "Ice Cyan", "Toxic Green", "Gold", "Violet", "White", "Deep Blue", "Candy"
	};
	public static final int[] COLORS = {
		0,
		0xFFFF5A1F, 0xFFFF2D55, 0xFF55E6FF, 0xFF4DFFB0, 0xFFFFE14D, 0xFFB26BFF, 0xFFFFFFFF, 0xFF3D7BFF, 0xFFFF8AD8
	};

	private Palette() {
	}

	public static int clamp(int index) {
		return Math.floorMod(index, COLORS.length);
	}

	public static boolean isRainbow(int index) {
		return clamp(index) == 0;
	}

	/** Colour used for HUD dots and the like (rainbow shows as white). */
	public static int solid(int index) {
		int c = COLORS[clamp(index)];
		return c == 0 ? 0xFFFFFFFF : c;
	}
}
