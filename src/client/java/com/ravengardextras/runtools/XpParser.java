package com.ravengardextras.runtools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a single XP gain amount from one line of chat or action-bar text.
 *
 * <p>Matches per-event gains where the number comes <em>before</em> the keyword,
 * e.g. {@code "+15 XP"}, {@code "You gained 1,240 EXP"}, {@code "+2.5k Experience"}.
 * The number-after-keyword form ({@code "XP: 1240 / 2000"}) is deliberately ignored
 * so a progress bar total is not mistaken for a gain.
 *
 * <p>Pure string logic — no NBT, no network. Colour codes are assumed already stripped.
 */
public final class XpParser {
	private static final Pattern XP_PATTERN = Pattern.compile(
			"\\+?\\s*([\\d,]+(?:\\.\\d+)?)\\s*([kKmM]?)\\s*(?:XP|EXP|Experience)\\b",
			Pattern.CASE_INSENSITIVE);

	/** A "1,240 / 5,000" style ratio marks a progress bar total, not a per-event gain. */
	private static final Pattern RATIO = Pattern.compile("[\\d,]+\\s*/\\s*[\\d,]+");

	private XpParser() {
	}

	/** Returns the XP amount in the line, or -1 if the line carries no XP gain. */
	public static long parseXp(String text) {
		if (text == null || text.isEmpty()) {
			return -1;
		}
		if (RATIO.matcher(text).find()) {
			return -1;
		}
		Matcher matcher = XP_PATTERN.matcher(text);
		if (!matcher.find()) {
			return -1;
		}
		String numberPart = matcher.group(1).replace(",", "");
		String suffix = matcher.group(2);
		double value;
		try {
			value = Double.parseDouble(numberPart);
		} catch (NumberFormatException e) {
			return -1;
		}
		if (suffix != null) {
			if (suffix.equalsIgnoreCase("k")) {
				value *= 1_000;
			} else if (suffix.equalsIgnoreCase("m")) {
				value *= 1_000_000;
			}
		}
		return Math.round(value);
	}
}
