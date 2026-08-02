package com.ravengardextras.runtools;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decides whether a chat/system line is announcing that the stronghold has opened.
 *
 * <p>Requires both the word "stronghold" and an opening verb (open/unlock/unseal/breach)
 * so unrelated "opened a chest" lines and "stronghold is sealed/closed" lines are ignored.
 * Colour codes are assumed already stripped.
 */
public final class StrongholdAlertMatcher {
	private static final Pattern OPEN_VERB = Pattern.compile("\\b(open|unlock|unseal|breach)");

	private StrongholdAlertMatcher() {
	}

	public static boolean isStrongholdOpened(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		String lower = text.toLowerCase(Locale.ROOT);
		return lower.contains("stronghold") && OPEN_VERB.matcher(lower).find();
	}
}
