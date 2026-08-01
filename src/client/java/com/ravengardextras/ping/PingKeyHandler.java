package com.ravengardextras.ping;

import com.ravengardextras.RavengardExtrasClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Turns ping-key presses into local pings plus party chat broadcasts.
 * The ping key places a temporary ping. The mark key places a permanent mark
 * (up to {@link PingManager#MAX_MARKS}; the oldest is replaced past that); if
 * the crosshair is on a dropped item, the mark takes the item's name. Marking
 * an already-marked block clears that mark, and crouch + mark key clears the
 * mark you're looking toward - no menus, so it stays fast under pressure.
 */
public final class PingKeyHandler {
	/** Minimum gap between placement broadcasts; clears are exempt so panic-clearing is never blocked. */
	private static final long COOLDOWN_MILLIS = 2000;
	/** How far a dropped item's hitbox is inflated for aiming - items are tiny. */
	private static final double ITEM_AIM_MARGIN = 0.35;

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
		String name = player.getName().getString();

		if (permanent && player.isShiftKeyDown()) {
			clearAimedMark(client, player, name);
			return;
		}

		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		boolean blockHit = hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK;

		BlockPos pos;
		String label = null;
		if (permanent) {
			// Prefer a dropped item under the crosshair (but never through a wall).
			ItemEntity item = pickItem(player, blockHit ? hit.getLocation() : null, config.maxPingDistance);
			if (item != null) {
				pos = item.blockPosition();
				label = item.getItem().getHoverName().getString();
			} else if (blockHit) {
				pos = ((BlockHitResult) hit).getBlockPos();
			} else {
				client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
				return;
			}
			if (PingManager.markAt(name, pos) != null) {
				clearMarkAt(client, name, pos); // marking the same spot toggles it off
				return;
			}
		} else if (blockHit) {
			pos = ((BlockHitResult) hit).getBlockPos();
		} else {
			client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPingMillis < COOLDOWN_MILLIS) {
			client.gui.hud.setOverlayMessage(Component.literal("Ping on cooldown"), false);
			return;
		}
		lastPingMillis = now;

		if (permanent) {
			PingManager.addMark(name, pos, label);
			broadcast(client, config, PingMessage.formatMark(pos.getX(), pos.getY(), pos.getZ(), label));
		} else {
			PingManager.addPing(name, pos);
			broadcast(client, config, PingMessage.formatPing(pos.getX(), pos.getY(), pos.getZ()));
		}
	}

	/** The dropped item nearest along the view ray, or null. maxDistance stops at hitLocation (a wall) if given. */
	private static ItemEntity pickItem(LocalPlayer player, Vec3 hitLocation, double maxDistance) {
		Vec3 from = player.getEyePosition(1.0F);
		double range = hitLocation != null ? Math.min(hitLocation.distanceTo(from), maxDistance) : maxDistance;
		Vec3 to = from.add(player.getViewVector(1.0F).scale(range));
		AABB searchBox = new AABB(from, to).inflate(ITEM_AIM_MARGIN);

		ItemEntity nearest = null;
		double nearestDistSqr = Double.MAX_VALUE;
		List<Entity> candidates = player.level().getEntities(player, searchBox, entity -> entity instanceof ItemEntity);
		for (Entity entity : candidates) {
			Optional<Vec3> clip = entity.getBoundingBox().inflate(ITEM_AIM_MARGIN).clip(from, to);
			if (clip.isPresent()) {
				double distSqr = from.distanceToSqr(clip.get());
				if (distSqr < nearestDistSqr) {
					nearest = (ItemEntity) entity;
					nearestDistSqr = distSqr;
				}
			}
		}
		return nearest;
	}

	/** Clears whichever of the player's marks is closest to the crosshair direction. */
	private static void clearAimedMark(Minecraft client, LocalPlayer player, String name) {
		List<PingManager.Ping> marks = PingManager.marksOf(name);
		if (marks.isEmpty()) {
			client.gui.hud.setOverlayMessage(Component.literal("No marks to clear"), false);
			return;
		}
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 view = player.getViewVector(1.0F).normalize();
		PingManager.Ping best = null;
		double bestDot = -2.0;
		for (PingManager.Ping mark : marks) {
			Vec3 toMark = Vec3.atCenterOf(mark.pos()).subtract(eye).normalize();
			double dot = toMark.dot(view);
			if (dot > bestDot) {
				bestDot = dot;
				best = mark;
			}
		}
		clearMarkAt(client, name, best.pos());
	}

	private static void clearMarkAt(Minecraft client, String name, BlockPos pos) {
		PingManager.Ping mark = PingManager.markAt(name, pos);
		PingManager.removeMarkAt(name, pos);
		broadcast(client, RavengardExtrasClient.PING_CONFIG,
				PingMessage.formatClearMark(pos.getX(), pos.getY(), pos.getZ()));
		String what = mark != null && mark.label() != null ? mark.label() : "Mark";
		client.gui.hud.setOverlayMessage(Component.literal(what + " cleared"), false);
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
