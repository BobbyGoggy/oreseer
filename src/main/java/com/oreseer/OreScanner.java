package com.oreseer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Incremental ore scanner.
 *
 * Step 1: every tick a few chunk columns (round-robin, nearest first) are scanned into a cache.
 * Step 2: every few ticks the cache is merged into an immutable {@link Snapshot}: filtered by the
 *         user's settings, re-validated against the live world, sorted by distance, ancient-debris
 *         veins are grouped (26-neighbourhood flood fill) and the nearest debris is picked.
 * The renderer and HUD only ever read the snapshot.
 */
public final class OreScanner {
	public record Snapshot(List<OreHit> hits, OreHit nearestDebris, int debrisCount, int veinCount, Map<OreType, Integer> counts) {
		public static final Snapshot EMPTY = new Snapshot(List.of(), null, 0, 0, Map.of());
	}

	private static final Map<Long, List<OreHit>> columns = new HashMap<>();
	private static final List<Long> order = new ArrayList<>();
	private static int cursor;
	private static int lastPcx = Integer.MAX_VALUE;
	private static int lastPcz = Integer.MAX_VALUE;
	private static int lastCr = -1;
	private static int tickCount;
	private static ClientLevel lastLevel;
	private static volatile Snapshot snapshot = Snapshot.EMPTY;

	private OreScanner() {
	}

	public static Snapshot snapshot() {
		return snapshot;
	}

	/** Active whenever a world is loaded (singleplayer or multiplayer) and highlights are enabled. */
	public static boolean isActive(Minecraft mc) {
		return mc.level != null && mc.player != null && OreSeerConfig.get().enabled;
	}

	public static void clear() {
		columns.clear();
		order.clear();
		cursor = 0;
		lastPcx = Integer.MAX_VALUE;
		snapshot = Snapshot.EMPTY;
	}

	public static void tick(Minecraft mc) {
		OreSeerConfig cfg = OreSeerConfig.get();
		if (!isActive(mc)) {
			if (snapshot != Snapshot.EMPTY || !columns.isEmpty()) {
				clear();
			}
			return;
		}
		ClientLevel level = mc.level;
		if (level != lastLevel) {
			clear();
			lastLevel = level;
		}

		BlockPos pp = mc.player.blockPosition();
		int pcx = pp.getX() >> 4;
		int pcz = pp.getZ() >> 4;
		int cr = (cfg.scanRadius + 15) / 16;
		if (pcx != lastPcx || pcz != lastPcz || cr != lastCr) {
			rebuildOrder(pcx, pcz, cr);
		}

		int n = Math.min(Math.max(1, cfg.columnsPerTick), order.size());
		for (int i = 0; i < n; i++) {
			long key = order.get(cursor);
			cursor = (cursor + 1) % order.size();
			scanColumn(level, key, pp.getY(), cfg);
		}

		tickCount++;
		if (tickCount % 4 == 0) {
			publish(mc, level, cfg);
		}
	}

	private static long pack(int cx, int cz) {
		return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
	}

	private static void rebuildOrder(int pcx, int pcz, int cr) {
		lastPcx = pcx;
		lastPcz = pcz;
		lastCr = cr;
		List<long[]> tmp = new ArrayList<>();
		Set<Long> keep = new HashSet<>();
		for (int dx = -cr; dx <= cr; dx++) {
			for (int dz = -cr; dz <= cr; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > cr * cr + 1) {
					continue;
				}
				long k = pack(pcx + dx, pcz + dz);
				tmp.add(new long[] { k, d2 });
				keep.add(k);
			}
		}
		tmp.sort(Comparator.comparingLong(a -> a[1]));
		order.clear();
		for (long[] a : tmp) {
			order.add(a[0]);
		}
		columns.keySet().retainAll(keep);
		cursor = 0;
	}

	private static void scanColumn(ClientLevel level, long key, int py, OreSeerConfig cfg) {
		int cx = (int) (key >> 32);
		int cz = (int) key;
		if (!level.hasChunk(cx, cz)) {
			columns.remove(key);
			return;
		}
		int minY = Math.max(level.getMinY(), py - cfg.verticalRadius);
		int maxY = Math.min(level.getMaxY(), py + cfg.verticalRadius);
		List<OreHit> found = new ArrayList<>();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos nb = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					m.set((cx << 4) + x, y, (cz << 4) + z);
					OreType t = OreType.of(level.getBlockState(m));
					if (t == null) {
						continue;
					}
					if (cfg.hideExposed && exposed(level, m, nb)) {
						continue;
					}
					found.add(new OreHit(m.getX(), m.getY(), m.getZ(), t, 1));
				}
			}
		}
		columns.put(key, found);
	}

	private static boolean exposed(ClientLevel level, BlockPos p, BlockPos.MutableBlockPos n) {
		for (Direction d : Direction.values()) {
			n.setWithOffset(p, d);
			BlockState s = level.getBlockState(n);
			if (s.isAir() || !s.getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static void publish(Minecraft mc, ClientLevel level, OreSeerConfig cfg) {
		double px = mc.player.getX();
		double py = mc.player.getY();
		double pz = mc.player.getZ();
		double hr2 = (double) cfg.scanRadius * cfg.scanRadius;

		List<OreHit> debris = new ArrayList<>();
		List<OreHit> others = new ArrayList<>();
		Map<OreType, Integer> counts = new EnumMap<>(OreType.class);
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

		for (List<OreHit> col : columns.values()) {
			for (OreHit h : col) {
				if (!cfg.setting(h.type()).enabled) {
					continue;
				}
				if (cfg.debrisOnly && h.type() != OreType.ANCIENT_DEBRIS) {
					continue;
				}
				double dx = h.x() + 0.5 - px;
				double dz = h.z() + 0.5 - pz;
				double dy = h.y() + 0.5 - py;
				if (dx * dx + dz * dz > hr2 || Math.abs(dy) > cfg.verticalRadius) {
					continue;
				}
				m.set(h.x(), h.y(), h.z());
				if (!h.type().matches(level.getBlockState(m))) {
					continue; // mined since the last column scan
				}
				counts.merge(h.type(), 1, Integer::sum);
				if (h.type() == OreType.ANCIENT_DEBRIS) {
					debris.add(h);
				} else {
					others.add(h);
				}
			}
		}

		Comparator<OreHit> byDist = Comparator.comparingDouble(h -> {
			double dx = h.x() + 0.5 - px;
			double dy = h.y() + 0.5 - py;
			double dz = h.z() + 0.5 - pz;
			return dx * dx + dy * dy + dz * dz;
		});
		debris.sort(byDist);
		others.sort(byDist);

		int veins = groupVeins(debris);
		while (debris.size() > 300) {
			debris.remove(debris.size() - 1);
		}
		int room = Math.max(0, cfg.maxHighlights - debris.size());
		List<OreHit> all = new ArrayList<>(debris);
		all.addAll(others.subList(0, Math.min(room, others.size())));

		OreHit nearest = debris.isEmpty() ? null : debris.get(0);
		int debrisTotal = counts.getOrDefault(OreType.ANCIENT_DEBRIS, 0);
		snapshot = new Snapshot(List.copyOf(all), nearest, debrisTotal, veins, counts);
	}

	/** Flood-fills touching debris blocks (26-neighbourhood) and writes the vein size into each hit. */
	private static int groupVeins(List<OreHit> debris) {
		Map<Long, OreHit> byKey = new HashMap<>();
		for (OreHit d : debris) {
			byKey.put(d.key(), d);
		}
		Map<Long, Integer> sizes = new HashMap<>();
		Set<Long> seen = new HashSet<>();
		int veins = 0;
		for (OreHit start : debris) {
			if (!seen.add(start.key())) {
				continue;
			}
			veins++;
			List<OreHit> comp = new ArrayList<>();
			ArrayDeque<OreHit> queue = new ArrayDeque<>();
			queue.add(start);
			while (!queue.isEmpty()) {
				OreHit cur = queue.poll();
				comp.add(cur);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dy == 0 && dz == 0) {
								continue;
							}
							OreHit nb = byKey.get(BlockPos.asLong(cur.x() + dx, cur.y() + dy, cur.z() + dz));
							if (nb != null && seen.add(nb.key())) {
								queue.add(nb);
							}
						}
					}
				}
			}
			for (OreHit c : comp) {
				sizes.put(c.key(), comp.size());
			}
		}
		debris.replaceAll(h -> new OreHit(h.x(), h.y(), h.z(), h.type(), sizes.getOrDefault(h.key(), 1)));
		return veins;
	}
}
