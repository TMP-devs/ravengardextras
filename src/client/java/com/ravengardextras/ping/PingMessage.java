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
 *   <li>{@code RGE-MARK @ x, y, z} - permanent mark, stays until cleared</li>
 *   <li>{@code RGE-MARK @ x, y, z (Bandage)} - permanent mark labeled with an item name</li>
 *   <li>{@code RGE-MARK CLEAR @ x, y, z} - removes the sender's mark at that spot</li>
 *   <li>{@code RGE-MARK CLEAR} - removes all of the sender's marks</li>
 * </ul>
 *
 * <p>Pure string logic, no Minecraft types.
 */
public final class PingMessage {
	private static final Pattern COORDS =
			Pattern.compile("RGE-(PING|MARK) @ (-?\\d{1,8}), (-?\\d{1,8}), (-?\\d{1,8})( \\(([^()]{1,48})\\))?");
	private static final Pattern CLEAR_AT = Pattern.compile("RGE-MARK CLEAR @ (-?\\d{1,8}), (-?\\d{1,8}), (-?\\d{1,8})");
	private static final Pattern CLEAR_ALL = Pattern.compile("RGE-MARK CLEAR(?! @)");
	/** A player-name token directly followed by a chat separator ("Name:", "Name >", "Name »"). */
	private static final Pattern SENDER = Pattern.compile("([A-Za-z0-9_]{1,16})\\s*[:>»]");

	public static final int MAX_LABEL_LENGTH = 32;

	private PingMessage() {
	}

	public enum Type {
		PING, MARK, CLEAR_MARK, CLEAR_ALL_MARKS
	}

	/** Coordinates are meaningless (zero) for CLEAR_ALL_MARKS; label is null unless type is MARK with a name. */
	public record Parsed(String sender, Type type, int x, int y, int z, String label) {
	}

	public static String formatPing(int x, int y, int z) {
		return "RGE-PING @ " + x + ", " + y + ", " + z;
	}

	public static String formatMark(int x, int y, int z, String label) {
		String base = "RGE-MARK @ " + x + ", " + y + ", " + z;
		String clean = sanitizeLabel(label);
		return clean.isEmpty() ? base : base + " (" + clean + ")";
	}

	public static String formatClearMark(int x, int y, int z) {
		return "RGE-MARK CLEAR @ " + x + ", " + y + ", " + z;
	}

	public static String formatClearAllMarks() {
		return "RGE-MARK CLEAR";
	}

	/** Keeps a label safe for the wire format: no parentheses or formatting codes, bounded length. */
	public static String sanitizeLabel(String label) {
		if (label == null) {
			return "";
		}
		String clean = label.replaceAll("§.", "").replaceAll("[(){}\\[\\]]", "").strip();
		return clean.length() > MAX_LABEL_LENGTH ? clean.substring(0, MAX_LABEL_LENGTH).strip() : clean;
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

		Matcher clearAt = CLEAR_AT.matcher(plainChatLine);
		if (clearAt.find()) {
			String sender = senderBefore(plainChatLine, clearAt.start());
			if (sender == null) {
				return null;
			}
			try {
				return new Parsed(sender, Type.CLEAR_MARK,
						Integer.parseInt(clearAt.group(1)),
						Integer.parseInt(clearAt.group(2)),
						Integer.parseInt(clearAt.group(3)), null);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		Matcher coords = COORDS.matcher(plainChatLine);
		if (coords.find()) {
			String sender = senderBefore(plainChatLine, coords.start());
			if (sender == null) {
				return null;
			}
			try {
				Type type = coords.group(1).equals("MARK") ? Type.MARK : Type.PING;
				String label = type == Type.MARK ? coords.group(6) : null;
				return new Parsed(sender, type,
						Integer.parseInt(coords.group(2)),
						Integer.parseInt(coords.group(3)),
						Integer.parseInt(coords.group(4)), label);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		Matcher clearAll = CLEAR_ALL.matcher(plainChatLine);
		if (clearAll.find()) {
			String sender = senderBefore(plainChatLine, clearAll.start());
			if (sender == null) {
				return null;
			}
			return new Parsed(sender, Type.CLEAR_ALL_MARKS, 0, 0, 0, null);
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
