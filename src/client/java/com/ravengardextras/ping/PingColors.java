package com.ravengardextras.ping;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every player gets a ping color derived from their name, so clients agree on
 * "Scrolls = blue" without exchanging any data. When a new name would land on a
 * color already taken this session, it moves to the next free one. Assignments
 * are sticky until disconnect: a player's color never changes mid-session, no
 * matter who pings later, and no two players on the same screen share a color
 * (up to palette size). Party chat is delivered to everyone in the same order,
 * so clients observing the same pings resolve clashes identically.
 */
public final class PingColors {
	// Only ever three players in practice, so keep the palette small and maximally
	// distinct: bright cyan, neon green, and hot pink read clearly against grass,
	// stone, sky, or cave alike.
	private static final int[] PALETTE = {
			0xFF00FFFF, // cyan
			0xFF39FF14, // neon green
			0xFFFF3399, // hot pink
	};

	/** Lowercased name -> color, for the lifetime of the connection. */
	private static final Map<String, Integer> ASSIGNED = new ConcurrentHashMap<>();

	private PingColors() {
	}

	public static int colorFor(String playerName) {
		return ASSIGNED.computeIfAbsent(playerName.toLowerCase(Locale.ROOT), PingColors::pickColor);
	}

	/** The color the name has (or would get) without reserving it - for UI previews. */
	public static int preview(String playerName) {
		Integer assigned = ASSIGNED.get(playerName.toLowerCase(Locale.ROOT));
		return assigned != null ? assigned : PALETTE[baseSlot(playerName.toLowerCase(Locale.ROOT))];
	}

	public static void reset() {
		ASSIGNED.clear();
	}

	private static int pickColor(String lowercaseName) {
		int slot = baseSlot(lowercaseName);
		Set<Integer> used = new HashSet<>(ASSIGNED.values());
		if (used.size() >= PALETTE.length) {
			return PALETTE[slot]; // palette exhausted, collisions unavoidable
		}
		while (used.contains(PALETTE[slot])) {
			slot = (slot + 1) % PALETTE.length;
		}
		return PALETTE[slot];
	}

	private static int baseSlot(String lowercaseName) {
		return Math.floorMod(lowercaseName.hashCode(), PALETTE.length);
	}
}
