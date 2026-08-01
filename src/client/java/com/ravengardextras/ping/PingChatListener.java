package com.ravengardextras.ping;

import com.ravengardextras.RavengardExtrasClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Watches incoming chat for ping messages. Party chat arrives as a signed player
 * message on vanilla servers but as a system/game message on most plugin servers,
 * so both events are registered.
 */
public final class PingChatListener {
	private PingChatListener() {
	}

	public static void register() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> handle(message));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				handle(message);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PingManager.clear());
	}

	private static void handle(Component message) {
		if (!RavengardExtrasClient.PING_CONFIG.enabled) {
			return;
		}
		PingMessage.Parsed parsed = PingMessage.parse(message.getString());
		if (parsed != null) {
			PingManager.add(parsed.sender(), new BlockPos(parsed.x(), parsed.y(), parsed.z()));
		}
	}
}
