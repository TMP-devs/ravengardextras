package com.ravengardextras.cooldown;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

/**
 * Investigation tool for the cooldown-timer feature. When enabled (via {@code /rge debug}), lists
 * every inventory item that currently has an active cooldown, with its slot, name, remaining
 * percent, seconds, and cooldown group id. Purpose: confirm whether server-drawn ability icons
 * (e.g. Q/F on Hypixel) are backed by real cooldown items and which slots they live in, so the real
 * numeric countdown overlay can be built on solid data.
 */
public final class CooldownDebugOverlay implements HudElement {

	public static volatile boolean enabled = false;

	private static final int LINE_HEIGHT = 10;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (!enabled) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		ItemCooldowns cooldowns = player.getCooldowns();
		RavengardCooldownAccess access = (RavengardCooldownAccess) cooldowns;
		Inventory inv = player.getInventory();

		int x = 4;
		int y = 4;
		g.text(mc.font, "[RGE] items on cooldown:", x, y, 0xFFFFFF55, true);
		y += LINE_HEIGHT;

		int found = 0;
		int size = inv.getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.isEmpty() || !cooldowns.isOnCooldown(stack)) {
				continue;
			}
			Identifier group = cooldowns.getCooldownGroup(stack);
			float pct = cooldowns.getCooldownPercent(stack, 0.0F);
			float seconds = access.ravengardextras$remainingTicks(group) / 20.0F;
			String line = String.format(
					"slot %d  %s  %.0f%%  %.1fs  [%s]",
					i, stack.getHoverName().getString(), pct * 100.0F, seconds, group);
			g.text(mc.font, line, x, y, 0xFFFFFFFF, true);
			y += LINE_HEIGHT;
			found++;
		}
		if (found == 0) {
			g.text(mc.font, "(nothing on cooldown - use Q / F, then read this list)", x, y, 0xFFAAAAAA, true);
		}
	}
}
