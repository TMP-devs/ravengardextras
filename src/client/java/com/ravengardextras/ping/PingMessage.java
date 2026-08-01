package com.ravengardextras.ping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The chat wire format for pings, sent through the server's party chat and parsed
 * back out of whatever the server wraps it in, e.g.
 * {@code Party > [MVP+] Scrolls: RGE-PING @ 123, 64, -456}. Deliberately
 * human-readable so party members without the mod still get usable coordinates.
 *
 * <ul>
 *   <li>{@code RGE-PING @ x, y, z} - temporary ping, disappears after a few seconds</li>
 *   <li>{@code RGE-MARK @ x, y, z} - permanent mark, stays until moved or cleared</li>
 *   <li>{@code RGE-MARK CLEAR} - removes the sender's permanent mark</li>
 * </ul>
 *
 * <p>Pure string logic, no Minecraft types.
 */
public final class PingMessage {
	private static final Pattern COORDS = Pattern.compile("RGE-(PING|MARK) @ (-?\\d{1,8}), (-?\\d{1,8}), (-?\\d{1,8})");
	private static final Pattern CLEAR = Pattern.compile("RGE-MARK CLEAR");
	/** A player-name token directly followed by a chat separator ("Name:", "Name >", "Name »"). */
	private static final Pattern SENDER = Pattern.compile("([A-Za-z0-9_]{1,16})\\s*[:>»]");

	private PingMessage() {
	}

	public enum Type {
		PING, MARK, CLEAR_MARK
	}

	/** Coordinates are meaningless (zero) when type is CLEAR_MARK. */
	public record Parsed(String sender, Type type, int x, int y, int z) {
	}

	public static String formatPing(int x, int y, int z) {
		return "RGE-PING @ " + x + ", " + y + ", " + z;
	}

	public static String formatMark(int x, int y, int z) {
		return "RGE-MARK @ " + x + ", " + y + ", " + z;
	}

	public static String formatClearMark() {
		return "RGE-MARK CLEAR";
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
		// Servers embed legacy formatting codes in the text ("Scrolls§f: ..."); strip
		// them or the code letter next to the separator gets mistaken for the sender.
		plainChatLine = plainChatLine.replaceAll("§.", "");

		Matcher coords = COORDS.matcher(plainChatLine);
		if (coords.find()) {
			String sender = senderBefore(plainChatLine, coords.start());
			if (sender == null) {
				return null;
			}
			try {
				Type type = coords.group(1).equals("MARK") ? Type.MARK : Type.PING;
				return new Parsed(sender, type,
						Integer.parseInt(coords.group(2)),
						Integer.parseInt(coords.group(3)),
						Integer.parseInt(coords.group(4)));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		Matcher clear = CLEAR.matcher(plainChatLine);
		if (clear.find()) {
			String sender = senderBefore(plainChatLine, clear.start());
			if (sender == null) {
				return null;
			}
			return new Parsed(sender, Type.CLEAR_MARK, 0, 0, 0);
		}
		return null;
	}

	private static String senderBefore(String line, int markerStart) {
		Matcher senderMatcher = SENDER.matcher(line.substring(0, markerStart));
		String sender = null;
		while (senderMatcher.find()) {
			sender = senderMatcher.group(1);
		}
		return sender;
	}
}
