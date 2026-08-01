package com.ravengardextras.ping;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active pings, keyed by sender (one ping per player - a new ping replaces the old one).
 * Written from the network/tick thread, read from the render thread.
 */
public final class PingManager {
	public record Ping(String sender, BlockPos pos, long createdAtMillis) {
	}

	private static final Map<String, Ping> PINGS = new ConcurrentHashMap<>();

	private PingManager() {
	}

	public static void add(String sender, BlockPos pos) {
		PINGS.put(sender.toLowerCase(Locale.ROOT), new Ping(sender, pos.immutable(), System.currentTimeMillis()));
	}

	/** Live pings, dropping (and forgetting) any older than durationMillis. */
	public static List<Ping> active(long nowMillis, long durationMillis) {
		List<Ping> result = new ArrayList<>();
		for (Map.Entry<String, Ping> entry : PINGS.entrySet()) {
			if (nowMillis - entry.getValue().createdAtMillis() > durationMillis) {
				PINGS.remove(entry.getKey(), entry.getValue());
			} else {
				result.add(entry.getValue());
			}
		}
		return result;
	}

	public static void clear() {
		PINGS.clear();
	}
}
