package com.ravengardextras.ping;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active pings and marks. Each player has at most one temporary ping and up to
 * {@link #MAX_MARKS} permanent marks; adding a mark past the cap evicts that
 * player's oldest mark. Every client applies the same cap in the same chat
 * order, so party members stay in sync without extra messages.
 * Written from the network/tick thread, read from the render thread.
 */
public final class PingManager {
	public static final int MAX_MARKS = 5;

	/** label is the item name shown instead of the sender, or null for a plain mark/ping. */
	public record Ping(String sender, BlockPos pos, long createdAtMillis, boolean permanent, String label) {
	}

	private static final Map<String, Ping> PINGS = new ConcurrentHashMap<>();

	private PingManager() {
	}

	public static void addPing(String sender, BlockPos pos) {
		PINGS.put(pingKey(sender), new Ping(sender, pos.immutable(), System.currentTimeMillis(), false, null));
	}

	public static void addMark(String sender, BlockPos pos, String label) {
		String cleanLabel = label == null || label.isBlank() ? null : label;
		PINGS.put(markKey(sender, pos), new Ping(sender, pos.immutable(), System.currentTimeMillis(), true, cleanLabel));
		List<Ping> marks = marksOf(sender);
		while (marks.size() > MAX_MARKS) {
			Ping oldest = marks.stream().min(Comparator.comparingLong(Ping::createdAtMillis)).orElseThrow();
			PINGS.remove(markKey(sender, oldest.pos()));
			marks = marksOf(sender);
		}
	}

	/** Live pings: permanent marks always, temporary pings until tempDurationMillis old. */
	public static List<Ping> active(long nowMillis, long tempDurationMillis) {
		List<Ping> result = new ArrayList<>();
		for (Map.Entry<String, Ping> entry : PINGS.entrySet()) {
			Ping ping = entry.getValue();
			if (!ping.permanent() && nowMillis - ping.createdAtMillis() > tempDurationMillis) {
				PINGS.remove(entry.getKey(), ping);
			} else {
				result.add(ping);
			}
		}
		return result;
	}

	public static List<Ping> marksOf(String sender) {
		String prefix = markKeyPrefix(sender);
		List<Ping> result = new ArrayList<>();
		for (Map.Entry<String, Ping> entry : PINGS.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				result.add(entry.getValue());
			}
		}
		return result;
	}

	/** The sender's mark at exactly this position, or null. */
	public static Ping markAt(String sender, BlockPos pos) {
		return PINGS.get(markKey(sender, pos));
	}

	public static void removeMarkAt(String sender, BlockPos pos) {
		PINGS.remove(markKey(sender, pos));
	}

	public static void removeAllMarks(String sender) {
		PINGS.keySet().removeIf(key -> key.startsWith(markKeyPrefix(sender)));
	}

	/** Removes the sender's temporary ping only if it sits at exactly this position. */
	public static void removePingIfAt(String sender, BlockPos pos) {
		PINGS.computeIfPresent(pingKey(sender), (key, ping) -> ping.pos().equals(pos) ? null : ping);
	}

	public static void clear() {
		PINGS.clear();
	}

	private static String pingKey(String sender) {
		return sender.toLowerCase(Locale.ROOT) + "#ping";
	}

	private static String markKeyPrefix(String sender) {
		return sender.toLowerCase(Locale.ROOT) + "#mark#";
	}

	private static String markKey(String sender, BlockPos pos) {
		return markKeyPrefix(sender) + pos.asLong();
	}
}
