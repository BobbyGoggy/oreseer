package com.oreseer;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Text panel (counts, nearest debris with an arrow) and a rotating mini radar. */
public final class OreHud {
	private static final String[] ARROWS = { "↑", "↗", "→", "↘", "↓", "↙", "←", "↖" };

	private OreHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		OreSeerConfig cfg = OreSeerConfig.get();
		if (!OreScanner.isActive(mc) || mc.options.hideGui) {
			return;
		}
		OreScanner.Snapshot snap = OreScanner.snapshot();

		if (cfg.showHud) {
			drawPanel(g, mc, cfg, snap);
		}
		if (cfg.showRadar) {
			drawRadar(g, mc, cfg, snap);
		}
	}

	private static void drawPanel(GuiGraphicsExtractor g, Minecraft mc, OreSeerConfig cfg, OreScanner.Snapshot snap) {
		int x = 6;
		int y = 6;
		int line = 11;
		String title = "OreSeer" + (cfg.debrisOnly ? "  [debris only]" : "");
		g.text(mc.font, title, x, y, 0xFFFFD34D, true);
		y += line;

		if (snap.nearestDebris() != null && mc.player != null) {
			OreHit d = snap.nearestDebris();
			double dx = d.x() + 0.5 - mc.player.getX();
			double dy = d.y() + 0.5 - mc.player.getY();
			double dz = d.z() + 0.5 - mc.player.getZ();
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			String arrow = ARROWS[arrowIndex(mc, dx, dz)];
			String vert = dy > 1.5 ? " ^" : dy < -1.5 ? " v" : "";
			g.text(mc.font, "Ancient Debris: " + snap.debrisCount() + " in " + snap.veinCount() + " vein(s)", x, y, 0xFFFF6A1F, true);
			y += line;
			g.text(mc.font, "Nearest " + arrow + " " + String.format("%.1f", dist) + "m" + vert + "  Y=" + d.y()
				+ (d.cluster() > 1 ? "  vein x" + d.cluster() : ""), x, y, 0xFFFFFFFF, true);
			y += line;
		} else {
			g.text(mc.font, "Ancient Debris: none in range", x, y, 0xFF8A8A8A, true);
			y += line;
		}

		for (OreType t : OreType.ALL) {
			if (t == OreType.ANCIENT_DEBRIS) {
				continue;
			}
			int n = snap.counts().getOrDefault(t, 0);
			if (n > 0) {
				g.text(mc.font, t.label + ": " + n, x, y, Palette.solid(cfg.setting(t).colorIndex), true);
				y += line;
			}
		}
	}

	/** 0 = straight ahead, then clockwise in 45 degree steps. */
	private static int arrowIndex(Minecraft mc, double dx, double dz) {
		float rel = relativeAngle(mc, dx, dz);
		return Math.floorMod(Math.round(rel / 45f), 8);
	}

	/** Angle of the target relative to the player's view direction, degrees, positive = to the right. */
	private static float relativeAngle(Minecraft mc, double dx, double dz) {
		double target = Math.toDegrees(Math.atan2(-dx, dz));
		double rel = target - mc.player.getYRot();
		rel = ((rel % 360) + 540) % 360 - 180;
		return (float) rel;
	}

	private static void drawRadar(GuiGraphicsExtractor g, Minecraft mc, OreSeerConfig cfg, OreScanner.Snapshot snap) {
		int size = 90;
		int half = size / 2;
		int x0 = mc.getWindow().getGuiScaledWidth() - size - 8;
		int y0 = 8;
		int cx = x0 + half;
		int cy = y0 + half;

		g.fill(x0, y0, x0 + size, y0 + size, 0x90000000);
		g.fill(x0, y0, x0 + size, y0 + 1, 0xFF3A3A3A);
		g.fill(x0, y0 + size - 1, x0 + size, y0 + size, 0xFF3A3A3A);
		g.fill(x0, y0, x0 + 1, y0 + size, 0xFF3A3A3A);
		g.fill(x0 + size - 1, y0, x0 + size, y0 + size, 0xFF3A3A3A);
		g.fill(cx, y0 + 1, cx + 1, y0 + size - 1, 0x40FFFFFF);
		g.fill(x0 + 1, cy, x0 + size - 1, cy + 1, 0x40FFFFFF);

		// sweeping scan line, purely decorative
		float t = (Util.getMillis() % 3000L) / 3000f;
		double sweep = t * Math.PI * 2;
		for (int i = 4; i < half - 2; i += 2) {
			int sx = cx + (int) Math.round(Math.sin(sweep) * i);
			int sy = cy - (int) Math.round(Math.cos(sweep) * i);
			g.fill(sx, sy, sx + 1, sy + 1, 0x6055FF88);
		}

		float scale = (half - 4f) / Math.max(1, cfg.scanRadius);
		boolean blink = (Util.getMillis() / 250) % 2 == 0;
		for (OreHit h : snap.hits()) {
			double dx = h.x() + 0.5 - mc.player.getX();
			double dz = h.z() + 0.5 - mc.player.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			float rel = (float) Math.toRadians(relativeAngle(mc, dx, dz));
			int px = cx + Math.round((float) (Math.sin(rel) * dist * scale));
			int py = cy - Math.round((float) (Math.cos(rel) * dist * scale));
			if (h.type() == OreType.ANCIENT_DEBRIS) {
				int c = blink ? 0xFFFF6A1F : 0xFFFFD34D;
				g.fill(px - 2, py - 2, px + 3, py + 3, c);
			} else {
				g.fill(px - 1, py - 1, px + 1, py + 1, Palette.solid(cfg.setting(h.type()).colorIndex));
			}
		}
		g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
	}
}
