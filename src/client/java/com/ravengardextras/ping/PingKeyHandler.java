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

/** Turns a ping-key press into a local ping plus a party chat broadcast. */
public final class PingKeyHandler {
	private PingKeyHandler() {
	}

	public static void onPingKey(Minecraft client) {
		PingConfig config = RavengardExtrasClient.PING_CONFIG;
		if (!config.enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}

		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
			return;
		}

		BlockPos pos = blockHit.getBlockPos();
		PingManager.add(player.getName().getString(), pos);

		if (!config.partyCommand.isBlank()) {
			ClientPacketListener connection = client.getConnection();
			if (connection != null) {
				String command = config.partyCommand + " " + PingMessage.format(pos.getX(), pos.getY(), pos.getZ());
				// Always send the plain unsigned command packet. sendCommand() signs the
				// arguments when the command parses as signable, and proxy networks
				// (e.g. Hypixel) kick clients over signed-chat state mismatches.
				connection.send(new ServerboundChatCommandPacket(command));
			}
		}
	}
}
