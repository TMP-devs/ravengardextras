package com.ravengardextras.ping;

import java.util.regex.Pattern;

/**
 * Tracks whether party chat broadcasts are worth sending. A client-only mod can't
 * query party membership, but it can watch the chat: if the server answers one of
 * our broadcasts with a "you are not in a party" error, we stop broadcasting
 * (pings keep working locally); any sign of party activity turns broadcasts back
 * on. Wrong guesses self-heal - at worst one extra error line in chat.
 *
 * <p>Pure string/time logic, no Minecraft types.
 */
public final class PartyStatus {
	/** Only blame a "no party" error on our broadcast if it arrives within this window. */
	private static final long ERROR_WINDOW_MILLIS = 3000;

	private static final Pattern NOT_IN_PARTY = Pattern.compile("(?i)not (currently )?in a party");
	private static final Pattern PARTY_ACTIVITY = Pattern.compile(
			"Party >|(?i)joined the party|(?i)you have joined .{0,40}party");

	private static volatile boolean broadcastEnabled = true;
	private static volatile long lastBroadcastMillis;

	private PartyStatus() {
	}

	public static boolean shouldBroadcast() {
		return broadcastEnabled;
	}

	public static void noteBroadcast(long nowMillis) {
		lastBroadcastMillis = nowMillis;
	}

	/**
	 * Feeds an incoming chat line (formatting codes stripped). Returns true if this
	 * just switched broadcasts off, so the caller can tell the player.
	 */
	public static boolean observe(String plainChatLine, long nowMillis) {
		if (PARTY_ACTIVITY.matcher(plainChatLine).find()) {
			broadcastEnabled = true;
			return false;
		}
		if (broadcastEnabled
				&& nowMillis - lastBroadcastMillis < ERROR_WINDOW_MILLIS
				&& NOT_IN_PARTY.matcher(plainChatLine).find()) {
			broadcastEnabled = false;
			return true;
		}
		return false;
	}

	public static void reset() {
		broadcastEnabled = true;
		lastBroadcastMillis = 0;
	}
}
