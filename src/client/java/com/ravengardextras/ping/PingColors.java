package com.ravengardextras.ping;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every player gets a ping color derived from their name, so all clients agree on
 * "Scrolls = blue" without exchanging any data. When two names would land on the
 * same color, the alphabetically-later name moves to the next free color - names
 * are processed in sorted order, so every client resolves the clash identically
 * and no two players on the same screen ever share a color (up to palette size).
 */
public final class PingColors {
	private static final int[] PALETTE = {
			0xFF5577FF, // blue
			0xFFFFFF55, // yellow
			0xFF55FF55, // green
			0xFFFF5555, // red
			0xFFAA55FF, // purple
			0xFFFFAA00, // orange
			0xFF55FFFF, // cyan
			0xFFFF55FF, // pink
	};

	private PingColors() {
	}

	/**
	 * Assigns a distinct color to each name (until the palette runs out; a 9th+
	 * player falls back to their base hash color). Keys are lowercased names.
	 */
	public static Map<String, Integer> assign(Collection<String> playerNames) {
		List<String> sorted = playerNames.stream()
				.map(name -> name.toLowerCase(Locale.ROOT))
				.distinct()
				.sorted()
				.toList();
		Map<String, Integer> colors = new HashMap<>();
		boolean[] used = new boolean[PALETTE.length];
		int assigned = 0;
		for (String name : sorted) {
			int slot = Math.floorMod(name.hashCode(), PALETTE.length);
			if (assigned < PALETTE.length) {
				while (used[slot]) {
					slot = (slot + 1) % PALETTE.length;
				}
			}
			used[slot] = true;
			assigned++;
			colors.put(name, PALETTE[slot]);
		}
		return colors;
	}
}
