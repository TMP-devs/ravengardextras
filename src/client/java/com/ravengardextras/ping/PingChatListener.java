package com.ravengardextras.ping;

import com.ravengardextras.RavengardExtrasClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Watches incoming chat for ping messages. Party chat arrives as a signed player
 * message on vanilla servers but as a system/game message on most plugin servers,
 * so both events are registered. For signed chat the sender comes from the
 * authoritative GameProfile (so nobody can forge a ping as someone else by typing
 * "Name: RGE-PING ..."); for system messages the sender is parsed from the text,
 * which is the best a client-only mod can do.
 */
public final class PingChatListener {
	private PingChatListener() {
	}

	public static void register() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) ->
				handle(message, sender != null ? sender.name() : null));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				handle(message, null);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			PingManager.clear();
			PingColors.reset();
			PartyStatus.reset();
		});
		// Coordinates mean nothing in another dimension, so drop pings on any level change.
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> PingManager.clear());
	}

	private static void handle(Component message, String authoritativeSender) {
		if (!RavengardExtrasClient.PING_CONFIG.enabled) {
			return;
		}
		String plain = message.getString().replaceAll("§.", "");
		if (PartyStatus.observe(plain, System.currentTimeMillis())) {
			Minecraft.getInstance().gui.hud.setOverlayMessage(
					Component.literal("No party - pings are now just for you"), false);
		}
		PingMessage.Parsed parsed = PingMessage.parse(message.getString());
		if (parsed == null) {
			return;
		}
		String sender = authoritativeSender != null ? authoritativeSender : parsed.sender();
		if (parsed.type() == PingMessage.Type.CLEAR_MARK) {
			PingManager.removeMark(sender);
			return;
		}
		boolean permanent = parsed.type() == PingMessage.Type.MARK;
		BlockPos pos = new BlockPos(parsed.x(), parsed.y(), parsed.z());
		reconcileOwnEcho(sender, pos, permanent);
		PingManager.add(sender, pos, permanent);
	}

	/**
	 * On nickname servers the party chat echo of our own ping comes back under a
	 * display name that differs from our profile name. If an incoming ping lands
	 * exactly on our profile-keyed ping, treat it as that echo and drop the
	 * profile-keyed copy, so we keep one diamond keyed (and colored) the same way
	 * our teammates see it.
	 */
	private static void reconcileOwnEcho(String sender, BlockPos pos, boolean permanent) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		String ownName = minecraft.player.getName().getString();
		if (!sender.toLowerCase(Locale.ROOT).equals(ownName.toLowerCase(Locale.ROOT))) {
			PingManager.removeIfAt(ownName, pos, permanent);
		}
	}
}
