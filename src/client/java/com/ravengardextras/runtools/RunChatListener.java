package com.ravengardextras.runtools;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

/**
 * Single entry point for reading the chat and action bar. Every incoming line is
 * colour-stripped and offered to the XP parser (feeding the run tally) and to the
 * stronghold alert. XP gains routinely appear in the action bar (an overlay game
 * message), so overlay messages are read too — unlike the ping listener, which skips
 * them.
 */
public final class RunChatListener {
	private RunChatListener() {
	}

	public static void register(RunTracker tracker, StrongholdAlert alert, RunToolsConfig config) {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) ->
				handle(message, tracker, alert, config));
		ClientReceiveMessageEvents.GAME.register((message, overlay) ->
				handle(message, tracker, alert, config));
	}

	private static void handle(Component message, RunTracker tracker, StrongholdAlert alert, RunToolsConfig config) {
		if (message == null) {
			return;
		}
		String plain = message.getString().replaceAll("§.", "");

		alert.observe(plain, System.currentTimeMillis());

		if (config.xpCalcEnabled) {
			long xp = XpParser.parseXp(plain);
			if (xp > 0) {
				tracker.addXp(xp);
			}
		}
	}
}
