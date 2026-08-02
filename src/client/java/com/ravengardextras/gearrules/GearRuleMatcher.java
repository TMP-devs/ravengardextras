package com.ravengardextras.gearrules;

import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/** Evaluates a preset's cards in priority order (highest number first); the first matching card's color wins. */
public final class GearRuleMatcher {
	/** id tiebreak keeps ties stable without depending on list/grid position, which is purely cosmetic. */
	private static final Comparator<GearCard> BY_PRIORITY =
			Comparator.comparingInt((GearCard card) -> -card.priority).thenComparing(card -> card.id);

	private GearRuleMatcher() {
	}

	public static int colorFor(ItemStack stack, GearRulesConfig config) {
		if (stack.isEmpty() || !config.enabled) {
			return 0;
		}
		GearPreset preset = config.active();
		ItemStats stats = ItemStats.of(stack);
		List<GearCard> cards = new java.util.ArrayList<>(preset.cards);
		cards.sort(BY_PRIORITY);
		for (GearCard card : cards) {
			if (card.matches(stats)) {
				return card.effectiveColor();
			}
		}
		return 0;
	}
}
