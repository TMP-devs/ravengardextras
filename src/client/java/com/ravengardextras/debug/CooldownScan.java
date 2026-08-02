package com.ravengardextras.debug;

import com.ravengardextras.cooldown.RavengardCooldownAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshots the player's item cooldowns for the debug file. Lists every hotbar/offhand item (so the
 * Q/F ability slots can be identified even when ready) plus any item anywhere that is currently on
 * cooldown, with its slot, name, cooldown group id, remaining percent and seconds.
 */
public final class CooldownScan {

	private CooldownScan() {
	}

	public static List<String> lines() {
		List<String> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			out.add("(no player)");
			return out;
		}

		ItemCooldowns cooldowns = player.getCooldowns();
		RavengardCooldownAccess access = (RavengardCooldownAccess) cooldowns;
		Inventory inv = player.getInventory();

		int size = inv.getContainerSize();
		int listed = 0;
		for (int i = 0; i < size; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			boolean onCooldown = cooldowns.isOnCooldown(stack);
			boolean hotbar = i < 9;
			// Hotbar/offhand items are always listed (to map slots to abilities); other slots only
			// when actually on cooldown.
			if (!onCooldown && !hotbar) {
				continue;
			}
			Identifier group = cooldowns.getCooldownGroup(stack);
			String status;
			if (onCooldown) {
				float pct = cooldowns.getCooldownPercent(stack, 0.0F);
				float seconds = access.ravengardextras$remainingTicks(group) / 20.0F;
				status = String.format("ON COOLDOWN %.0f%% %.1fs", pct * 100.0F, seconds);
			} else {
				status = "ready";
			}
			out.add(String.format("slot %d  [%s]  cdGroup=%s", i, status, group));
			out.add("  " + ItemDump.signature(stack));
			listed++;
		}

		if (listed == 0) {
			out.add("(no hotbar/offhand items and nothing on cooldown)");
		}
		return out;
	}
}
