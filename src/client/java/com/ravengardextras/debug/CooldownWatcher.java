package com.ravengardextras.debug;

import com.ravengardextras.cooldown.CooldownDebugOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ticks every client tick while {@code /rge debug} is on. Records a baseline of each hotbar/offhand
 * item, then logs every subsequent change to the debug file - so pressing an ability captures
 * exactly how its item data (custom_model_data, damage, count, ...) animates as it cools down.
 * That reveals where a resource-pack-driven cooldown actually lives.
 */
public final class CooldownWatcher {

	private static final Map<Integer, String> LAST = new HashMap<>();

	private CooldownWatcher() {
	}

	public static void tick() {
		if (!CooldownDebugOverlay.enabled) {
			if (!LAST.isEmpty()) {
				LAST.clear();
			}
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		Inventory inv = player.getInventory();
		int size = inv.getContainerSize();
		for (int i = 0; i < size; i++) {
			// Hotbar (0-8) and equipment/offhand (>=36); skip the 27 storage slots.
			if (i >= 9 && i < 36) {
				continue;
			}
			ItemStack stack = inv.getItem(i);
			String sig = ItemDump.signature(stack);
			String prev = LAST.get(i);
			if (prev == null) {
				LAST.put(i, sig);
				if (!stack.isEmpty()) {
					DebugFile.section("ITEM BASELINE slot " + i, List.of(sig));
				}
				continue;
			}
			if (!sig.equals(prev)) {
				LAST.put(i, sig);
				DebugFile.section("ITEM CHANGE slot " + i, List.of("was: " + prev, "now: " + sig));
			}
		}
	}
}
