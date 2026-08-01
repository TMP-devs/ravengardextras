package com.ravengardextras.ping;

import java.util.Locale;

/**
 * Every player gets a stable ping color derived from their name, so all clients
 * agree on "Scrolls = blue" without exchanging any data.
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

	public static int colorFor(String playerName) {
		int hash = playerName.toLowerCase(Locale.ROOT).hashCode();
		return PALETTE[Math.floorMod(hash, PALETTE.length)];
	}
}
