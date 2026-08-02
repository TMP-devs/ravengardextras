package com.ravengardextras.gearrules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A highlight rule: every condition must match (AND) for the card's color to apply. */
public class GearCard {
	public String id = UUID.randomUUID().toString();
	public String name = "New Card";
	public boolean rainbow = false;
	public int color = 0xFF55FFFF;
	public List<GearCondition> conditions = new ArrayList<>();

	public boolean matches(ItemStats stats) {
		if (this.conditions.isEmpty()) {
			return false;
		}
		for (GearCondition condition : this.conditions) {
			if (!condition.matches(stats)) {
				return false;
			}
		}
		return true;
	}

	public int effectiveColor() {
		return this.rainbow ? com.ravengardextras.gearhighlighter.GearHighlighterConfig.RAINBOW : this.color;
	}
}
