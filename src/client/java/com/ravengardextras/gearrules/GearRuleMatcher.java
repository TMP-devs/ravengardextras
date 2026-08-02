package com.ravengardextras.gearrules;

import net.minecraft.world.item.ItemStack;

/** Evaluates a stack's cards top-to-bottom; the first matching card's color wins. */
public final class GearRuleMatcher {
	private GearRuleMatcher() {
	}

	public static int colorFor(ItemStack stack, GearRulesConfig config) {
		if (stack.isEmpty() || !config.enabled) {
			return 0;
		}
		GearPreset preset = config.active();
		ItemStats stats = ItemStats.of(stack);
		for (GearCard card : preset.cards) {
			if (card.matches(stats)) {
				return card.effectiveColor();
			}
		}
		return 0;
	}
}
