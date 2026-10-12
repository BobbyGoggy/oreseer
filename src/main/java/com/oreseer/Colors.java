package com.oreseer;

/** Small ARGB colour helpers. */
public final class Colors {
	private Colors() {
	}

	public static int clamp255(float v) {
		return Math.max(0, Math.min(255, Math.round(v)));
	}

	/** Multiplies the alpha channel of an ARGB colour. */
	public static int mulAlpha(int argb, float f) {
		int a = clamp255((argb >>> 24) * f);
		return (a << 24) | (argb & 0xFFFFFF);
	}

	public static int withAlpha(int rgb, float alpha) {
		return (clamp255(alpha * 255f) << 24) | (rgb & 0xFFFFFF);
	}

	/** Scales the RGB channels (fake face shading). */
	public static int shade(int argb, float f) {
		int a = argb >>> 24;
		int r = clamp255(((argb >> 16) & 0xFF) * f);
		int g = clamp255(((argb >> 8) & 0xFF) * f);
		int b = clamp255((argb & 0xFF) * f);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int hsv(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float hh = h * 6f;
		int i = (int) hh;
		float f = hh - i;
		float p = v * (1f - s);
		float q = v * (1f - s * f);
		float t = v * (1f - s * (1f - f));
		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | (clamp255(r * 255f) << 16) | (clamp255(g * 255f) << 8) | clamp255(b * 255f);
	}

	/** Cheap deterministic pseudo random in [0,1) from a seed and a channel number. */
	public static float rnd(int seed, int k) {
		int h = seed * 374761393 + k * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFF) / 65536f;
	}
}
