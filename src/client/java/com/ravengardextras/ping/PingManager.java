package com.ravengardextras.ping;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active pings and marks. Each player can have one temporary ping and one
 * permanent mark at a time - adding a new one of the same kind replaces the old.
 * Written from the network/tick thread, read from the render thread.
 */
public final class PingManager {
	public record Ping(String sender, BlockPos pos, long createdAtMillis, boolean permanent) {
	}

	private static final Map<String, Ping> PINGS = new ConcurrentHashMap<>();

	private PingManager() {
	}

	public static void add(String sender, BlockPos pos, boolean permanent) {
		PINGS.put(key(sender, permanent), new Ping(sender, pos.immutable(), System.currentTimeMillis(), permanent));
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

	/** The sender's permanent mark, or null. */
	public static Ping getMark(String sender) {
		return PINGS.get(key(sender, true));
	}

	public static void removeMark(String sender) {
		PINGS.remove(key(sender, true));
	}

	/** Removes the sender's ping of this kind only if it sits at exactly this position. */
	public static void removeIfAt(String sender, BlockPos pos, boolean permanent) {
		PINGS.computeIfPresent(key(sender, permanent),
				(key, ping) -> ping.pos().equals(pos) ? null : ping);
	}

	public static void clear() {
		PINGS.clear();
	}

	private static String key(String sender, boolean permanent) {
		return sender.toLowerCase(Locale.ROOT) + (permanent ? "#mark" : "#ping");
	}
}
