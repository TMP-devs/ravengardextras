package com.ravengardextras.ping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The chat wire format for pings: {@code RGE-PING @ x, y, z}. Sent through the server's party
 * chat and parsed back out of whatever the server wraps it in, e.g.
 * {@code Party > [MVP+] Scrolls: RGE-PING @ 123, 64, -456}. Deliberately human-readable so
 * party members without the mod still get usable coordinates.
 *
 * <p>Pure string logic, no Minecraft types.
 */
public final class PingMessage {
	private static final Pattern COORDS = Pattern.compile("RGE-PING @ (-?\\d{1,7}), (-?\\d{1,7}), (-?\\d{1,7})");
	/** A player-name token directly followed by a chat separator ("Name:", "Name >", "Name »"). */
	private static final Pattern SENDER = Pattern.compile("([A-Za-z0-9_]{1,16})\\s*[:>»]");

	private PingMessage() {
	}

	public record Parsed(String sender, int x, int y, int z) {
	}

	public static String format(int x, int y, int z) {
		return "RGE-PING @ " + x + ", " + y + ", " + z;
	}

	/**
	 * Extracts a ping from the plain text of a chat line, or returns null if the line
	 * isn't a ping or has no recognizable sender. The sender is the last "Name:"-like
	 * token before the marker, which skips channel prefixes and rank tags.
	 */
	public static Parsed parse(String plainChatLine) {
		if (plainChatLine == null) {
			return null;
		}
		Matcher coords = COORDS.matcher(plainChatLine);
		if (!coords.find()) {
			return null;
		}
		Matcher senderMatcher = SENDER.matcher(plainChatLine.substring(0, coords.start()));
		String sender = null;
		while (senderMatcher.find()) {
			sender = senderMatcher.group(1);
		}
		if (sender == null) {
			return null;
		}
		try {
			return new Parsed(sender,
					Integer.parseInt(coords.group(1)),
					Integer.parseInt(coords.group(2)),
					Integer.parseInt(coords.group(3)));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
