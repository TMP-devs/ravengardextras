package com.ravengardextras.ping;

import com.ravengardextras.RavengardExtrasClient;
import com.ravengardextras.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Turns ping-key presses into local pings plus party chat broadcasts.
 *
 * <p>The ping key places a temporary ping; double-tapping it upgrades that ping into
 * a red flashing alert. The mark key: a tap places a permanent
 * mark (up to {@link PingManager#MAX_MARKS}; the oldest is replaced past that),
 * marking an already-marked block toggles it off, and holding the key clears ALL
 * your marks - no menus, so it stays fast under pressure. If the crosshair is on
 * a dropped item, the mark takes the item's name; with a container GUI open, the
 * mark key marks the hovered item by name at the container's position.
 */
public final class PingKeyHandler {
	/** Minimum gap between placement broadcasts; clears are exempt so panic-clearing is never blocked. */
	private static final long COOLDOWN_MILLIS = 2000;
	/** How far a dropped item's hitbox is inflated for aiming - items are tiny. */
	private static final double ITEM_AIM_MARGIN = 0.35;

	private static long lastPingMillis;
	private static long holdStartMillis;
	private static boolean holdConsumed;

	/** Two ping-key taps at most this far apart upgrade the ping into an alert. */
	private static final long DOUBLE_TAP_WINDOW_MILLIS = 400;

	private static final DoubleTap PING_DOUBLE_TAP = new DoubleTap(DOUBLE_TAP_WINDOW_MILLIS);
	/** Whether the previous ping-key tap actually placed a ping (not cooldown/range blocked). */
	private static boolean lastTapPinged;
	/** Where that ping landed, as an upgradeToAlert fallback if the second tap's pick misses. */
	private static BlockPos lastPingPos;

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
		long now = System.currentTimeMillis();
		// Only a tap that actually placed a ping can be upgraded; a blocked first
		// tap falls through to the normal (cooldown-checked) path, so double-tap
		// spam can't flood party chat.
		if (PING_DOUBLE_TAP.tap(now) && lastTapPinged) {
			upgradeToAlert(client, config, player, now);
			return;
		}
		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
			lastTapPinged = false;
			return;
		}
		if (onCooldown(client)) {
			lastTapPinged = false;
			return;
		}
		lastTapPinged = true;
		BlockPos pos = blockHit.getBlockPos();
		lastPingPos = pos;
		PingManager.addPing(player.getName().getString(), pos);
		broadcast(client, config, PingMessage.formatPing(pos.getX(), pos.getY(), pos.getZ()));
	}

	/**
	 * Second tap of a double-tap: replaces the just-placed ping with an alert at
	 * the block now under the crosshair, or at the first tap's own block if this
	 * pick misses (e.g. the player looked at the sky). Uses the cached
	 * {@link #lastPingPos} rather than re-querying {@link PingManager#pingOf} so
	 * this still works if the party-chat echo of the first tap already re-keyed
	 * the ping under a different display name (nickname servers). Exempt from
	 * the cooldown check - the first tap just passed it - but refreshes the
	 * timestamp so a double-tap still counts as one use.
	 */
	private static void upgradeToAlert(Minecraft client, PingConfig config, LocalPlayer player, long now) {
		String name = player.getName().getString();
		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		BlockPos pos = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
				? blockHit.getBlockPos()
				: lastPingPos;
		lastPingMillis = now;
		PingManager.addAlert(name, pos);
		broadcast(client, config, PingMessage.formatAlert(pos.getX(), pos.getY(), pos.getZ()));
	}

	/**
	 * Called every client tick with the mark key's held state. A short press places
	 * (on release); holding for the configured duration clears all marks.
	 */
	public static void tickMarkKey(Minecraft client, boolean down) {
		long holdMillis = RavengardExtrasClient.PING_CONFIG.clearAllHoldSeconds * 1000L;
		long now = System.currentTimeMillis();
		if (down) {
			if (holdStartMillis == 0) {
				holdStartMillis = now;
				holdConsumed = false;
			}
			long held = now - holdStartMillis;
			if (!holdConsumed && held >= holdMillis) {
				holdConsumed = true;
				clearAllMarks(client);
			} else if (!holdConsumed && held >= 400) {
				client.gui.hud.setOverlayMessage(Component.literal("Keep holding to clear your marks..."), false);
			}
		} else if (holdStartMillis != 0) {
			boolean tap = !holdConsumed;
			holdStartMillis = 0;
			if (tap) {
				placeOrToggleMark(client);
			}
		}
	}

	/** Marks the item hovered in a container GUI, at the player's own position (not wherever they're looking). */
	public static void markHoveredItem(Minecraft client, AbstractContainerScreen<?> screen) {
		PingConfig config = RavengardExtrasClient.PING_CONFIG;
		if (!config.enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}
		Slot slot = ((AbstractContainerScreenAccessor) screen).ravengardextras$getHoveredSlot();
		if (slot == null || !slot.hasItem()) {
			client.gui.hud.setOverlayMessage(Component.literal("Hover an item to mark it"), false);
			return;
		}
		if (onCooldown(client)) {
			return;
		}
		String label = slot.getItem().getHoverName().getString();
		BlockPos pos = player.blockPosition();

		String name = player.getName().getString();
		PingManager.addMark(name, pos, label);
		broadcast(client, config, PingMessage.formatMark(pos.getX(), pos.getY(), pos.getZ(), label));
	}

	private static void placeOrToggleMark(Minecraft client) {
		PingConfig config = RavengardExtrasClient.PING_CONFIG;
		if (!config.enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}
		String name = player.getName().getString();

		// Looking at one of your own mark diamonds? Tap clears it.
		PingManager.Ping aimed = findAimedMark(player, name);
		if (aimed != null) {
			clearMarkAt(client, name, aimed.pos());
			return;
		}

		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		boolean blockHit = hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK;

		// Prefer a dropped item under the crosshair (but never through a wall).
		BlockPos pos;
		String label = null;
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
		if (onCooldown(client)) {
			return;
		}
		PingManager.addMark(name, pos, label);
		broadcast(client, config, PingMessage.formatMark(pos.getX(), pos.getY(), pos.getZ(), label));
	}

	private static boolean onCooldown(Minecraft client) {
		long now = System.currentTimeMillis();
		if (now - lastPingMillis < COOLDOWN_MILLIS) {
			client.gui.hud.setOverlayMessage(Component.literal("Ping on cooldown"), false);
			return true;
		}
		lastPingMillis = now;
		return false;
	}

	/**
	 * The player's own mark whose floating diamond the crosshair is on, or null.
	 * Angular test against the diamond's center; the diamond's apparent size is
	 * roughly constant (it scales with distance), so a fixed cone works at any range.
	 */
	private static PingManager.Ping findAimedMark(LocalPlayer player, String name) {
		final double aimCone = 0.9975; // cos ~4 degrees, slightly wider than the diamond
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 view = player.getViewVector(1.0F).normalize();
		PingManager.Ping best = null;
		double bestDot = aimCone;
		for (PingManager.Ping mark : PingManager.marksOf(name)) {
			Vec3 diamondCenter = new Vec3(
					mark.pos().getX() + 0.5, mark.pos().getY() + PingRenderer.HOVER, mark.pos().getZ() + 0.5);
			double dot = diamondCenter.subtract(eye).normalize().dot(view);
			if (dot > bestDot) {
				bestDot = dot;
				best = mark;
			}
		}
		return best;
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

	private static void clearAllMarks(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		String name = player.getName().getString();
		if (PingManager.marksOf(name).isEmpty()) {
			client.gui.hud.setOverlayMessage(Component.literal("No marks to clear"), false);
			return;
		}
		PingManager.removeAllMarks(name);
		broadcast(client, RavengardExtrasClient.PING_CONFIG, PingMessage.formatClearAllMarks());
		client.gui.hud.setOverlayMessage(Component.literal("All marks cleared"), false);
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
