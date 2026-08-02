package com.ravengardextras.runtools;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single entry point for reading the chat and action bar. Every incoming line is
 * colour-stripped and offered to the XP parser (feeding the run tally) and checked for
 * the local player's escape line, which triggers the calculated-totals summary. XP
 * gains routinely appear in the action bar (an overlay game message), so overlay
 * messages are read too — unlike the ping listener, which skips them.
 */
public final class RunChatListener {
	private RunChatListener() {
	}

	public static void register(RunTracker tracker, RunToolsConfig config) {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) ->
				handle(message, tracker, config));
		ClientReceiveMessageEvents.GAME.register((message, overlay) ->
				handle(message, tracker, config));
	}

	private static void handle(Component message, RunTracker tracker, RunToolsConfig config) {
		if (message == null) {
			return;
		}
		String plain = message.getString().replaceAll("§.", "");

		// The player's own "<name> has escaped with …!" line is the last line of the escape
		// block (after the final +EXP), so summarising here captures the complete totals.
		if (isOwnEscapeLine(plain)) {
			tracker.reportEscape(parseEscapedCrowns(plain));
		}

		if (config.xpCalcEnabled) {
			long xp = XpParser.parseXp(plain);
			if (xp > 0) {
				tracker.addXp(xp);
			}
		}
	}

	private static boolean isOwnEscapeLine(String plain) {
		if (!plain.toLowerCase(Locale.ROOT).contains("has escaped with")) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return false;
		}
		String name = client.player.getName().getString();
		return !name.isEmpty() && plain.contains(name);
	}

	/** "…has escaped with ♛136!" → 136. The crown glyph and any commas are skipped. Returns -1 if unparseable. */
	static long parseEscapedCrowns(String plain) {
		if (plain == null) {
			return -1;
		}
		Matcher matcher = ESCAPED_WITH.matcher(plain);
		if (!matcher.find()) {
			return -1;
		}
		try {
			return Long.parseLong(matcher.group(1).replace(",", ""));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static final Pattern ESCAPED_WITH =
			Pattern.compile("escaped with\\D*?([\\d,]+)", Pattern.CASE_INSENSITIVE);
}
