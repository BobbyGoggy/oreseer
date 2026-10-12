package com.oreseer;

/**
 * The highlight effects. (rx, ry, rz) is the block's min corner relative to the camera,
 * {@code t} is wall-clock seconds, {@code alpha} already includes opacity and distance fade.
 */
public final class OreFx {
	private OreFx() {
	}

	private static float sq(float v) {
		return v * v;
	}

	/** Base colour (opaque) - palette colour or a time-cycling rainbow. */
	private static int base(OreSetting o, float t, float speed, float hueOffset) {
		if (Palette.isRainbow(o.colorIndex)) {
			return Colors.hsv(t * 0.15f * speed + hueOffset, 0.78f, 1f);
		}
		return Palette.solid(o.colorIndex);
	}

	public static void draw(Shapes s, OreHit h, OreSetting o, OreSeerConfig cfg, float t, float rx, float ry, float rz, float alpha) {
		if (alpha <= 0.01f) {
			return;
		}
		float sp = cfg.pulseSpeed;
		float cx = rx + 0.5f;
		float cy = ry + 0.5f;
		float cz = rz + 0.5f;
		switch (o.style) {
			case HEARTBEAT -> heartbeat(s, o, t, sp, cx, cy, cz, alpha);
			case CRYSTAL -> crystal(s, o, t, sp, cx, cy, cz, alpha);
			case AURORA -> aurora(s, o, t, sp, rx, ry, rz, alpha);
			case BEACON -> beacon(s, o, t, sp, cx, cy, cz, alpha);
			case RIPPLE -> ripple(s, o, t, sp, cx, cy, cz, alpha);
			case GLITCH -> glitch(s, h, o, t, sp, rx, ry, rz, alpha);
			case ORBIT -> orbit(s, o, t, sp, cx, cy, cz, alpha);
			case PRISM -> prism(s, o, t, sp, cx, cy, cz, alpha);
		}
	}

	/** Extra effects that only apply to Ancient Debris. */
	public static void debrisExtras(Shapes s, OreHit h, OreSetting o, OreSeerConfig cfg, float t, float rx, float ry, float rz, float alpha) {
		float cx = rx + 0.5f;
		float cy = ry + 0.5f;
		float cz = rz + 0.5f;
		if (cfg.debrisPillar && o.style != HighlightStyle.BEACON) {
			int c = base(o, t, cfg.pulseSpeed, 0.1f);
			s.pillar(cx, ry + 1f, cz, 18f, 0.07f, Colors.mulAlpha(c, alpha * 0.9f), Colors.mulAlpha(c, 0f));
		}
		if (cfg.debrisVeinFlash && h.cluster() >= 3) {
			// "motherlode" - strobing white frame, bigger for bigger veins
			float strobe = 0.5f + 0.5f * (float) Math.sin(t * 9f * cfg.pulseSpeed);
			float half = 0.62f + Math.min(h.cluster(), 8) * 0.03f;
			s.frame(cx, cy, cz, half, 0.04f, Colors.mulAlpha(0xFFFFFFFF, alpha * (0.35f + 0.65f * strobe)));
		}
	}

	private static void heartbeat(Shapes s, OreSetting o, float t, float sp, float cx, float cy, float cz, float a) {
		float p = (t * sp * 0.9f) % 1f;
		float beat = (float) (Math.exp(-sq((p - 0.05f) / 0.06f)) + 0.65 * Math.exp(-sq((p - 0.30f) / 0.08f)));
		float half = 0.5f * o.size * (0.50f + 0.40f * beat);
		int c = base(o, t, sp, 0f);
		s.cube(cx, cy, cz, half, Colors.mulAlpha(c, a * (0.30f + 0.55f * beat)));
		float halo = half + 0.12f + 0.25f * beat;
		s.frame(cx, cy, cz, halo, 0.03f, Colors.mulAlpha(0xFFFFFFFF, a * 0.65f * beat));
	}

	private static void crystal(Shapes s, OreSetting o, float t, float sp, float cx, float cy, float cz, float a) {
		float yaw = t * sp * 1.6f;
		float bob = (float) Math.sin(t * 2f * sp) * 0.06f;
		float r = 0.50f * o.size;
		int c = base(o, t, sp, 0f);
		s.octahedron(cx, cy + bob, cz, r, 1.25f, yaw, Colors.mulAlpha(c, a * 0.55f));
		s.octahedron(cx, cy + bob, cz, r * 0.5f, 1.25f, -yaw * 1.7f, Colors.mulAlpha(0xFFFFFFFF, a * 0.8f));
		s.frame(cx, cy, cz, 0.5f, 0.02f, Colors.mulAlpha(c, a * 0.35f));
	}

	private static void aurora(Shapes s, OreSetting o, float t, float sp, float rx, float ry, float rz, float a) {
		int layers = 6;
		float lh = 1.1f / layers;
		for (int i = 0; i < layers; i++) {
			float wave = 0.5f + 0.5f * (float) Math.sin(t * 2.2f * sp + i * 0.9f);
			int c = Palette.isRainbow(o.colorIndex)
				? Colors.hsv(t * 0.1f * sp + i * 0.07f, 0.7f, 1f)
				: Colors.hsv(hueOf(Palette.solid(o.colorIndex)) + (i - 2.5f) * 0.04f + 0.03f * (float) Math.sin(t * sp), 0.75f, 1f);
			float shift = (float) Math.sin(t * 1.3f * sp + i) * 0.05f;
			s.box(rx - 0.05f + shift, ry - 0.05f + i * lh, rz - 0.05f, rx + 1.05f + shift, ry - 0.05f + (i + 1) * lh - 0.01f, rz + 1.05f,
				Colors.mulAlpha(c, a * (0.20f + 0.45f * wave)));
		}
	}

	private static void beacon(Shapes s, OreSetting o, float t, float sp, float cx, float cy, float cz, float a) {
		int c = base(o, t, sp, 0f);
		float pulse = 0.75f + 0.25f * (float) Math.sin(t * 3f * sp);
		s.pillar(cx, cy - 5f, cz, 36f, 0.12f * o.size, Colors.mulAlpha(c, a * 0.85f * pulse), Colors.mulAlpha(c, 0f));
		s.pillar(cx, cy - 5f, cz, 20f, 0.05f, Colors.mulAlpha(0xFFFFFFFF, a * pulse), Colors.mulAlpha(0xFFFFFFFF, 0f));
		s.cube(cx, cy, cz, 0.28f * o.size, Colors.mulAlpha(0xFFFFFFFF, a * 0.9f));
		s.frame(cx, cy, cz, 0.52f, 0.03f, Colors.mulAlpha(c, a * 0.7f));
	}

	private static void ripple(Shapes s, OreSetting o, float t, float sp, float cx, float cy, float cz, float a) {
		for (int k = 0; k < 3; k++) {
			float ph = (t * sp * 0.5f + k / 3f) % 1f;
			float half = (0.35f + ph * 1.7f) * o.size;
			float fade = (float) Math.pow(1f - ph, 1.5);
			int c = base(o, t, sp, k * 0.1f);
			s.frame(cx, cy, cz, half, 0.03f + 0.03f * (1f - ph), Colors.mulAlpha(c, a * fade));
		}
		float core = 0.16f + 0.05f * (float) Math.sin(t * 5f * sp);
		s.cube(cx, cy, cz, core * o.size, Colors.mulAlpha(0xFFFFFFFF, a * 0.9f));
	}

	private static void glitch(Shapes s, OreHit h, OreSetting o, float t, float sp, float rx, float ry, float rz, float a) {
		int bucket = (int) (t * 10f * sp);
		int seed = h.x() * 73856093 ^ h.y() * 19349663 ^ h.z() * 83492791 ^ bucket * 2654435;
		int[] glitchColors = { 0xFF00FFFF, 0xFFFF00FF, 0xFFFFFF00 };
		int c = base(o, t, sp, 0f);
		if (Colors.rnd(seed, 0) < 0.55f) {
			s.frame(rx + 0.5f, ry + 0.5f, rz + 0.5f, 0.52f, 0.035f, Colors.mulAlpha(c, a * 0.8f));
			s.cube(rx + 0.5f, ry + 0.5f, rz + 0.5f, 0.46f, Colors.mulAlpha(c, a * 0.18f));
		}
		for (int i = 0; i < 3; i++) {
			float sy = ry + Colors.rnd(seed, 1 + i * 3) * 0.8f;
			float hgt = 0.06f + Colors.rnd(seed, 2 + i * 3) * 0.16f;
			float dx = (Colors.rnd(seed, 3 + i * 3) - 0.5f) * 0.8f;
			if (Colors.rnd(seed, 20 + i) < 0.7f) {
				s.box(rx + dx, sy, rz - 0.02f, rx + dx + 1f, sy + hgt, rz + 1.02f, Colors.mulAlpha(glitchColors[i], a * 0.6f));
			}
		}
	}

	private static void orbit(Shapes s, OreSetting o, float t, float sp, float cx, float cy, float cz, float a) {
		int c = base(o, t, sp, 0f);
		s.cube(cx, cy, cz, 0.13f * o.size, Colors.mulAlpha(0xFFFFFFFF, a * 0.9f));
		float rad = 0.9f * o.size;
		for (int i = 0; i < 3; i++) {
			for (int k = 0; k < 5; k++) {
				float ang = t * sp * (2.2f + 0.5f * i) + i * 2.09f - k * 0.2f;
				float cs = (float) Math.cos(ang) * rad;
				float sn = (float) Math.sin(ang) * rad;
				float px = cx;
				float py = cy;
				float pz = cz;
				switch (i) {
					case 0 -> { px += cs; pz += sn; }
					case 1 -> { px += cs; py += sn; }
					default -> { py += cs; pz += sn; }
				}
				float fade = 1f / (1f + k * 0.9f);
				s.cube(px, py, pz, (0.085f - k * 0.009f) * o.size, Colors.mulAlpha(Colors.hsv(hueOf(c) + i * 0.08f, 0.8f, 1f), a * fade));
			}
		}
	}

	private static void prism(Shapes s, OreSetting o, float t, float sp, float cx, float cy, float cz, float a) {
		float hue = t * 0.2f * sp;
		s.frame(cx - 0.52f, cy - 0.52f, cz - 0.52f, cx + 0.52f, cy + 0.52f, cz + 0.52f, 0.06f * o.size,
			i -> Colors.mulAlpha(Colors.hsv(hue + i / 12f, 0.85f, 1f), a * 0.9f));
		s.cube(cx, cy, cz, 0.45f, Colors.mulAlpha(Colors.hsv(hue + 0.5f, 0.6f, 1f), a * 0.12f));
	}

	private static float hueOf(int argb) {
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		if (d < 1e-4f) {
			return 0f;
		}
		float hue;
		if (max == r) {
			hue = ((g - b) / d) % 6f;
		} else if (max == g) {
			hue = (b - r) / d + 2f;
		} else {
			hue = (r - g) / d + 4f;
		}
		hue /= 6f;
		return hue < 0 ? hue + 1f : hue;
	}
}
