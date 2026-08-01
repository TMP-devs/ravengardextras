package com.ravengardextras.ping;

import com.ravengardextras.RavengardExtrasClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Turns ping-key presses into local pings plus party chat broadcasts.
 * The ping key places a temporary ping; the mark key places a permanent mark
 * (crouch + mark key, or marking the same block again, clears it).
 */
public final class PingKeyHandler {
	/** Minimum gap between broadcasts, so key spam can't flood party chat and trip server spam limits. */
	private static final long COOLDOWN_MILLIS = 2000;

	private static long lastPingMillis;

	private PingKeyHandler() {
	}

	public static void onPingKey(Minecraft client) {
		press(client, false);
	}

	public static void onMarkKey(Minecraft client) {
		press(client, true);
	}

	private static void press(Minecraft client, boolean permanent) {
		PingConfig config = RavengardExtrasClient.PING_CONFIG;
		if (!config.enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPingMillis < COOLDOWN_MILLIS) {
			client.gui.hud.setOverlayMessage(Component.literal("Ping on cooldown"), false);
			return;
		}

		String name = player.getName().getString();
		if (permanent && player.isShiftKeyDown()) {
			clearMark(client, name, now);
			return;
		}

		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
			return;
		}
		BlockPos pos = blockHit.getBlockPos();

		if (permanent) {
			PingManager.Ping existing = PingManager.getMark(name);
			if (existing != null && existing.pos().equals(pos)) {
				clearMark(client, name, now); // marking the same block toggles it off
				return;
			}
		}

		lastPingMillis = now;
		PingManager.add(name, pos, permanent);
		String message = permanent
				? PingMessage.formatMark(pos.getX(), pos.getY(), pos.getZ())
				: PingMessage.formatPing(pos.getX(), pos.getY(), pos.getZ());
		broadcast(client, config, message);
	}

	private static void clearMark(Minecraft client, String name, long now) {
		if (PingManager.getMark(name) == null) {
			client.gui.hud.setOverlayMessage(Component.literal("No mark to clear"), false);
			return;
		}
		lastPingMillis = now;
		PingManager.removeMark(name);
		broadcast(client, RavengardExtrasClient.PING_CONFIG, PingMessage.formatClearMark());
		client.gui.hud.setOverlayMessage(Component.literal("Mark cleared"), false);
	}

	private static void broadcast(Minecraft client, PingConfig config, String message) {
		if (config.partyCommand.isBlank() || !PartyStatus.shouldBroadcast()) {
			return;
		}
		ClientPacketListener connection = client.getConnection();
		if (connection != null) {
			// Always send the plain unsigned command packet. sendCommand() signs the
			// arguments when the command parses as signable, and proxy networks
			// (e.g. Hypixel) kick clients over signed-chat state mismatches.
			connection.send(new ServerboundChatCommandPacket(config.partyCommand + " " + message));
			PartyStatus.noteBroadcast(System.currentTimeMillis());
		}
	}
}
