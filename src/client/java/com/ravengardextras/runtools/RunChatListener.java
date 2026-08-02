package com.ravengardextras.runtools;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

/**
 * Single entry point for reading the chat and action bar. Every incoming line is
 * colour-stripped and offered to the XP parser, feeding the run tally. XP gains
 * routinely appear in the action bar (an overlay game message), so overlay messages
 * are read too — unlike the ping listener, which skips them.
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
		if (message == null || !config.xpCalcEnabled) {
			return;
		}
		String plain = message.getString().replaceAll("§.", "");
		long xp = XpParser.parseXp(plain);
		if (xp > 0) {
			tracker.addXp(xp);
		}
	}
}
